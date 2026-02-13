#!/bin/bash

# SnapFit Redis 설치 스크립트 (Amazon Linux 2 기준)
# 실행: ./install-redis.sh

set -e

echo "🚀 Redis 설치 시작..."

# 1. Redis 패키지 설치
if command -v dnf &> /dev/null; then
    echo "📦 Amazon Linux 2023 (dnf) 감지됨. redis6 설치..."
    sudo dnf install redis6 -y
elif command -v amazon-linux-extras &> /dev/null; then
    echo "📦 Amazon Linux 2: amazon-linux-extras 사용..."
    sudo amazon-linux-extras install redis6 -y
else
    echo "📦 yum으로 redis 설치 시도..."
    sudo yum install redis -y
fi

# 3. 서비스 시작 및 자동 시작 설정
echo "🔄 Redis 서비스 시작..."
SERVICE_NAME="redis"
if [ -f /usr/lib/systemd/system/redis6.service ]; then
    SERVICE_NAME="redis6"
fi

sudo systemctl enable $SERVICE_NAME
sudo systemctl start $SERVICE_NAME

# 4. 상태 확인
echo "📊 Redis 상태 확인..."
sudo systemctl status $SERVICE_NAME --no-pager

echo ""
echo "✅ Redis 설치 완료!"
echo "테스트: redis-cli ping -> PONG"
