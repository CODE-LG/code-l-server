-- VALUES_CODE 카테고리 추가 (채팅방 전용 가치관 코드)
-- Issue: #397

-- category ENUM에 VALUES_CODE 추가
ALTER TABLE question
MODIFY COLUMN category
ENUM(
    'VALUES',
    'VALUES_CODE',
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
