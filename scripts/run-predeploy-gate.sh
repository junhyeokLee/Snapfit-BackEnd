#!/usr/bin/env bash
set -euo pipefail

# Pre-deploy QA gate:
# 1) Build/unit tests
# 2) Basic API smoke
# 3) Optional full phase123 check (when token/admin key are provided)

BASE_URL="${SNAPFIT_API_BASE_URL:-http://54.253.3.176}"
USER_ID="${SNAPFIT_QA_USER_ID:-}"
ALBUM_ID="${SNAPFIT_QA_ALBUM_ID:-}"
ADMIN_KEY="${SNAPFIT_ORDER_ADMIN_KEY:-}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"

for arg in "$@"; do
  case "$arg" in
    --base-url=*) BASE_URL="${arg#*=}" ;;
    --user-id=*) USER_ID="${arg#*=}" ;;
    --album-id=*) ALBUM_ID="${arg#*=}" ;;
    --admin-key=*) ADMIN_KEY="${arg#*=}" ;;
    --access-token=*) ACCESS_TOKEN="${arg#*=}" ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "[Gate] Gradle tests"
cd "${ROOT_DIR}"
./gradlew test

echo
echo "[Gate] Basic smoke (${BASE_URL})"
TEMPLATE_HTTP="$(curl -sS -o /tmp/snapfit_gate_templates.json -w "%{http_code}" "${BASE_URL}/api/templates")"
if [[ "${TEMPLATE_HTTP}" != "200" ]]; then
  echo "Smoke failed: GET /api/templates returned ${TEMPLATE_HTTP}" >&2
  exit 1
fi

if [[ -n "${ACCESS_TOKEN}" && -n "${ADMIN_KEY}" && -n "${USER_ID}" && -n "${ALBUM_ID}" ]]; then
  echo
  echo "[Gate] Full phase123 check"
  "${SCRIPT_DIR}/run-phase123-check.sh" \
    --base-url="${BASE_URL}" \
    --user-id="${USER_ID}" \
    --album-id="${ALBUM_ID}" \
    --admin-key="${ADMIN_KEY}" \
    --access-token="${ACCESS_TOKEN}"
else
  echo
  echo "[Gate] Full phase123 skipped (missing ACCESS_TOKEN / ADMIN_KEY / USER_ID / ALBUM_ID)"
fi

echo
echo "Pre-deploy gate passed ✅"

