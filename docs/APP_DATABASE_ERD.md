# SnapFit Application ERD

전체 애플리케이션 데이터 흐름을 도메인 단위로 빠르게 보기 위한 ERD 문서다.

관련 문서:
- [App Database Spec](./APP_DATABASE_SPEC.md)
- [App API Spec](./APP_API_SPEC.md)
- [App DB Constraints](./APP_DB_CONSTRAINTS.md)

## 1. High-Level ERD

```mermaid
erDiagram
    USER ||--o{ REFRESH_TOKEN : "refreshes"
    USER ||--o{ ALBUM : "owns"
    ALBUM ||--o{ ALBUM_PAGE : "contains"
    ALBUM ||--o{ ALBUM_MEMBER : "shared with"

    TEMPLATE ||--o{ TEMPLATE_LIKE : "liked by"
    TEMPLATE ||--o{ ALBUM : "creates"

    USER ||--|| USER_SUBSCRIPTION : "has"
    USER ||--o{ BILLING_ORDER : "billing"
    USER ||--o{ ORDERS : "orders"
    USER ||--o{ SUPPORT_INQUIRIES : "asks"

    NOTIFICATION_INBOX ||--o{ NOTIFICATION_READ : "read by"
```

## 2. Domain View

### 인증 / 사용자
- `user`
- `refresh_token`

### 컨텐츠
- `template`
- `template_like`
- `album`
- `album_page`
- `album_member`

### 상거래
- `user_subscription`
- `billing_order`
- `orders`

### 운영 / 커뮤니케이션
- `notification_inbox`
- `notification_read`
- `support_inquiries`

## 3. 핵심 흐름

### 템플릿 사용하기

```mermaid
flowchart LR
    T["template"] --> U["POST /api/templates/{id}/use"]
    U --> A["album"]
    U --> P["album_page[]"]
```

### 구독 결제

```mermaid
flowchart LR
    B["billing_order"] --> S["user_subscription"]
```

### 실물 주문

```mermaid
flowchart LR
    A["album"] --> O["orders"]
```

### 알림 읽음 처리

```mermaid
flowchart LR
    I["notification_inbox"] --> R["notification_read"]
```
