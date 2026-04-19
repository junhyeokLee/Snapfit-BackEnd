# SnapFit Application API Spec

이 문서는 SnapFit 백엔드의 앱 전체 API를 도메인 단위로 빠르게 보기 위한 명세서다.
실제 기준은 컨트롤러 코드이며, 운영에서 자주 쓰는 공개 API와 관리자 API를 함께 정리한다.

관련 문서:
- [App Database Spec](./APP_DATABASE_SPEC.md)
- [App Database ERD](./APP_DATABASE_ERD.md)
- [App DB Constraints](./APP_DB_CONSTRAINTS.md)
- [Template API Spec](./TEMPLATE_API_SPEC.md)

기준 소스:
- `src/main/java/com/snapfit/snapfitbackend/controller/*.java`

## 1. 인증 / 사용자

Base:
- `/api/auth`

주요 API:
- `POST /login/kakao`
- `POST /login/google`
- `POST /refresh`
- `PATCH|PUT /profile`
- `DELETE /account`
- `POST /consents`

주요 용도:
- 소셜 로그인
- access/refresh token 갱신
- 프로필 수정
- 회원 탈퇴
- 약관/마케팅 동의 저장

주요 DB:
- `user`
- `refresh_token`

## 2. 앨범 / 페이지 / 협업

Base:
- `/api/albums`

주요 API:
- `POST /`
- `GET /`
- `GET /{albumId}`
- `PUT /{albumId}`
- `DELETE /{albumId}`
- `GET /{albumId}/pages`
- `POST /{albumId}/pages`
- `PATCH /reorder`
- `POST /{albumId}/lock`
- `POST /{albumId}/unlock`
- `POST /{albumId}/members/invite`
- `GET /{albumId}/members`

초대 관련:
- `GET /api/invites/{token}`
- `POST /api/invites/{token}/accept`
- `GET /invite`
- `GET /invite/preview.png`

주요 용도:
- 앨범 생성/조회/수정/삭제
- 페이지 저장
- 앨범 순서 변경
- 잠금 처리
- 공동 편집 초대

주요 DB:
- `album`
- `album_page`
- `album_member`

## 3. 템플릿 / 스토어

Base:
- `/api/templates`

공개 API:
- `GET /`
- `GET /summary`
- `GET /{id}`
- `POST /{id}/like`
- `POST /{id}/use`

관리자 API:
- `POST /admin/upsert`
- `GET /admin/paged`
- `GET /admin/{id}`
- `POST /admin/{id}/active`

대체 관리자 API:
- `/api/admin/templates/*`

주요 용도:
- 스토어 목록
- 요약 피드
- 상세
- 좋아요 토글
- 템플릿으로 앨범 생성
- 템플릿 업서트/활성화/삭제

주요 DB:
- `template`
- `template_like`
- `album`
- `album_page`

## 4. 구독 / 결제

Base:
- `/api/billing`

주요 API:
- `GET /plans`
- `GET /admin/readiness`
- `GET /subscription`
- `POST /subscription/cancel`
- `GET /storage/quota`
- `POST /storage/preflight`
- `POST /prepare`
- `POST /approve`
- `POST /{orderId}/cancel`
- `POST /webhook/{provider}`
- `POST /naverpay/prepare`
- `POST /naverpay/approve`
- `POST /naverpay/webhook`
- `GET /mock/checkout`
- `GET /return/success`
- `GET /return/fail`

주요 용도:
- 구독 플랜 조회
- 결제 준비/승인
- 구독 상태 조회/해지
- 웹훅 수신
- 저장용량 사전 점검

주요 DB:
- `user_subscription`
- `billing_order`

## 5. 실물 주문 / 배송

Base:
- `/api/orders`

주요 API:
- `GET /`
- `GET /paged`
- `GET /summary`
- `POST /`
- `GET /quote`
- `GET /address/search`
- `GET /{orderId}/payment/checkout`
- `POST /{orderId}/payment/confirm`
- `POST /{orderId}/shipping`
- `POST /{orderId}/delivered`
- `POST /{orderId}/advance`
- `POST /{orderId}/status`
- `GET /status-options`

관리자/운영:
- `GET /admin/paged`
- `GET /admin/summary`

주요 용도:
- 포토북 주문 생성
- 예상 견적 조회
- 결제 확인
- 배송 상태 전이
- 관리자 주문 조회

주요 DB:
- `orders`
- `album`

## 6. 알림

Base:
- `/api/notifications`

주요 API:
- `POST /template-new`
- `POST /order-status`
- `POST /comment`
- `GET /health`
- `POST /topic`
- `GET /inbox`
- `GET /unread-count`
- `GET /policy`
- `POST /{notificationId}/read`
- `POST /read-all`

주요 용도:
- 알림 발송
- 토픽 등록
- 사용자 inbox 조회
- 읽음 처리

주요 DB:
- `notification_inbox`
- `notification_read`

## 7. 고객지원

Base:
- `/api/support`

주요 API:
- `POST /inquiries`

관리자/운영:
- `GET /api/admin/inquiries/paged`
- `POST /api/admin/inquiries/{id}/resolve`

주요 용도:
- 문의 등록
- 문의 목록 조회
- 문의 해결 처리

주요 DB:
- `support_inquiries`

## 8. 관리자 운영 / 대시보드

Base:
- `/api/admin`
- `/api/ops`

주요 API:
- `GET /dashboard`
- `GET /cs-signals`
- `GET /orders/paged`
- `POST /orders/{orderId}/shipping`
- `POST /orders/{orderId}/delivered`
- `GET /inquiries/paged`
- `POST /inquiries/{id}/resolve`
- `GET /templates/paged`
- `GET /templates/{id}`
- `POST /templates/upsert`
- `POST /templates/validate`
- `POST /templates/ai-draft`
- `POST /templates/{id}/active`
- `DELETE /templates/{id}`
- `GET /api/ops/admin/dashboard`
- `GET /api/ops/admin/cs-signals`

주요 용도:
- 관리자 웹 대시보드
- 주문/문의/템플릿 운영
- 템플릿 초안 검증

## 9. API와 DB 연결 포인트

핵심 흐름:
- 로그인: `user`, `refresh_token`
- 템플릿 사용하기: `template -> album -> album_page`
- 구독 결제: `billing_order -> user_subscription`
- 실물 주문: `album -> orders`
- 알림 읽음: `notification_inbox -> notification_read`
- 문의 해결: `support_inquiries.status/resolved_*`

## 10. 운영 주의사항

- 일부 API는 `String userId`를, 일부 내부 관계는 `Long id`를 사용한다.
- 관리자 API는 `X-Admin-Key` 또는 별도 운영 경로를 사용한다.
- `ddl-auto=update` 환경이라 실제 필드/제약 확인은 엔티티와 DB를 함께 봐야 한다.
- 템플릿, 주문, 결제는 운영 스크립트와 문서가 분리돼 있으니 배포 전에 관련 문서를 같이 확인하는 것이 좋다.
