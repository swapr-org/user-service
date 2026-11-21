#!/usr/bin/env bash
set -euo pipefail

: "${RENDER_DEPLOY_HOOK_URL:?}" "${IMAGE:?}" "${RENDER_SERVICE_ID:?}" "${RENDER_API_KEY:?}" "${DEPLOY_ENV:?}"

LIMIT_SECONDS=900
INITIAL_SLEEP_SECONDS=300
POLL_SECONDS=15

started=$(date +%s)
deadline=$((started + LIMIT_SECONDS))
body=$(mktemp)

image_param=$(jq -rn --arg v "$IMAGE" '$v|@uri')
if ! code=$(curl -sS -o "$body" -w '%{http_code}' -X POST "${RENDER_DEPLOY_HOOK_URL}&imgURL=${image_param}"); then
  echo "::error::${DEPLOY_ENV}: the request to the Render deploy hook failed"
  exit 1
fi

if [[ "$code" != 2* ]]; then
  echo "::error::${DEPLOY_ENV}: the Render deploy hook returned HTTP ${code}"
  exit 1
fi

deploy_id=$(jq -r '.deploy.id // empty' "$body" 2>/dev/null || true)
if [[ "$code" == "202" || -z "$deploy_id" ]]; then
  echo "::error::${DEPLOY_ENV}: Render queued the deploy behind another one and returned no deploy ID to track"
  exit 1
fi

echo "${DEPLOY_ENV}: triggered deploy ${deploy_id} of ${IMAGE}"
echo "${DEPLOY_ENV}: waiting ${INITIAL_SLEEP_SECONDS}s before the first poll"
sleep "$INITIAL_SLEEP_SECONDS"

last_status="none"
while true; do
  if ! code=$(curl -sS -o "$body" -w '%{http_code}' \
      -H "Authorization: Bearer ${RENDER_API_KEY}" -H "Accept: application/json" \
      "https://api.render.com/v1/services/${RENDER_SERVICE_ID}/deploys/${deploy_id}"); then
    echo "::error::${DEPLOY_ENV}: the request for deploy ${deploy_id} failed (last status: ${last_status})"
    exit 1
  fi
  if [[ "$code" != "200" ]]; then
    echo "::error::${DEPLOY_ENV}: Render returned HTTP ${code} for deploy ${deploy_id} (last status: ${last_status})"
    exit 1
  fi

  status=$(jq -r '.status // "unknown"' "$body")
  if [[ "$status" != "$last_status" ]]; then
    echo "${DEPLOY_ENV}: deploy ${deploy_id} is ${status}"
    last_status="$status"
  fi

  case "$status" in
    live)
      exit 0
      ;;
    build_failed | update_failed | pre_deploy_failed | canceled | deactivated)
      echo "::error::${DEPLOY_ENV}: deploy ${deploy_id} failed (last status: ${status})"
      exit 1
      ;;
  esac

  if (( $(date +%s) >= deadline )); then
    echo "::error::${DEPLOY_ENV}: deploy ${deploy_id} was not live after 15 minutes (last status: ${last_status})"
    exit 1
  fi
  sleep "$POLL_SECONDS"
done
