# SnapFit Application DB Constraints & Indexes

이 문서는 전체 애플리케이션 DB의 제약조건, 유니크 조건, 인덱스, 운영상 권장 인덱스를 정리한다.
실제 기준은 JPA Entity와 운영 DB 상태다.

관련 문서:
- [App Database Spec](./APP_DATABASE_SPEC.md)
- [App Database ERD](./APP_DATABASE_ERD.md)
- [App API Spec](./APP_API_SPEC.md)

## 1. 이미 코드에 명시된 제약

### `user`
- PK: `id`

### `refresh_token`
- PK: `id`
- UNIQUE: `token`
- 실질 FK 개념: `user_id -> user.id`

### `album`
- PK: `id`
- 실질 owner key: `user_id`

### `album_page`
- PK: `id`
- FK: `album_id -> album.id`
- NOT NULL:
  - `album_id`
  - `page_number`
  - `layers_json`

### `album_member`
- PK: `id`
- FK: `album_id -> album.id`
- UNIQUE: `invite_token`
- ENUM string:
  - `role`
  - `status`

### `template`
- PK: `id`

### `template_like`
- PK: `id`
- UNIQUE: `(template_id, user_id)`

### `user_subscription`
- PK: `user_id`
- ENUM string:
  - `status`

### `billing_order`
- PK: `id`
- UNIQUE: `order_id`
- INDEX:
  - `idx_billing_order_user_created (user_id, created_at)`
  - `idx_billing_order_status_created (status, created_at)`
- ENUM string:
  - `status`

### `orders`
- PK: `id`
- UNIQUE: `order_id`
- INDEX:
  - `idx_orders_user_created (user_id, created_at)`
  - `idx_orders_status_created (status, created_at)`
- ENUM string:
  - `status`

### `notification_inbox`
- PK: `id`

### `notification_read`
- PK: `id`
- UNIQUE: `(notification_id, user_id)`

### `support_inquiries`
- PK: `id`
- INDEX:
  - `idx_support_inquiries_user_created (user_id, created_at)`
  - `idx_support_inquiries_status_created (status, created_at)`
- ENUM string:
  - `status`

## 2. 운영상 권장 인덱스

현재 코드나 `schema.sql`에 명시가 약한 테이블도 있어서, 아래 인덱스는 운영 안정성 기준으로 권장한다.

### `album`
- 권장 INDEX: `(user_id, updated_at desc)`
  - 홈/내 앨범 목록 조회 최적화
- 권장 INDEX: `(user_id, orders)`
  - 사용자 정렬 순서 조회 최적화

### `album_page`
- 권장 UNIQUE: `(album_id, page_number)`
  - 같은 앨범 내 페이지 번호 중복 방지
- 권장 INDEX: `(album_id, page_number)`
  - 페이지 순서 조회 최적화

### `album_member`
- 권장 UNIQUE: `(album_id, user_id)`
  - 같은 사용자 중복 초대 방지
- 권장 INDEX: `(user_id, status)`
  - 내가 초대받은 앨범 조회 최적화

### `template`
- 권장 INDEX: `(active, weekly_score desc, created_at desc)`
  - 스토어/summary 목록 최적화
- 권장 INDEX: `(active, category, created_at desc)`
  - 카테고리 필터 대비
- 권장 INDEX: `(new_until)`
  - NEW 배지 판별 보조

### `template_like`
- 권장 INDEX: `(user_id, created_at desc)`
  - 사용자 좋아요 목록 확장 대비

### `notification_inbox`
- 권장 INDEX: `(created_at desc)`
  - 최신 inbox 조회
- 권장 INDEX: `(type, created_at desc)`
  - 알림 타입별 운영 조회

### `notification_read`
- 권장 INDEX: `(user_id, read_at desc)`
  - 읽음 상태 조회

### `user_subscription`
- 권장 INDEX: `(status, expires_at)`
  - 구독 만료 배치 처리

## 3. Nullable / Required 체크 포인트

운영상 특히 주의할 필드:

### 거의 필수로 다뤄야 하는 값
- `album.user_id`
- `album.ratio`
- `album_page.layers_json`
- `template.cover_image_url`
- `template.template_json`
- `billing_order.order_id`
- `orders.order_id`
- `notification_inbox.type/title/body`
- `support_inquiries.category/title/content`

### 비어 있어도 되지만 운영상 채우는 것이 좋은 값
- `template.preview_images_json`
- `template.tags_json`
- `album.cover_layers_json`
- `orders.album_id`
- `billing_order.checkout_url`

## 4. 일관성 리스크

현재 구조에서 주의할 리스크:

### 사용자 식별자 혼합
- `user.id`는 `Long`
- 다수 도메인의 `user_id`는 `String`

영향:
- FK를 강하게 걸기 어렵다
- 배치/분석 쿼리에서 형 변환 이슈가 생길 수 있다

### JSON 컬럼 의존
- `template.preview_images_json`
- `template.tags_json`
- `template.template_json`
- `album.cover_layers_json`
- `album_page.layers_json`
- `notification_inbox.payload_json`

영향:
- 스키마 진화는 빠르지만 SQL 레벨 검증은 약하다
- 앱/백엔드 파서가 사실상 유효성 검증 레이어가 된다

## 5. 권장 후속 작업

1. `album_page (album_id, page_number)` 유니크 제약 추가
2. `album_member (album_id, user_id)` 유니크 제약 추가
3. `template active + score` 복합 인덱스 추가
4. `album user_id + updated_at` 인덱스 추가
5. 장기적으로 `user_id` 식별 체계 통합 검토
