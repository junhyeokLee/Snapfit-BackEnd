# AWS 배포 가이드 (한국, 비용 최소화)

## 💰 비용 예상 (월간)

- **EC2 t3.micro (프리티어)**: 무료 (1년간) → 이후 약 15,000원/월
- **Elastic IP**: 무료 (EC2에 연결 시)
- **데이터 전송**: 처음 1GB 무료, 이후 GB당 약 100원
- **총 예상**: 첫 1년 무료, 이후 월 15,000~20,000원

---

## 1단계: AWS 계정 생성 및 EC2 인스턴스 생성

### 1.1 AWS 계정 생성
1. https://aws.amazon.com/ 접속
2. "계정 만들기" 클릭
3. 신용카드 등록 (프리티어 사용 시에도 필요, 하지만 1년간 무료)

### 1.2 EC2 인스턴스 생성
1. AWS 콘솔 로그인 → **서비스** → **EC2** 클릭
2. 왼쪽 메뉴에서 **인스턴스** → **인스턴스 시작** 클릭

**설정 단계별:**

#### 이름 및 태그
- **이름**: `snapfit-backend`

#### 애플리케이션 및 OS 이미지
- **Amazon Linux 2023** 선택 (무료)

#### 인스턴스 유형
- **t3.micro** 선택 (프리티어 가능, 무료)
  - vCPU: 2
  - 메모리: 1GB
  - 네트워크 성능: 최대 5Gbps

#### 키 페어 (로그인용)
- **새 키 페어 생성** 클릭
- 키 페어 이름: `snapfit-key`
- 키 페어 유형: **RSA**
- 프라이빗 키 파일 형식: **.pem**
- **키 페어 생성** 클릭 → 자동으로 다운로드됨
- ⚠️ **중요**: 이 파일을 안전한 곳에 보관 (다시 다운로드 불가능)

#### 네트워크 설정
- **보안 그룹**: 새 보안 그룹 생성
- **이름**: `snapfit-backend-sg`
- **설명**: SnapFit Backend Security Group
- **인바운드 규칙 추가**:
  - **SSH (22)**: 
    - ⚠️ **처음에는 "어디서나(Anywhere-IPv4)" 또는 "0.0.0.0/0" 선택** (접속 테스트용)
    - 나중에 보안 강화 시 특정 IP만 허용 가능
  - **HTTP (80)**: 어디서나(Anywhere-IPv4) 또는 0.0.0.0/0
  - **HTTPS (443)**: 어디서나(Anywhere-IPv4) 또는 0.0.0.0/0
  - **커스텀 TCP (8080)**: 어디서나(Anywhere-IPv4) 또는 0.0.0.0/0 (개발용)

#### 스토리지 구성
- **볼륨 크기**: 8GB (프리티어 무료)
- **볼륨 유형**: gp3 (기본값)

#### 고급 세부 정보
- 건드리지 않아도 됨

#### 인스턴스 시작
- **인스턴스 시작** 클릭
- 잠시 후 인스턴스가 생성됨

---

## 2단계: Elastic IP 할당 (고정 IP 주소)

### 2.1 Elastic IP 생성
1. EC2 콘솔 → 왼쪽 메뉴 **네트워크 및 보안** → **Elastic IP** 클릭
2. 오른쪽 상단 **Elastic IP 주소 할당** 버튼 클릭
3. **Elastic IP 주소 할당** 화면이 나타남:
   - **네트워크 경계 그룹**: 기본값 그대로 (리전 자동 선택)
   - **퍼블릭 IPv4 주소 풀**: **"Amazon의 IPv4 주소 풀"**이 기본값으로 선택되어 있음
     - ⚠️ 만약 이 옵션이 보이지 않으면 그냥 기본값으로 두고 진행하면 됩니다
   - **태그**: 건드리지 않아도 됨
4. 오른쪽 하단 **할당** 버튼 클릭
5. 잠시 후 생성 완료 → 목록에서 방금 생성된 Elastic IP 선택
6. 상단 **작업** 버튼 클릭 → **Elastic IP 주소 연결** 선택
7. **인스턴스** 드롭다운에서 방금 만든 `snapfit-backend` 선택
8. **연결** 버튼 클릭

**이제 이 IP 주소가 서버의 고정 주소입니다!** (예: `54.123.45.67`)

---

## 3단계: 서버 접속 및 초기 설정

### 3.1 서버 접속 (Mac/Linux)
터미널에서:
```bash
# 키 파일 권한 설정 (처음 한 번만)
chmod 400 snapfit-key.pem

# 서버 접속 (Elastic IP 주소 사용)
ssh -i snapfit-key.pem ec2-user@54.253.3.176
```

