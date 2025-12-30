-- chat_room_question 테이블에 is_initial 컬럼 추가
-- 초기 질문(시그널 수락 시 자동 생성)과 버튼 클릭 질문을 구분하기 위함

-- 1. is_initial 컬럼 추가
ALTER TABLE chat_room_question
ADD COLUMN is_initial BOOLEAN NOT NULL DEFAULT FALSE
COMMENT '초기 질문 여부 (시그널 수락 시 자동 생성)';

-- 2. 기존 데이터 마이그레이션
-- 채팅방 생성 후 1분 이내에 생성된 질문은 초기 질문으로 간주
UPDATE chat_room_question crq
JOIN chat_room cr ON crq.chat_room_id = cr.id
SET crq.is_initial = TRUE
WHERE TIMESTAMPDIFF(SECOND, cr.created_at, crq.created_at) <= 60;

-- 3. 인덱스 추가 (KPI 집계 성능 향상)
CREATE INDEX idx_is_initial_created_at
ON chat_room_question(is_initial, created_at);
