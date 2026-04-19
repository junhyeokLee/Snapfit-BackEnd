#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USER_ID="${2:-1958142146}"
PROVIDER="${3:-TOSS_NAVERPAY}"

echo "[1/5] Prepare subscription order"
PREPARE_JSON=$(curl -sS -X POST "${BASE_URL}/api/billing/prepare" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"${USER_ID}\",\"planCode\":\"SNAPFIT_PRO_MONTHLY\",\"provider\":\"${PROVIDER}\"}")
echo "${PREPARE_JSON}"

ORDER_ID=$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["orderId"])' <<< "${PREPARE_JSON}")
AMOUNT=$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["amount"])' <<< "${PREPARE_JSON}")

echo "[2/5] Approve payment (mock/real depending server mode)"
APPROVE_JSON=$(curl -sS -X POST "${BASE_URL}/api/billing/approve" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"${ORDER_ID}\",\"amount\":${AMOUNT},\"paymentKey\":\"MOCK-PAY-${ORDER_ID}\"}")
echo "${APPROVE_JSON}"

echo "[3/5] Move order to IN_PRODUCTION"
curl -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PRODUCTION"}' | tee /dev/stderr >/dev/null

echo "[4/5] Move order to SHIPPING"
curl -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPING"}' | tee /dev/stderr >/dev/null

echo "[5/5] Move order to DELIVERED"
FINAL_JSON=$(curl -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}')
echo "${FINAL_JSON}"

echo "E2E DONE: orderId=${ORDER_ID}"