**⚠️ 첫 접속 시 호스트 키 확인 메시지가 나타납니다:**
```
The authenticity of host '54.253.3.176' can't be established.
Are you sure you want to continue connecting (yes/no/[fingerprint])?
```
→ **`yes` 입력하고 Enter** (이것은 정상적인 보안 절차입니다)

**성공하면 다음과 같이 표시됩니다:**
```
       __|  __|_  )
       _|  (     /   Amazon Linux 2023
      ___|\___|___|

[ec2-user@ip-xxx-xx-xx-xx ~]$
```

**Windows 사용자:**
- PuTTY 또는 Windows Terminal 사용
- 키 파일을 `.ppk` 형식으로 변환 필요

### 3.2 서버 업데이트
```bash
# 서버 접속 후
sudo dnf update -y
```

---

## 4단계: Java 17 설치

```bash
# Amazon Linux 2023에서 Java 17 설치
sudo dnf install java-17-amazon-corretto -y

# 설치 확인
java -version
# 출력 예시: openjdk version "17.0.x" ...
```

---

## 5단계: MySQL 설치 및 데이터베이스 생성

```bash
# Amazon Linux 2023: MySQL 대신 MariaDB 사용(호환)
sudo dnf install mariadb105-server -y

# MariaDB 시작 및 자동 시작 설정
sudo systemctl enable --now mariadb

# MariaDB 보안 설정
sudo mysql_secure_installation
# 질문들:
# - 비밀번호 정책: Y
# - 비밀번호 강도: 0 (간단하게)
# - 새 비밀번호 입력: (예: SnapFit2024!)
# - 비밀번호 확인: (다시 입력)
# - 익명 사용자 제거: Y
# - 원격 root 로그인 비활성화: Y
# - test 데이터베이스 제거: Y
# - 권한 테이블 다시 로드: Y

# DB 접속
sudo mysql -u root -p
# 비밀번호 입력

# 데이터베이스 및 사용자 생성
CREATE DATABASE snapfit_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'snapfit_user'@'localhost' IDENTIFIED BY 'SnapFit2024!';
GRANT ALL PRIVILEGES ON snapfit_prod.* TO 'snapfit_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

## 6단계: 애플리케이션 디렉토리 생성 및 파일 업로드

### 6.1 서버에서 디렉토리 생성
```bash
# 애플리케이션 디렉토리 생성
sudo mkdir -p /opt/snapfit-backend
sudo chown ec2-user:ec2-user /opt/snapfit-backend
cd /opt/snapfit-backend
```

### 6.2 로컬에서 JAR 빌드
**로컬 PC에서:**
```bash
cd /Users/devsheep/SnapFit/SnapFit-BackEnd
./gradlew clean build -x test

