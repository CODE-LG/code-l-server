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
  -e TZ=UTC \
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

# 포트 변경 (Spring Boot upstream만 변경, Next.js는 그대로 유지)
# upstream backend 블록 내의 8080/8081 포트만 변경
if [ $CURRENT_PORT -ne 0 ]; then
    # 기존 포트가 있는 경우: CURRENT_PORT -> NEW_PORT
    sudo sed -i "/upstream backend/,/}/s/server localhost:$CURRENT_PORT/server localhost:$NEW_PORT/g" /etc/nginx/conf.d/www.codelg.store.conf
else
    # 기존 포트가 없는 경우: 8080 또는 8081 중 하나를 NEW_PORT로
    sudo sed -i "/upstream backend/,/}/s/server localhost:808[01]/server localhost:$NEW_PORT/g" /etc/nginx/conf.d/www.codelg.store.conf
fi

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
# 4. 외부 접근 테스트 (상세 버전)
echo -e "${YELLOW}🌐 외부 접근 테스트 중...${NC}"

# 단계별 테스트
echo -e "${BLUE}1️⃣ 로컬 포트 직접 확인${NC}"
LOCAL_TEST=$(curl -f -s -m 5 http://localhost:$NEW_PORT/actuator/health 2>/dev/null || echo "FAILED")
if [ "$LOCAL_TEST" = "FAILED" ]; then
    echo -e "${RED}❌ 로컬 포트 $NEW_PORT 응답 없음${NC}"
    echo "컨테이너 상태:"
    sudo docker ps | grep codel-$NEW_PORT
    echo "컨테이너 로그:"
    sudo docker logs --tail 10 codel-$NEW_PORT
    exit 1
else
    echo -e "${GREEN}✅ 로컬 포트 $NEW_PORT 응답 정상${NC}"
fi

echo -e "${BLUE}2️⃣ Nginx upstream 설정 확인${NC}"
UPSTREAM_PORT=$(sudo grep -A 3 "upstream backend" /etc/nginx/conf.d/www.codelg.store.conf | grep "server localhost" | grep -o "[0-9]\+")
echo -e "${BLUE}   현재 upstream 포트: $UPSTREAM_PORT${NC}"
if [ "$UPSTREAM_PORT" != "$NEW_PORT" ]; then
    echo -e "${RED}❌ Upstream 포트가 새 포트와 다릅니다 ($UPSTREAM_PORT ≠ $NEW_PORT)${NC}"
    exit 1
fi

echo -e "${BLUE}3️⃣ Nginx 프로세스 상태 확인${NC}"
if ! sudo systemctl is-active --quiet nginx; then
    echo -e "${RED}❌ Nginx 서비스가 실행되지 않고 있습니다${NC}"
    sudo systemctl status nginx --no-pager
    exit 1
else
    echo -e "${GREEN}✅ Nginx 서비스 정상${NC}"
fi

echo -e "${BLUE}4️⃣ 외부 HTTPS 접근 테스트 (타임아웃 10초)${NC}"
sleep 3

# 더 짧은 타임아웃으로 빠른 실패
EXTERNAL_TEST=$(curl -f -s -m 10 --connect-timeout 5 https://codelg.store/actuator/health 2>&1)
CURL_EXIT_CODE=$?

if [ $CURL_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ 외부 접근 정상${NC}"
    echo "응답: $EXTERNAL_TEST"
else
    echo -e "${RED}❌ 외부 접근 실패 (종료코드: $CURL_EXIT_CODE)${NC}"
    
    # curl 에러 코드별 메시지
    case $CURL_EXIT_CODE in
        6)  echo "DNS 해석 실패" ;;
        7)  echo "서버 연결 실패" ;;
        28) echo "타임아웃" ;;
        22) echo "HTTP 에러 응답" ;;
        *)  echo "기타 curl 오류" ;;
    esac
    
    echo "상세 응답: $EXTERNAL_TEST"
    
    echo -e "${RED}=== 디버깅 정보 ===${NC}"
    echo "Nginx 에러 로그 (최근 5줄):"
    sudo tail -5 /var/log/nginx/error.log
    
    echo "Nginx 액세스 로그 (최근 3줄):"
    sudo tail -3 /var/log/nginx/access.log
    
    echo "포트 상태:"
    sudo netstat -tlnp | grep -E ":(443|80|808[01])"
    
    # 롤백
    echo -e "${RED}롤백을 진행합니다${NC}"
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
