#!/bin/bash
set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔄 무중단 블루-그린 배포 시작${NC}"

# Docker 이미지 이름 (GitHub Actions에서 전달받을 예정)
IMAGE_NAME=${1:-"docker_username/codel-app:latest"}

# Health check 함수
check_health() {
    local port=$1
    # actuator health 먼저 시도, 실패하면 간단한 health 시도
    curl -f -s http://localhost:$port/actuator/health > /dev/null 2>&1 || \
    curl -f -s http://localhost:$port/health > /dev/null 2>&1
}

# 현재 활성 포트 감지
echo -e "${YELLOW}📍 현재 활성 포트 감지 중...${NC}"
if check_health 8080; then
    CURRENT_PORT=8080
    NEW_PORT=8081
elif check_health 8081; then
    CURRENT_PORT=8081
    NEW_PORT=8080
else
    echo -e "${YELLOW}⚠️  기존 서비스가 없습니다. 8080 포트로 시작합니다.${NC}"
    CURRENT_PORT=0
    NEW_PORT=8080
fi

echo -e "${GREEN}✅ 현재 포트: $CURRENT_PORT, 새 포트: $NEW_PORT${NC}"

# 1. 새 컨테이너 시작
echo -e "${BLUE}🚀 새 컨테이너 시작 (포트: $NEW_PORT)${NC}"

# 기존 컨테이너가 있다면 정리
sudo docker stop codel-$NEW_PORT 2>/dev/null || true
sudo docker rm codel-$NEW_PORT 2>/dev/null || true

# 새 컨테이너 시작
sudo docker run -d \
  --name codel-$NEW_PORT \
  -p $NEW_PORT:8080 \
  -v /var/log/app:/var/log/app \
  -e SERVER_PORT=8080 \
  -e JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:-UseContainerSupport" \
  $IMAGE_NAME

# 2. Health Check 대기
echo -e "${YELLOW}⏳ 새 컨테이너 Health Check 대기 중...${NC}"
for i in {1..60}; do
  if check_health $NEW_PORT; then
    echo -e "${GREEN}✅ 새 컨테이너 준비 완료 ($i초 소요)${NC}"
    break
  fi
  if [ $i -eq 60 ]; then
    echo -e "${RED}❌ Health Check 실패: 새 컨테이너가 준비되지 않았습니다${NC}"
    sudo docker logs codel-$NEW_PORT
    exit 1
  fi
  echo -n "."
  sleep 2
done

# 3. Nginx 설정 전환
echo -e "${BLUE}🔄 Nginx 설정 전환 중...${NC}"

# 백업 생성
sudo cp /etc/nginx/conf.d/www.codelg.store.conf /etc/nginx/conf.d/www.codelg.store.conf.backup

# 포트 변경
sudo sed -i "s/server localhost:$CURRENT_PORT/server localhost:$NEW_PORT/g" /etc/nginx/conf.d/www.codelg.store.conf
sudo sed -i "s/server localhost:[0-9]\+/server localhost:$NEW_PORT/g" /etc/nginx/conf.d/www.codelg.store.conf

# 설정 검증
sudo nginx -t
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Nginx 설정 오류: 롤백합니다${NC}"
    sudo cp /etc/nginx/conf.d/www.codelg.store.conf.backup /etc/nginx/conf.d/www.codelg.store.conf
    exit 1
fi

# Nginx 리로드
sudo nginx -s reload
echo -e "${GREEN}✅ Nginx 설정 전환 완료${NC}"

# 4. 외부 접근 테스트
echo -e "${YELLOW}🌐 외부 접근 테스트 중...${NC}"
sleep 3
if curl -f -s https://codelg.store/actuator/health > /dev/null 2>&1 || \
   curl -f -s https://codelg.store/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 외부 접근 정상${NC}"
else
    echo -e "${RED}❌ 외부 접근 실패: 롤백합니다${NC}"
    # Nginx 롤백
    sudo cp /etc/nginx/conf.d/www.codelg.store.conf.backup /etc/nginx/conf.d/www.codelg.store.conf
    sudo nginx -s reload
    exit 1
fi

# 5. 기존 컨테이너 정리 (포트 0이면 건너뛰기)
if [ $CURRENT_PORT -ne 0 ]; then
    echo -e "${YELLOW}🧹 기존 컨테이너 정리 중...${NC}"
    sleep 2  # 안전 여유시간
    sudo docker stop codel-$CURRENT_PORT || true
    sudo docker rm codel-$CURRENT_PORT || true
    echo -e "${GREEN}✅ 기존 컨테이너 정리 완료${NC}"
fi

# 6. 정리 작업
echo -e "${YELLOW}🧹 정리 작업 중...${NC}"
# 사용하지 않는 이미지 정리
sudo docker image prune -f

echo -e "${GREEN}🎉 무중단 배포 완료!${NC}"
echo -e "${GREEN}   활성 포트: $NEW_PORT${NC}"
echo -e "${GREEN}   서비스 URL: https://codelg.store${NC}"
