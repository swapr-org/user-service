#!/usr/bin/env bash
set -uo pipefail

: "${IMAGE:?}"

SUFFIX=${GITHUB_RUN_ID:-local}
NET="user-service-boot-${SUFFIX}"
PG="user-service-boot-pg-${SUFFIX}"
APP="user-service-boot-app-${SUFFIX}"
HOST_PORT=18081
LIMIT_SECONDS=120

cleanup() {
  docker rm -f "$APP" "$PG" >/dev/null 2>&1
  docker network rm "$NET" >/dev/null 2>&1
}
trap cleanup EXIT

docker network create "$NET" >/dev/null || exit 1
docker run -d --name "$PG" --network "$NET" \
  -e POSTGRES_DB=user_service -e POSTGRES_USER=user_service -e POSTGRES_PASSWORD=user_service \
  postgis/postgis:17-3.6 >/dev/null || exit 1

until docker exec "$PG" pg_isready -h 127.0.0.1 -U user_service -d user_service >/dev/null 2>&1; do
  sleep 1
done

docker run -d --name "$APP" --network "$NET" -p "${HOST_PORT}:8080" \
  -e SPRING_PROFILES_ACTIVE=qa \
  -e DATABASE_URL="jdbc:postgresql://${PG}:5432/user_service" \
  -e DATABASE_USERNAME=user_service -e DATABASE_PASSWORD=user_service \
  -e SUPABASE_URL=https://example.supabase.co \
  -e INTERNAL_TOKEN=boot-check-token -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  "$IMAGE" >/dev/null || exit 1

verdict=""
for _ in $(seq 1 "$LIMIT_SECONDS"); do
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
  health=$(curl -sS "http://localhost:${HOST_PORT}/actuator/health")
  echo "health: ${health}"
  grep -q '"status":"UP"' <<<"$health" || verdict=RED
fi

if [ "$verdict" != GREEN ]; then
  echo "::group::application log"
  docker logs "$APP" 2>&1
  echo "::endgroup::"
  echo "::error::native boot check failed: ${verdict:-timed out after ${LIMIT_SECONDS}s}"
  exit 1
fi

echo "native boot check passed"
