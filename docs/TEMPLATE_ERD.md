# SnapFit Template ERD

템플릿 운영에서 실제로 자주 보는 테이블 관계만 따로 정리한 문서다.

관련 문서:
- [Template DB Spec](./TEMPLATE_DB_SPEC.md)
- [Template Operations Guide](./TEMPLATE_OPERATIONS.md)

## 1. 핵심 관계

```mermaid
erDiagram
    TEMPLATE ||--o{ TEMPLATE_LIKE : "liked by"
    TEMPLATE ||--o{ ALBUM : "used to create"
    ALBUM ||--o{ ALBUM_PAGE : "contains"

    TEMPLATE {
        bigint id PK
        varchar title
        varchar sub_title
        text description
        varchar cover_image_url
        longtext preview_images_json
        int page_count
        int like_count
        int user_count
        boolean is_best
        boolean is_premium
        varchar category
        longtext tags_json
        int weekly_score
        datetime new_until
        boolean active
        longtext template_json
        datetime created_at
        datetime updated_at
    }

    TEMPLATE_LIKE {
        bigint id PK
        bigint template_id UK_PART
        varchar user_id UK_PART
        datetime created_at
    }

    ALBUM {
        bigint id PK
        varchar user_id
        varchar ratio
        varchar title
        varchar cover_image_url
        varchar cover_preview_url
        varchar cover_original_url
        longtext cover_layers_json
        varchar cover_theme
        int total_pages
        int target_pages
        int orders
        datetime created_at
        datetime updated_at
    }

    ALBUM_PAGE {
        bigint id PK
        bigint album_id FK
        int page_number
        longtext layers_json
        varchar image_url
        varchar preview_url
        varchar original_url
        varchar thumbnail_url
        datetime created_at
        datetime updated_at
    }
```

## 2. 테이블 역할 요약

### `template`
- 스토어 상품 원본
- 커버/미리보기/배지/정렬/활성 상태 보관
- 실제 레이아웃 원본은 `template_json`

### `template_like`
- 사용자별 좋아요 상태 원본
- `(template_id, user_id)` 유니크
- `template.like_count` 는 캐시

### `album`
- 템플릿 사용하기 시 생성되는 앨범 헤더
- 템플릿 커버가 여기로 복제됨

### `album_page`
- `template_json.pages[]` 가 실제 사용자 페이지로 저장된 결과

## 3. 데이터 흐름

```mermaid
flowchart LR
    A["Figma approved template"] --> B["template_json generated"]
    B --> C["template row upsert"]
    C --> D["GET /api/templates"]
    C --> E["GET /api/templates/summary"]
    C --> F["GET /api/templates/{id}"]
    F --> G["POST /api/templates/{id}/use"]
    G --> H["album row created"]
    H --> I["album_page rows created"]
    F --> J["POST /api/templates/{id}/like"]
    J --> K["template_like row toggle"]
```

## 4. 운영 시 가장 중요한 컬럼

| 테이블 | 컬럼 | 이유 |
|---|---|---|
| `template` | `active` | 스토어 노출 on/off 기준 |
| `template` | `cover_image_url` | 카드/상세 대표 이미지 |
| `template` | `preview_images_json` | 페이지 미리보기 원본 |
| `template` | `template_json` | 실제 생성 로직의 원본 |
| `template` | `weekly_score` | summary/list 정렬 핵심 |
| `template` | `new_until` | NEW 배지 계산 |
| `album` | `ratio` | 세로/정사각/가로 비율 복원 |
| `album_page` | `layers_json` | 페이지 레이아웃 원본 |

## 5. 운영 포인트

- 템플릿 삭제는 `template_like` 정리까지 같이 일어난다.
- 템플릿 비노출만 원하면 `active=false` 가 우선이다.
- 템플릿 변경 영향은 `template` 뿐 아니라 생성 이후 `album`, `album_page` 결과까지 같이 봐야 한다.
