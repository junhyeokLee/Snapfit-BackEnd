#!/usr/bin/env bash
set -euo pipefail

# Usage:
# ./scripts/push_load_test.sh http://54.253.3.176 SnapfitPushAdmin2026! 200 20

BASE_URL="${1:-http://54.253.3.176}"
ADMIN_KEY="${2:-}"
COUNT="${3:-200}"
PARALLEL="${4:-20}"

if [[ -z "$ADMIN_KEY" ]]; then
  echo "ADMIN KEY is required"
  echo "Usage: ./scripts/push_load_test.sh <base_url> <admin_key> [count] [parallel]"
  exit 1
fi

TMP_DIR="$(mktemp -d)"
OK_FILE="$TMP_DIR/ok.txt"
LAT_FILE="$TMP_DIR/latency.txt"

echo "Running push load test: count=$COUNT parallel=$PARALLEL base=$BASE_URL"

seq 1 "$COUNT" | xargs -I{} -P "$PARALLEL" bash -c '
  idx="$1"
  base="$2"
  key="$3"
  start=$(python3 -c "import time; print(int(time.time()*1000))")
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$base/api/notifications/topic" \
    -H "Content-Type: application/json" \
    -H "X-Admin-Key: $key" \
    -d "{\"topic\":\"snapfit_template_new\",\"title\":\"Load Test\",\"body\":\"push-$idx\",\"dryRun\":true,\"data\":{\"type\":\"load_test\",\"index\":\"$idx\"}}")
  end=$(python3 -c "import time; print(int(time.time()*1000))")
  dur=$((end-start))
  if [[ "$code" == "200" ]]; then
    echo 1 >> "'"$OK_FILE"'"
  else
    echo 0 >> "'"$OK_FILE"'"
  fi
  echo "$dur" >> "'"$LAT_FILE"'"
' _ {} "$BASE_URL" "$ADMIN_KEY"

total=$(wc -l < "$OK_FILE" | tr -d ' ')
success=$(awk '{s+=$1} END {print s+0}' "$OK_FILE")
fail=$((total-success))
avg=$(awk '{s+=$1} END {if (NR==0) print 0; else printf "%.2f", s/NR}' "$LAT_FILE")
p95=$(sort -n "$LAT_FILE" | awk 'BEGIN{c=0} {a[c++]=$1} END{if(c==0){print 0}else{idx=int(c*0.95); if(idx>=c) idx=c-1; print a[idx]}}')

echo "Result: success=$success fail=$fail total=$total avg_ms=$avg p95_ms=$p95"
rm -rf "$TMP_DIR"
