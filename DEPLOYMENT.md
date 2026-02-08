# SnapFit Backend 배포 가이드

## 실제 운영 환경 배포 방법

### 1. 클라우드 서버 선택

**추천 옵션:**
- **AWS EC2** (가장 일반적)
- **Google Cloud Platform (GCP) Compute Engine**
- **Azure VM**
- **DigitalOcean Droplet** (간단하고 저렴)

**최소 사양:**
- CPU: 2코어 이상
- RAM: 2GB 이상
- 스토리지: 20GB 이상
- OS: Ubuntu 22.04 LTS 또는 Amazon Linux 2

---

### 2. 서버 초기 설정

#### 2.1 Java 17 설치
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk -y

# Amazon Linux 2
sudo amazon-linux-extras install java-openjdk17 -y

# 확인
java -version
```

#### 2.2 MySQL 설치 및 설정
```bash
# Ubuntu/Debian
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# MySQL 보안 설정
sudo mysql_secure_installation

# 데이터베이스 생성
sudo mysql -u root -p
CREATE DATABASE snapfit_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'snapfit_prod'@'localhost' IDENTIFIED BY '강력한비밀번호';
GRANT ALL PRIVILEGES ON snapfit_prod.* TO 'snapfit_prod'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

### 3. 애플리케이션 빌드 및 배포

#### 3.1 로컬에서 JAR 빌드
```bash
# 프로젝트 루트에서
./gradlew clean build -x test

# 빌드된 JAR 확인
ls -lh build/libs/*.jar
```

#### 3.2 서버로 파일 전송
```bash
# SCP로 전송 (예시)
scp build/libs/snapfit-backend-0.0.1-SNAPSHOT.jar user@서버IP:/home/user/
scp src/main/resources/application.yml user@서버IP:/home/user/
```

#### 3.3 서버에서 실행
```bash
# 서버 접속
ssh user@서버IP

# 애플리케이션 디렉토리 생성
mkdir -p /opt/snapfit-backend
cd /opt/snapfit-backend

# JAR 파일 이동
mv ~/snapfit-backend-0.0.1-SNAPSHOT.jar app.jar

# 환경 변수 설정 파일 생성
nano .env
# 내용:
# DB_USERNAME=snapfit_prod
# DB_PASSWORD=실제비밀번호
# FIREBASE_SERVICE_ACCOUNT_FILE=/opt/snapfit-backend/firebase-service-account.json
# FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com
# SPRING_PROFILES_ACTIVE=prod
```

---

### 4. Systemd 서비스 설정 (자동 시작/재시작)

```bash
sudo nano /etc/systemd/system/snapfit-backend.service
```

**파일 내용:**
```ini
[Unit]
Description=SnapFit Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/snapfit-backend
EnvironmentFile=/opt/snapfit-backend/.env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /opt/snapfit-backend/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

**서비스 시작:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable snapfit-backend
sudo systemctl start snapfit-backend
sudo systemctl status snapfit-backend
```

---

### 5. Nginx 리버스 프록시 설정 (HTTPS + 도메인)

#### 5.1 Nginx 설치
```bash
sudo apt install nginx -y
```

#### 5.2 Nginx 설정
```bash
sudo nano /etc/nginx/sites-available/snapfit-backend
```

**파일 내용:**
```nginx
server {
    listen 80;
    server_name api.snapfit.com;  # 실제 도메인으로 변경

    # HTTPS로 리다이렉트
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.snapfit.com;  # 실제 도메인으로 변경

    # SSL 인증서 (Let's Encrypt 사용)
    ssl_certificate /etc/letsencrypt/live/api.snapfit.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.snapfit.com/privkey.pem;

    # SSL 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 프록시 설정
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

**심볼릭 링크 생성 및 Nginx 재시작:**
```bash
sudo ln -s /etc/nginx/sites-available/snapfit-backend /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

#### 5.3 Let's Encrypt SSL 인증서 발급
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d api.snapfit.com
```

---

### 6. 방화벽 설정

```bash
# UFW 방화벽 설정 (Ubuntu)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# AWS Security Group 설정
# - 인바운드 규칙:
#   - SSH (22): 내 IP만
#   - HTTP (80): 0.0.0.0/0
#   - HTTPS (443): 0.0.0.0/0
#   - 커스텀 TCP (8080): localhost만 (Nginx가 프록시하므로)
```

---

### 7. 모니터링 및 로그 확인

```bash
# 애플리케이션 로그 확인
sudo journalctl -u snapfit-backend -f

# Nginx 로그 확인
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# 시스템 리소스 확인
htop
df -h
```

---

### 8. 배포 자동화 (선택사항)

#### GitHub Actions 예시
`.github/workflows/deploy.yml`:
```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build JAR
        run: ./gradlew clean build -x test
      - name: Deploy to Server
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_KEY }}
          source: "build/libs/*.jar"
          target: "/opt/snapfit-backend/"
      - name: Restart Service
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            sudo systemctl restart snapfit-backend
```

---

### 9. 운영 체크리스트

- [ ] 서버 보안 그룹/방화벽 설정 완료
- [ ] MySQL 데이터베이스 생성 및 사용자 설정 완료
- [ ] 환경 변수(.env) 설정 완료
- [ ] Firebase 서비스 계정 키 파일 업로드 완료
- [ ] Systemd 서비스 등록 및 자동 시작 설정 완료
- [ ] Nginx 리버스 프록시 설정 완료
- [ ] SSL 인증서 발급 완료 (Let's Encrypt)
- [ ] 도메인 DNS A 레코드 설정 완료
- [ ] CORS 설정에서 실제 프론트엔드 도메인만 허용하도록 수정
- [ ] 로그 모니터링 설정 완료
- [ ] 백업 전략 수립 (DB, 파일 등)

---

### 10. 프론트엔드 설정

프론트엔드에서 API 호출 시:
```dart
// Flutter 예시
final baseUrl = 'https://api.snapfit.com';  // 실제 도메인
// 또는 개발 환경: 'http://서버IP:8080'
```

---

## 문제 해결

### 서비스가 시작되지 않을 때
```bash
sudo journalctl -u snapfit-backend -n 50
```

### 포트가 이미 사용 중일 때
```bash
sudo lsof -i :8080
sudo kill -9 <PID>
```

### 데이터베이스 연결 실패
- MySQL이 실행 중인지 확인: `sudo systemctl status mysql`
- 방화벽에서 MySQL 포트(3306) 허용 확인
- 사용자 권한 확인: `SHOW GRANTS FOR 'snapfit_prod'@'localhost';`
