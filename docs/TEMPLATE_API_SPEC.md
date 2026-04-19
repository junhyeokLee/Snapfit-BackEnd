# SnapFit Template API Spec

템플릿 운영에 필요한 API만 빠르게 보기 위한 명세서다.

관련 문서:
- [Template DB Spec](./TEMPLATE_DB_SPEC.md)
- [Template ERD](./TEMPLATE_ERD.md)
- [Template Operations Guide](./TEMPLATE_OPERATIONS.md)

## 1. Public API

### 1.1 템플릿 전체 목록

- `GET /api/templates`

Query:
- `userId` optional

용도:
- 앱 스토어의 전체 템플릿 목록
- 상세 진입 전 기본 데이터 확보

응답 특징:
- `active=true` 템플릿만 반환
- 정렬 기준:
  1. `weeklyScore desc`
  2. `likeCount desc`
  3. `userCount desc`
  4. `createdAt desc`
  5. `id desc`

주요 필드:
- `id`
- `title`
- `subTitle`
- `description`
- `coverImageUrl`
- `previewImages`
- `pageCount`
- `category`
- `tags`
- `weeklyScore`
- `isNew`
- `isBest`
- `isPremium`
- `isLiked`
- `templateJson`

### 1.2 템플릿 summary 목록

- `GET /api/templates/summary`

Query:
- `userId` optional
- `page` default `0`
- `size` default `20`, max `50`

용도:
- 홈 추천/스토어 피드
- 가벼운 리스트 화면

응답 구조:
- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`
- `hasNext`

주요 필드:
- `id`
- `title`
- `coverImageUrl`
- `tags`
- `weeklyScore`
- `likeCount`
- `userCount`
- `isPremium`
- `isBest`
- `isNew`
- `isLiked`

### 1.3 템플릿 상세

- `GET /api/templates/{id}`

Query:
- `userId` optional

용도:
- 상세 화면
- 페이지 미리보기/템플릿 사용하기 진입

### 1.4 좋아요 토글

- `POST /api/templates/{id}/like`

Query:
- `userId` required

동작:
- 좋아요가 있으면 삭제
- 없으면 생성
- `template.likeCount` 같이 증감

### 1.5 템플릿 사용하기

- `POST /api/templates/{id}/use`

Query:
- `userId` required

Body:
- `Map<String, String>` optional
- 템플릿 레이어 치환값 전달 용도

동작:
1. 템플릿 active 확인
2. premium이면 구독 확인
3. `templateJson` 파싱
4. `album` 생성
5. `pages[].layers` 를 `album_page` 로 저장
6. `template.userCount + 1`

## 2. Admin API

인증:
- `X-Admin-Key`

### 2.1 업서트

- `POST /api/templates/admin/upsert`
- 대체 경로: `POST /api/admin/templates/upsert`

필수 body 필드:
- `title`
- `coverImageUrl`
- `pageCount`
- `templateJson`

권장 body 필드:
- `previewImagesJson`
- `subTitle`
- `description`
- `category`
- `tagsJson`
- `weeklyScore`
- `newUntil`
- `active`
- `isBest`
- `isPremium`

응답:
- `{ "id": <templateId> }`

### 2.2 관리자 페이지 목록

- `GET /api/templates/admin/paged`
- 대체 경로: `GET /admin/templates/paged`

Query:
- `page`
- `size`

용도:
- 운영용 템플릿 관리 화면

### 2.3 관리자 상세

- `GET /api/templates/admin/{id}`
- 대체 경로: `GET /admin/templates/{id}`

### 2.4 초안 검증

- `POST /admin/templates/validate`

용도:
- 업서트 전 서버 측 기본 형식 검증

검증 항목:
- `title` 존재
- `coverImageUrl` 존재
- `pageCount > 0`
- `templateJson` 존재 및 JSON 파싱 가능
- `templateJson.cover.layers` array
- `templateJson.pages` array
- `previewImagesJson`, `tagsJson` 는 JSON array 형식

### 2.5 활성/비활성

- `POST /api/templates/admin/{id}/active`
- 대체 경로: `POST /admin/templates/{id}/active`

Body:
```json
{ "active": true }
```

용도:
- DB는 유지하고 스토어 노출만 제어

### 2.6 삭제

- `DELETE /api/admin/templates/{id}`
- 대체 경로: `DELETE /admin/templates/{id}`

동작:
- `template` row 삭제
- 연결된 `template_like` row 삭제

응답 예시:
```json
{ "id": 38, "deleted": true, "deletedLikes": 1 }
```

## 3. 필드 사용 기준

### `coverImageUrl`
- 스토어 카드 대표 이미지
- 템플릿 상세 상단 대표 이미지
- 앨범 생성 시 초기 커버 URL로 복제

### `previewImagesJson`
- 상세 화면의 페이지 미리보기 소스
- 템플릿 QA용 대표 페이지 모음

### `templateJson`
- 실제 앨범 생성의 원본
- 커버/페이지/레이어 구조 전체 포함

### `weeklyScore`
- 홈/스토어 summary 랭킹 핵심

### `newUntil`
- 현재 시각보다 미래면 `isNew=true`

### `active`
- `false`면 public API에서 노출 금지

## 4. 운영 권장 순서

1. Figma 승인본 완성
2. 템플릿 이미지 CDN 업로드
3. `templateJson` / `previewImagesJson` / `coverImageUrl` 준비
4. Admin validate
5. Admin upsert
6. `/api/templates`, `/api/templates/summary`, `/api/templates/{id}` 확인
7. `POST /api/templates/{id}/use` 로 실제 생성 확인

## 5. 주의사항

- 현재 프로젝트는 `ddl-auto: update` 이므로 코드가 스키마 기준이다.
- 템플릿 목록과 summary는 모두 `active` 상태를 본다.
- 앱에서 로컬 템플릿을 섞지 않도록 운영 원칙을 유지해야 한다.
- 삭제보다 `active=false`가 기본 운영 방식이고, 완전 제거만 DELETE를 쓴다.
