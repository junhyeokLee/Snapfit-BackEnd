# SnapFit Release/Ops Runbook

## 1) 배포 트랙
- `DEV`: `.github/workflows/deploy-backend-dev.yml`
- `PROD`: `.github/workflows/deploy-backend-prod.yml`

두 워크플로우 모두 배포 전에 `scripts/run-predeploy-gate.sh`를 실행합니다.

## 2) Pre-deploy QA Gate
`run-predeploy-gate.sh`는 아래 순서로 검증합니다.

1. `./gradlew test`
2. 스모크 API 확인 (`GET /api/templates`)
3. 선택: full phase check (`scripts/run-phase123-check.sh`)
   - 결제/주문 mock E2E
   - 운영 대시보드 확인
   - billing readiness 확인

## 3) GitHub Secrets (필수)

### DEV 배포
- `DEV_SERVER_HOST`
- `DEV_SERVER_USER`
- `DEV_SERVER_PORT`
- `DEV_SSH_KEY`
- `DEV_DEPLOY_PATH`
- `SNAPFIT_DEV_BASE_URL`
- `SNAPFIT_QA_USER_ID`
- `SNAPFIT_QA_ALBUM_ID`
- `SNAPFIT_ORDER_ADMIN_KEY`
- `SNAPFIT_QA_ACCESS_TOKEN`

### PROD 배포
- `SERVER_HOST`
- `SERVER_USER`
- `SERVER_PORT`
- `SSH_KEY`
- `DEPLOY_PATH`
- `SNAPFIT_DEV_BASE_URL` (prod 배포 전 gate 대상)
- `SNAPFIT_QA_USER_ID`
- `SNAPFIT_QA_ALBUM_ID`
- `SNAPFIT_ORDER_ADMIN_KEY`
- `SNAPFIT_QA_ACCESS_TOKEN`

## 4) 운영 권장 순서
1. `develop` 브랜치에서 DEV 배포 및 QA 확인
2. 운영센터에서 주문/결제/CS 시그널 확인
3. `workflow_dispatch`로 PROD 배포
4. 배포 직후 `/api/templates`, `/api/ops/admin/dashboard` 확인

## 4-1) GitHub Environment 보호 규칙 (필수)
- `prod` environment에 Required reviewers를 설정합니다.
- 배포 워크플로우 입력값 `confirm=DEPLOY_PROD`가 아니면 실행되지 않습니다.
- PROD 배포는 동시 실행되지 않도록 `concurrency: snapfit-backend-prod`가 적용되어 있습니다.

## 5) 관리자 운영센터 (앱)
마이페이지 > `운영센터(관리자)`에서:
- 대시보드 지표 확인
- 주문 상태 전환 (`배송중`, `배송완료`)
- CS 대응 로그 확인
- 템플릿 수동 등록(upsert)

`X-Admin-Key`는 백엔드 `SNAPFIT_ORDER_ADMIN_KEY`와 동일해야 합니다.
