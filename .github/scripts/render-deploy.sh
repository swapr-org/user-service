#!/usr/bin/env bash
set -euo pipefail

: "${RENDER_DEPLOY_HOOK_URL:?}" "${IMAGE:?}" "${RENDER_SERVICE_ID:?}" "${RENDER_API_KEY:?}" "${DEPLOY_ENV:?}"

LIMIT_SECONDS=900
POLL_SECONDS=15

started=$(date +%s)
deadline=$((started + LIMIT_SECONDS))
body=$(mktemp)

fail() {
  echo "::error::${DEPLOY_ENV}: $1"
  exit 1
}

image_param=$(jq -rn --arg v "$IMAGE" '$v|@uri')
if ! code=$(curl -sS -o "$body" -w '%{http_code}' -X POST "${RENDER_DEPLOY_HOOK_URL}&imgURL=${image_param}"); then
  fail "the request to the Render deploy hook failed"
fi

if [[ "$code" != 2* ]]; then
  fail "the Render deploy hook returned HTTP ${code}"
fi

deploy_id=$(jq -r '.deploy.id // empty' "$body" 2>/dev/null || true)
if [[ "$code" == "202" || -z "$deploy_id" ]]; then
  fail "Render queued the deploy behind another one and returned no deploy ID to track"
fi

echo "${DEPLOY_ENV}: triggered deploy ${deploy_id} of ${IMAGE}"

last_status="none"
while true; do
  if ! code=$(curl -sS -o "$body" -w '%{http_code}' \
      -H "Authorization: Bearer ${RENDER_API_KEY}" -H "Accept: application/json" \
      "https://api.render.com/v1/services/${RENDER_SERVICE_ID}/deploys/${deploy_id}"); then
    fail "the request for deploy ${deploy_id} failed (last status: ${last_status})"
  fi
  if [[ "$code" == 5* ]]; then
    echo "${DEPLOY_ENV}: Render returned HTTP ${code} for deploy ${deploy_id}, retrying"
  elif [[ "$code" != "200" ]]; then
    fail "Render returned HTTP ${code} for deploy ${deploy_id} (last status: ${last_status})"
  else
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
        fail "deploy ${deploy_id} failed (last status: ${status})"
        ;;
    esac
  fi

  if (( $(date +%s) >= deadline )); then
    fail "deploy ${deploy_id} was not live after 15 minutes (last status: ${last_status})"
  fi
  sleep "$POLL_SECONDS"
done
