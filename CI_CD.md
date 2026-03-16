# SnapFit Backend CI/CD 가이드

이 저장소는 GitHub Actions로 테스트와 배포를 자동화합니다.

## 1) CI (테스트 자동화)

- `main`, `master` 브랜치로 push 시
- PR 생성/업데이트 시

워크플로우 파일:
- `.github/workflows/ci.yml`

실행 내용:
- `./gradlew test`

## 2) CD (배포 자동화)

워크플로우 파일:
- `.github/workflows/deploy-backend.yml`

실행 방법:
- GitHub Actions 탭 → `Deploy Backend` → `Run workflow`

### 필요한 GitHub Secrets

GitHub 저장소 Settings → Secrets and variables → Actions 에 아래 값을 등록합니다.

- `SERVER_HOST`: 서버 도메인 또는 IP
- `SERVER_USER`: SSH 유저명
- `SSH_KEY`: 배포용 SSH private key
- `SERVER_PORT`: SSH 포트 (예: 22)
- `DEPLOY_PATH`: 서버 내 배포 경로 (예: `/opt/snapfit-backend/`)
