#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://54.253.3.176}"
USER_ID="${2:-1958142146}"
ORDER_ADMIN_KEY="${3:-}"
ALBUM_ID="${4:-178}"
ACCESS_TOKEN="${5:-${ACCESS_TOKEN:-}}"
CURL_BIN="${CURL_BIN:-$(command -v curl || true)}"
PYTHON_BIN="${PYTHON_BIN:-$(command -v python3 || true)}"

if [[ -z "${CURL_BIN}" ]]; then
  CURL_BIN="/usr/bin/curl"
fi
if [[ -z "${PYTHON_BIN}" ]]; then
  PYTHON_BIN="/usr/bin/python3"
fi

json_get() {
  local key="$1"
  local payload="${2:-}"
  "${PYTHON_BIN}" - "$key" "$payload" <<'PY'
import json,sys
k=sys.argv[1]
raw=sys.argv[2] if len(sys.argv) > 2 else ''
obj=json.loads(raw) if raw else {}
cur=obj
for part in k.split('.'):
  if isinstance(cur, dict):
    cur=cur.get(part)
  else:
    cur=None
print('' if cur is None else cur)
PY
}

post_json() {
  local path="$1"
  local body="$2"
  local args=(
    -sS -X POST "${BASE_URL}${path}"
    -H "Content-Type: application/json"
  )
  if [[ -n "${ACCESS_TOKEN}" ]]; then
    args+=(-H "Authorization: Bearer ${ACCESS_TOKEN}")
  fi
  args+=(-d "${body}")
  "${CURL_BIN}" "${args[@]}"
}

get_json() {
  local path="$1"
  local args=(-sS "${BASE_URL}${path}")
  if [[ -n "${ACCESS_TOKEN}" ]]; then
    args+=(-H "Authorization: Bearer ${ACCESS_TOKEN}")
  fi
  "${CURL_BIN}" "${args[@]}"
}

echo "[A] Billing mock flow QA (TOSS / NAVER_ENTRY / INICIS)"
if [[ -z "${ACCESS_TOKEN}" ]]; then
  echo "  skip (ACCESS_TOKEN not provided): billing/order/address API is protected."
  echo "  usage: ./scripts/run-qa-mock-suite.sh <baseUrl> <userId> <adminKey> <albumId> <accessToken>"
  echo "QA MOCK SUITE DONE (SKIPPED) ✅"
  exit 0
fi

declare -a PREPARE_CASES=(
  "TOSS|/api/billing/prepare|{\"userId\":\"${USER_ID}\",\"planCode\":\"SNAPFIT_PRO_MONTHLY\",\"provider\":\"TOSS_NAVERPAY\"}"
  "NAVER_ENTRY|/api/billing/naverpay/prepare|{\"userId\":\"${USER_ID}\",\"planCode\":\"SNAPFIT_PRO_MONTHLY\",\"provider\":\"TOSS_NAVERPAY\"}"
  "INICIS|/api/billing/prepare|{\"userId\":\"${USER_ID}\",\"planCode\":\"SNAPFIT_PRO_MONTHLY\",\"provider\":\"INICIS_NAVERPAY\"}"
)

for row in "${PREPARE_CASES[@]}"; do
  IFS='|' read -r NAME PATH BODY <<<"${row}"
  echo "  - case=${NAME}"
  PREPARE_JSON=$(post_json "${PATH}" "${BODY}")
  ORDER_ID=$(json_get "orderId" "${PREPARE_JSON}")
  PROVIDER=$(json_get "provider" "${PREPARE_JSON}")
  if [[ -z "${ORDER_ID}" ]]; then
    echo "    prepare failed: ${PREPARE_JSON}"
    exit 1
  fi
  APPROVE_JSON=$(post_json "/api/billing/approve" "{\"orderId\":\"${ORDER_ID}\"}")
  SUB_STATUS=$(json_get "status" "${APPROVE_JSON}")
  PLAN=$(json_get "planCode" "${APPROVE_JSON}")
  if [[ "${SUB_STATUS}" != "ACTIVE" ]]; then
    echo "    approve failed: ${APPROVE_JSON}"
    exit 1
  fi
  echo "    ok orderId=${ORDER_ID} provider=${PROVIDER} status=${SUB_STATUS} plan=${PLAN}"
