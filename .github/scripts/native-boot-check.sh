#!/usr/bin/env bash
set -uo pipefail

: "${IMAGE:?}"

SUFFIX=${GITHUB_RUN_ID:-local}
NET="user-service-boot-${SUFFIX}"
PG="user-service-boot-pg-${SUFFIX}"
APP="user-service-boot-app-${SUFFIX}"
HOST_PORT=18081
DB_LIMIT_SECONDS=120
APP_LIMIT_SECONDS=120
HEALTH_TIMEOUT_SECONDS=10

cleanup() {
  docker rm -f "$APP" "$PG" >/dev/null 2>&1
  docker network rm "$NET" >/dev/null 2>&1
}
trap cleanup EXIT

docker network create "$NET" >/dev/null || exit 1
docker run -d --name "$PG" --network "$NET" \
  -e POSTGRES_DB=user_service -e POSTGRES_USER=user_service -e POSTGRES_PASSWORD=user_service \
  postgis/postgis:17-3.6 >/dev/null || exit 1

db_ready=""
for _ in $(seq 1 "$DB_LIMIT_SECONDS"); do
  if docker exec "$PG" pg_isready -h 127.0.0.1 -U user_service -d user_service >/dev/null 2>&1; then
    db_ready=yes
    break
  fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$PG")" != "true" ]; then
    break
  fi
  sleep 1
done

if [ -z "$db_ready" ]; then
  echo "::group::database log"
  docker logs "$PG" 2>&1
  echo "::endgroup::"
  echo "::error::database exited or was not ready within ${DB_LIMIT_SECONDS}s"
  exit 1
fi

docker run -d --name "$APP" --network "$NET" -p "${HOST_PORT}:8080" \
  -e SPRING_PROFILES_ACTIVE=qa \
  -e DATABASE_URL="jdbc:postgresql://${PG}:5432/user_service" \
  -e DATABASE_USERNAME=user_service -e DATABASE_PASSWORD=user_service \
  -e SUPABASE_URL=https://example.supabase.co \
  -e INTERNAL_TOKEN=boot-check-token -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  "$IMAGE" >/dev/null || exit 1

verdict=""
for _ in $(seq 1 "$APP_LIMIT_SECONDS"); do
  logs=$(docker logs "$APP" 2>&1)
  if grep -q 'Application run failed' <<<"$logs" || [ "$(docker inspect -f '{{.State.Running}}' "$APP")" != "true" ]; then
    verdict=RED
    break
  fi
  if grep -q 'Started UserServiceApplication' <<<"$logs"; then
    verdict=GREEN
    break
  fi
  sleep 1
done

if [ "$verdict" = GREEN ]; then
  if health=$(curl -sSf --max-time "$HEALTH_TIMEOUT_SECONDS" "http://localhost:${HOST_PORT}/actuator/health"); then
    echo "health: ${health}"
    jq -e '.status == "UP"' >/dev/null <<<"$health" || verdict=RED
  else
    verdict=RED
  fi
fi

if [ "$verdict" != GREEN ]; then
  echo "::group::application log"
  docker logs "$APP" 2>&1
  echo "::endgroup::"
  echo "::error::native boot check failed: ${verdict:-timed out after ${APP_LIMIT_SECONDS}s}"
  exit 1
fi

echo "native boot check passed"