# 빌드 확인
ls -lh build/libs/*.jar
```

### 6.3 파일 업로드 (로컬 PC에서 실행)
```bash
# JAR 파일 업로드
scp -i ~/Downloads/snapfit-key.pem \
  build/libs/snapfit-backend-0.0.1-SNAPSHOT.jar \
  ec2-user@54.123.45.67:/opt/snapfit-backend/app.jar

# Firebase 서비스 계정 키 파일 업로드 (있는 경우)
scp -i ~/Downloads/snapfit-key.pem \
  ~/Downloads/firebase-service-account.json \
  ec2-user@54.123.45.67:/opt/snapfit-backend/firebase-service-account.json
```

---

## 7단계: 환경 변수 설정

**서버에서:**
```bash
cd /opt/snapfit-backend

# 환경 변수 파일 생성
nano .env
```

**파일 내용 (실제 값으로 변경):**
```bash
# 데이터베이스 설정
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=snapfit_user
DB_PASSWORD=SnapFit2024!

# Firebase 설정
FIREBASE_SERVICE_ACCOUNT_FILE=/opt/snapfit-backend/firebase-service-account.json
FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com

# Spring 프로필
SPRING_PROFILES_ACTIVE=prod

# CORS 설정 (프론트엔드 도메인)
CORS_ALLOWED_ORIGINS=*
```

**저장**: `Ctrl + X` → `Y` → `Enter`

---

## 8단계: Systemd 서비스 등록 (자동 시작/재시작)

```bash
# 서비스 파일 생성
sudo nano /etc/systemd/system/snapfit-backend.service
```

**파일 내용:**
```ini
[Unit]
Description=SnapFit Backend Service
After=network.target mariadb.service

[Service]
Type=simple
User=ec2-user
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

**저장 후 서비스 시작:**
```bash
# 서비스 등록 및 시작
sudo systemctl daemon-reload
sudo systemctl enable snapfit-backend
sudo systemctl start snapfit-backend

# 상태 확인
sudo systemctl status snapfit-backend
```

**로그 확인:**
```bash
sudo journalctl -u snapfit-backend -f
```

---

## 9단계: Nginx 설치 및 설정 (HTTPS + 도메인)

### 9.1 Nginx 설치
```bash
sudo dnf install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 9.2 Nginx 설정
```bash
sudo nano /etc/nginx/conf.d/snapfit-backend.conf
```

**파일 내용:**
```nginx
server {
    listen 80;
    server_name _;  # 도메인이 없으면 _ 사용

    # HTTP를 HTTPS로 리다이렉트 (SSL 설정 후)
    # return 301 https://$server_name$request_uri;

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

**Nginx 재시작:**
```bash
sudo nginx -t  # 설정 확인
sudo systemctl restart nginx
```

---

## 10단계: 테스트

### 10.1 서버에서 테스트
```bash
# 로컬에서 API 호출 테스트
curl http://localhost:8080/api/albums?userId=test
```

### 10.2 외부에서 테스트
**로컬 PC 브라우저에서:**
```
http://54.123.45.67/api/albums?userId=test
```

또는 터미널:
```bash
curl http://54.123.45.67/api/albums?userId=test
```

---

## 11단계: (선택) 도메인 연결 및 SSL

### 11.1 도메인 구매
- 가비아, 후이즈 등에서 도메인 구매 (예: `snapfit.kr`)
- 연간 약 10,000~20,000원

### 11.2 DNS 설정
도메인 관리 페이지에서:
- **A 레코드** 추가
- **호스트**: `api` (또는 `@`)
- **값**: Elastic IP 주소 (예: `54.123.45.67`)
- **TTL**: 3600

### 11.3 SSL 인증서 발급 (Let's Encrypt)
```bash
# Certbot 설치
sudo dnf install certbot python3-certbot-nginx -y

# SSL 인증서 발급 (도메인 필요)
sudo certbot --nginx -d api.snapfit.kr

# 자동 갱신 설정
sudo systemctl enable certbot.timer
```

---

## 12단계: 프론트엔드 설정

**Flutter/React 등에서:**
```dart
// 개발 환경
final baseUrl = 'http://54.123.45.67:8080';

// 운영 환경 (도메인 사용 시)
final baseUrl = 'https://api.snapfit.kr';
```

---

## 🔒 보안 체크리스트

- [ ] SSH 키 파일 안전하게 보관
- [ ] MySQL 비밀번호 강력하게 설정
- [ ] 보안 그룹에서 SSH(22)는 내 IP만 허용
- [ ] 환경 변수 파일(.env) 권한 설정: `chmod 600 .env`
- [ ] Firebase 서비스 계정 키 파일 권한: `chmod 600 firebase-service-account.json`
- [ ] 운영 환경에서는 8080 포트 외부 접근 차단 (Nginx만 사용)

---

## 📊 모니터링

### 로그 확인
```bash
# 애플리케이션 로그
sudo journalctl -u snapfit-backend -f

# Nginx 로그
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# 시스템 리소스
htop
```

### 서비스 재시작
```bash
sudo systemctl restart snapfit-backend
```

---

## 💡 비용 절감 팁

1. **프리티어 활용**: 첫 1년 무료
2. **t3.micro 사용**: 가장 작은 인스턴스로 시작
3. **Elastic IP**: EC2에 연결 시 무료
4. **데이터 전송 최소화**: 이미지는 Firebase Storage 사용 (이미 사용 중)
5. **불필요한 서비스 중지**: 사용하지 않는 서비스는 중지

---

## 🆘 문제 해결

### 서비스가 시작되지 않을 때
```bash
sudo journalctl -u snapfit-backend -n 50
```

### 포트 충돌
```bash
sudo lsof -i :8080
```

### MySQL 연결 실패
```bash
sudo systemctl status mariadb
sudo mysql -u root -p
```

---

## 다음 단계

1. ✅ EC2 인스턴스 생성 완료
2. ✅ 서버 설정 완료
3. ✅ 애플리케이션 배포 완료
4. ✅ 테스트 완료
5. 프론트엔드에서 API 호출 테스트
6. (선택) 도메인 연결 및 SSL 설정