done

echo "[B] Address search QA"
ADDR_JSON=$(get_json "/api/orders/address/search?keyword=%EA%B0%95%EB%82%A8%EB%8C%80%EB%A1%9C%20396&page=1")
TOTAL=$(json_get "totalCount" "${ADDR_JSON}")
if [[ -z "${TOTAL}" ]]; then
  echo "  address search failed: ${ADDR_JSON}"
  exit 1
fi
echo "  ok totalCount=${TOTAL}"

echo "[C] Order flow QA (PAYMENT_PENDING -> IN_PRODUCTION -> SHIPPING -> DELIVERED)"
QUOTE_JSON=$(get_json "/api/orders/quote?albumId=${ALBUM_ID}&pageCount=24")
AMOUNT=$(json_get "amount" "${QUOTE_JSON}")
PAGE_COUNT=$(json_get "pageCount" "${QUOTE_JSON}")
if [[ -z "${AMOUNT}" ]]; then
  echo "  quote failed: ${QUOTE_JSON}"
  exit 1
fi

echo "  quote amount=${AMOUNT} pageCount=${PAGE_COUNT}"
CREATE_JSON=$(post_json "/api/orders" "{\"userId\":\"${USER_ID}\",\"albumId\":${ALBUM_ID},\"title\":\"QA 주문\",\"amount\":999999,\"pageCount\":${PAGE_COUNT},\"paymentMethod\":\"NAVERPAY\",\"recipientName\":\"QA 테스터\",\"recipientPhone\":\"01012341234\",\"zipCode\":\"06236\",\"addressLine1\":\"서울 강남구 강남대로 396\",\"addressLine2\":\"테헤란로\",\"deliveryMemo\":\"문앞\"}")
ORDER_ID=$(json_get "orderId" "${CREATE_JSON}")
SAVED_AMOUNT=$(json_get "amount" "${CREATE_JSON}")
STATUS=$(json_get "status" "${CREATE_JSON}")
if [[ -z "${ORDER_ID}" || "${STATUS}" != "PAYMENT_PENDING" ]]; then
  echo "  create failed: ${CREATE_JSON}"
  exit 1
fi
if [[ "${SAVED_AMOUNT}" != "${AMOUNT}" ]]; then
  echo "  amount mismatch expected=${AMOUNT} actual=${SAVED_AMOUNT}"
  exit 1
fi
echo "  create ok orderId=${ORDER_ID} amount=${SAVED_AMOUNT}"

CONFIRM_JSON=$(post_json "/api/orders/${ORDER_ID}/payment/confirm" "{}")
STATUS=$(json_get "status" "${CONFIRM_JSON}")
if [[ "${STATUS}" != "IN_PRODUCTION" ]]; then
  echo "  confirm failed: ${CONFIRM_JSON}"
  exit 1
fi
echo "  confirm ok status=${STATUS}"

if [[ -n "${ORDER_ADMIN_KEY}" ]]; then
  SHIP_JSON=$("${CURL_BIN}" -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/shipping" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "X-Admin-Key: ${ORDER_ADMIN_KEY}" \
    -d '{"courier":"CJ대한통운","trackingNumber":"1234567890"}')
  STATUS=$(json_get "status" "${SHIP_JSON}")
  if [[ "${STATUS}" != "SHIPPING" ]]; then
    echo "  shipping failed: ${SHIP_JSON}"
    exit 1
  fi
  echo "  shipping ok"

  DONE_JSON=$("${CURL_BIN}" -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/delivered" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "X-Admin-Key: ${ORDER_ADMIN_KEY}")
  STATUS=$(json_get "status" "${DONE_JSON}")
  if [[ "${STATUS}" != "DELIVERED" ]]; then
    echo "  delivered failed: ${DONE_JSON}"
    exit 1
  fi
  echo "  delivered ok"
else
  echo "  skip shipping/delivered (ORDER_ADMIN_KEY not provided)"
fi

echo "QA MOCK SUITE DONE ✅"
