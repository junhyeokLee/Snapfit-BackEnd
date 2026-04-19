# Branch Protection Setup (DEV/PROD)

GitHub Repository Settings > Branches 에서 아래 규칙을 설정합니다.

## 1) `develop` 보호 규칙
- Branch name pattern: `develop`
- Require a pull request before merging: ON
- Require approvals: 1 이상
- Dismiss stale pull request approvals when new commits are pushed: ON
- Require status checks to pass before merging: ON
  - Required checks:
    - `CI / Gradle Tests`
- Require conversation resolution before merging: ON
- Restrict who can push to matching branches: ON (관리자 제외)

## 2) `main` 보호 규칙
- Branch name pattern: `main`
- Require a pull request before merging: ON
- Require approvals: 2 권장
- Require review from Code Owners: ON
- Dismiss stale pull request approvals when new commits are pushed: ON
- Require status checks to pass before merging: ON
  - Required checks:
    - `CI / Gradle Tests`
- Require conversation resolution before merging: ON
- Require signed commits: 선택
- Restrict who can push to matching branches: ON

## 3) Environment 보호 규칙

### `dev` environment
- Required reviewers: 팀 리더 1인 이상 권장
- Deployment branches: `develop`, `dev`

### `prod` environment
- Required reviewers: 필수 (최소 1~2인)
- Wait timer: 5~15분 권장
- Deployment branches: `main`

`deploy-backend-prod.yml`는 입력값 `confirm=DEPLOY_PROD`가 아니면 실행되지 않습니다.

## 4) 운영 원칙
1. 기능은 `feature/*` → `develop` 머지
2. DEV 배포/QA 통과 후 `main` 승격
3. PROD는 수동 실행 + reviewer 승인 후 배포
4. 배포 후 스모크 체크 실패 시 즉시 롤백

