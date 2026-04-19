# SnapFit Real-Service Phase 1→2→3 Runbook

## 1) 결제 테스트(실서비스 전 QA)

### 1-1. Mock E2E 전체 점검
```bash
cd /Users/devsheep/SnapFit/SnapFit-BackEnd
./scripts/run-qa-mock-suite.sh http://54.253.3.176 1958142146 "$SNAPFIT_ORDER_ADMIN_KEY" 178
```

검증 항목:
- 구독 결제: `TOSS_NAVERPAY`, `INICIS_NAVERPAY`
- 주소 검색: `juso` 연동
- 주문 상태머신:
  - `PAYMENT_PENDING -> IN_PRODUCTION -> SHIPPING -> DELIVERED`

### 1-2. 실PG 전환 전 필수 운영값
- `SNAPFIT_BILLING_MOCK_MODE=false`
- `SNAPFIT_BILLING_TOSS_SECRET_KEY`
- `SNAPFIT_BILLING_WEBHOOK_TOSS_SECRET` (또는 `SNAPFIT_BILLING_WEBHOOK_SECRET`)
- `SNAPFIT_BILLING_WEBHOOK_INICIS_SECRET`
- `SNAPFIT_BILLING_PUBLIC_BASE_URL` (운영 도메인)

---

## 2) 주문/배송 운영 어드민 API

관리자 키 헤더: `X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY`

### 2-0. 운영 대시보드(통합 지표)
```bash
curl -sS "http://54.253.3.176/api/ops/admin/dashboard" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY"
```

### 2-1. 운영 요약
```bash
curl -sS "http://54.253.3.176/api/orders/admin/summary" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY"
```

### 2-2. 운영 목록(필터/검색/페이징)
```bash
curl -sS "http://54.253.3.176/api/orders/admin/paged?page=0&size=20&status=PAYMENT_PENDING,IN_PRODUCTION&keyword=010" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY"
```

### 2-3. 배송 처리
```bash
curl -sS -X POST "http://54.253.3.176/api/orders/{orderId}/shipping" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY" \
  -d '{"courier":"CJ대한통운","trackingNumber":"1234567890"}'
```

### 2-4. 배송완료 처리
```bash
curl -sS -X POST "http://54.253.3.176/api/orders/{orderId}/delivered" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY"
```

---

## 3) 템플릿 운영 파이프라인

코드 템플릿 하드코딩 대신 JSON 릴리즈 파이프라인 사용:

```bash
cd /Users/devsheep/SnapFit/SnapFit
./scripts/template_release_pipeline.sh \
  --store-json=assets/templates/generated/store_latest.json \
  --base-url=http://54.253.3.176 \
  --admin-key="$SNAPFIT_PUSH_ADMIN_KEY"
```

백엔드 워크스페이스에서 실행 시(래퍼):
```bash
cd /Users/devsheep/SnapFit/SnapFit-BackEnd
./scripts/template_release_pipeline.sh \
  --store-json=assets/templates/generated/store_latest.json \
  --base-url=http://54.253.3.176 \
  --admin-key="$SNAPFIT_PUSH_ADMIN_KEY"
```

동작:
1. `template_release_gate.dart` 품질 게이트
2. `publish_store_templates_to_server.dart` 서버 업서트
3. `/api/templates` 호출 검증

---

## 4) 결제 실운영 readiness 체크

```bash
curl -sS "http://54.253.3.176/api/billing/admin/readiness" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Admin-Key: $SNAPFIT_ORDER_ADMIN_KEY"
```

응답 예시:
- `readyForLive=true` 인 경우에만 `SNAPFIT_BILLING_MOCK_MODE=false` 전환 권장
- `checks[].ok=false` 항목이 남아있으면 실결제 전환 금지

---

## 5) 원클릭 통합 점검(1~3번)

```bash
cd /Users/devsheep/SnapFit/SnapFit-BackEnd
./scripts/run-phase123-check.sh \
  --base-url=http://54.253.3.176 \
  --user-id=1958142146 \
  --album-id=178 \
  --admin-key="$SNAPFIT_ORDER_ADMIN_KEY" \
  --access-token="$ACCESS_TOKEN"
```

설명:
- `Phase 1`: 결제/주문 mock E2E
- `Phase 2`: 운영 대시보드 API 점검
- `Phase 3`: 결제 실운영 readiness 체크

## 템플릿 게이트 실패 시
- `metadata.designWidth/designHeight`, `schemaVersion`, `templateId`, `version`, `lifecycleStatus` 필수값 채우기
- 각 페이지 `layoutId`, `role`, `recommendedPhotoCount` 채우기
- TEXT 레이어 `payload.textStyle.fontSizeRatio` 누락 제거
