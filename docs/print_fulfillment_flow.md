# Print Fulfillment Flow (MVP)

## 상태 흐름

`PAYMENT_PENDING -> PAYMENT_COMPLETED -> IN_PRODUCTION -> SHIPPING -> DELIVERED`

취소는 어느 단계에서든 `CANCELED`로 전이할 수 있습니다.

## 현재 구현 방식

- 결제 확정 시 인쇄소 접수는 `PrintVendorAdapter`를 통해 수행됩니다.
- 기본 어댑터는 `ManualPrintVendorAdapter`이며, 운영자가 실제 인쇄소에 접수하는 구조입니다.
- 인쇄소 API가 확정되면 `PrintVendorAdapter` 구현체만 교체하면 됩니다.

## API

### 1) 주문 생성 (주소 포함)

`POST /api/orders`

```json
{
  "userId": "1958142146",
  "albumId": 178,
  "title": "우리 가족 포토북",
  "amount": 34900,
  "recipientName": "홍길동",
  "recipientPhone": "01012345678",
  "zipCode": "06236",
  "addressLine1": "서울시 강남구 테헤란로 1",
  "addressLine2": "101호",
  "deliveryMemo": "문 앞에 놓아주세요"
}
```

### 2) 결제 확정 + 인쇄 접수

`POST /api/orders/{orderId}/payment/confirm`

- 상태를 `PAYMENT_COMPLETED`로 변경
- `PrintVendorAdapter.submit()` 호출
- 성공 시 `IN_PRODUCTION`으로 전이

### 3) 배송 시작 (관리자)

`POST /api/orders/{orderId}/shipping`

Headers:

- `X-Admin-Key: {SNAPFIT_ORDER_ADMIN_KEY}`

Body:

```json
{
  "courier": "CJ대한통운",
  "trackingNumber": "1234567890"
}
```

### 4) 배송 완료 (관리자)

`POST /api/orders/{orderId}/delivered`

Headers:

- `X-Admin-Key: {SNAPFIT_ORDER_ADMIN_KEY}`

