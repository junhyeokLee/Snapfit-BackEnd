# SnapFit Template Operations Guide

관련 문서:
- [Template DB Spec](./TEMPLATE_DB_SPEC.md)
- [Template ERD](./TEMPLATE_ERD.md)
- [Template API Spec](./TEMPLATE_API_SPEC.md)

## 1) Runtime Model
Template API is server-driven. Store ranking and badges should come from backend fields:
- `category`
- `tagsJson`
- `weeklyScore`
- `newUntil` -> mapped to `isNew`
- `active`

## 2) Seed Strategy
`TemplateDataLoader` now performs upsert behavior on startup:
- Creates missing seed templates
- Updates design metadata/layout for existing seed titles
- Preserves service metrics with monotonic merge (`likeCount`, `userCount`)

## 3) Style Families (6)
- `travel`
- `family`
- `couple`
- `graduation`
- `retro`
- `minimal`

## 4) Admin Upsert API
- Endpoint: `POST /api/admin/templates/upsert`
- Auth: `X-Admin-Key` header (`SNAPFIT_PUSH_ADMIN_KEY`)
- Validations:
  - title required
  - coverImageUrl required
  - pageCount > 0
  - templateJson valid and includes `cover.layers[]` and `pages[]`
  - `previewImagesJson` / `tagsJson` must be JSON arrays when provided

## 5) Recommended Release Process
1. Add/modify template payload in seed or admin upsert JSON
2. Run release pipeline (quality gate + publish + verify)
3. Verify `/api/templates` response includes metadata
4. Check store sorting behavior (`weeklyScore`, `isNew`)
5. Create album from template and inspect cover/page rendering
6. Send `template_new` push after publish

```bash
# frontend workspace
cd /Users/devsheep/SnapFit/SnapFit
./scripts/template_release_pipeline.sh \
  --store-json=assets/templates/generated/store_latest.json \
  --base-url=http://54.253.3.176 \
  --admin-key="$SNAPFIT_PUSH_ADMIN_KEY"
```

```bash
# backend workspace wrapper
cd /Users/devsheep/SnapFit/SnapFit-BackEnd
./scripts/template_release_pipeline.sh \
  --store-json=assets/templates/generated/store_latest.json \
  --base-url=http://54.253.3.176 \
  --admin-key="$SNAPFIT_PUSH_ADMIN_KEY"
```

## 6) Weekly Best / NEW Policy
- NEW: `newUntil > now`
- Weekly best score baseline:
  - `weeklyScore` (manual override)
  - fallback: likes/users + best bonus
