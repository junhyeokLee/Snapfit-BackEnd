#!/usr/bin/env bash
set -euo pipefail

# One-shot checker for Phase 1~3:
# 1) Billing/Order mock QA suite
# 2) Ops admin dashboard
# 3) Billing live readiness
#
# Usage:
#   ./scripts/run-phase123-check.sh \
#     --base-url=http://54.253.3.176 \
#     --user-id=1958142146 \
#     --album-id=178 \
#     --admin-key="$SNAPFIT_ORDER_ADMIN_KEY" \
#     --access-token="$ACCESS_TOKEN"
#
# Notes:
# - access token is required for protected APIs.
# - if token is missing, QA suite will be skipped (existing behavior).

BASE_URL="${SNAPFIT_API_BASE_URL:-http://54.253.3.176}"
USER_ID="${SNAPFIT_QA_USER_ID:-1958142146}"
ALBUM_ID="${SNAPFIT_QA_ALBUM_ID:-178}"
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

if [[ -z "${ADMIN_KEY}" ]]; then
  echo "Missing admin key. Use --admin-key or SNAPFIT_ORDER_ADMIN_KEY" >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "[Phase 1] Billing/Order mock QA suite"
ACCESS_TOKEN="${ACCESS_TOKEN}" "${SCRIPT_DIR}/run-qa-mock-suite.sh" "${BASE_URL}" "${USER_ID}" "${ADMIN_KEY}" "${ALBUM_ID}" "${ACCESS_TOKEN}"

if [[ -z "${ACCESS_TOKEN}" ]]; then
  echo
  echo "ACCESS_TOKEN is empty. Protected admin checks are skipped."
  echo "Provide --access-token to complete Phase 2~3 checks."
  exit 0
fi

auth_header=(-H "Authorization: Bearer ${ACCESS_TOKEN}")
admin_header=(-H "X-Admin-Key: ${ADMIN_KEY}")

echo
echo "[Phase 2] Ops dashboard check"
OPS_JSON="$(curl -sS "${BASE_URL}/api/ops/admin/dashboard" "${auth_header[@]}" "${admin_header[@]}")"
echo "${OPS_JSON}" | python3 - <<'PY'
import json,sys
obj=json.load(sys.stdin)
print("generatedAt:", obj.get("generatedAt"))
users=obj.get("users",{})
orders=obj.get("orders",{})
print("users.total:", users.get("total"), "users.new24h:", users.get("new24h"))
print("orders.total:", orders.get("total"), "orders.new24h:", orders.get("new24h"))
PY

echo
echo "[Phase 3] Billing readiness check"
READY_JSON="$(curl -sS "${BASE_URL}/api/billing/admin/readiness" "${auth_header[@]}" "${admin_header[@]}")"
echo "${READY_JSON}" | python3 - <<'PY'
import json,sys
obj=json.load(sys.stdin)
print("mode:", obj.get("mode"))
print("readyForLive:", obj.get("readyForLive"))
summary=obj.get("summary",{})
print("checks:", summary.get("passed"), "/", summary.get("total"))
for c in obj.get("checks",[]):
    mark = "OK" if c.get("ok") else "FAIL"
    print(f"- [{mark}] {c.get('code')}: {c.get('message')}")
PY

echo
echo "Phase 1~3 check done."
