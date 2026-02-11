#!/bin/bash

# SnapFit Backend 빠른 배포 스크립트
# 사용법: ./quick-deploy.sh <서버IP> <키파일경로>

set -e

SERVER_IP=$1
KEY_FILE=$2
JAR_FILE="build/libs/SnapFit-BackEnd-0.0.1-SNAPSHOT.jar"

if [ -z "$SERVER_IP" ] || [ -z "$KEY_FILE" ]; then
    echo "사용법: ./quick-deploy.sh <서버IP> <키파일경로>"
    echo "예시: ./quick-deploy.sh 54.123.45.67 ~/Downloads/snapfit-key.pem"
    exit 1
fi

if [ ! -f "$KEY_FILE" ]; then
    echo "오류: 키 파일을 찾을 수 없습니다: $KEY_FILE"
    exit 1
fi

echo "🚀 SnapFit Backend 배포 시작..."
echo "서버 IP: $SERVER_IP"
echo "키 파일: $KEY_FILE"

# 1. 빌드
echo ""
echo "📦 JAR 파일 빌드 중..."
./gradlew clean build -x test

if [ ! -f "$JAR_FILE" ]; then
    echo "오류: JAR 파일을 찾을 수 없습니다: $JAR_FILE"
    exit 1
fi

# 2. 서버로 파일 업로드
echo ""
echo "📤 서버로 파일 업로드 중..."
scp -i "$KEY_FILE" \
    "$JAR_FILE" \
    ec2-user@"$SERVER_IP":/opt/snapfit-backend/app.jar

# 3. 서버에서 서비스 재시작
echo ""
echo "🔄 서버에서 서비스 재시작 중..."
ssh -i "$KEY_FILE" ec2-user@"$SERVER_IP" << 'EOF'
    sudo systemctl restart snapfit-backend
    sleep 3
    sudo systemctl status snapfit-backend --no-pager
EOF

echo ""
echo "✅ 배포 완료!"
echo "서버 주소: http://$SERVER_IP"
echo "API 테스트: curl http://$SERVER_IP/api/albums?userId=test"
