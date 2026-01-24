-- 채팅방 카테고리 기반 질문 추천 기능을 위한 스키마 변경
-- Issue: #389

-- 1. question_group 컬럼 추가
ALTER TABLE question
ADD COLUMN question_group ENUM('A', 'B', 'RANDOM') NOT NULL DEFAULT 'RANDOM'
COMMENT '질문 그룹 (A: 가벼운/진입용, B: 깊이/무게감, RANDOM: 그룹없음)';

-- 2. category ENUM 확장 (채팅방 전용 카테고리 추가)
-- 기존: VALUES, FAVORITE, CURRENT_ME, DATE, MEMORY, WANT_TALK, BALANCE_ONE, IF
-- 추가: TENSION_UP, SECRET
ALTER TABLE question
MODIFY COLUMN category
ENUM(
    'VALUES',
    'FAVORITE',
    'CURRENT_ME',
    'DATE',
    'MEMORY',
    'WANT_TALK',
    'BALANCE_ONE',
    'IF',
    'TENSION_UP',
    'SECRET'
) NOT NULL;

-- 3. 인덱스 추가 (질문 추천 성능 향상)
CREATE INDEX idx_question_category_group_active
ON question(category, question_group, is_active);
