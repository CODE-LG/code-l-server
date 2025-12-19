-- 기존 체크 제약 조건 제거 및 새로운 제약 조건 추가

-- 기존 체크 제약 조건 확인 및 제거
SET @constraint_exists := (
    SELECT COUNT(*) 
    FROM information_schema.table_constraints 
    WHERE constraint_schema = database() 
    AND table_name = 'recommendation_history'
    AND constraint_name = 'chk_recommendation_type'
);

SET @sqlstmt := IF(@constraint_exists > 0, 
    'ALTER TABLE recommendation_history DROP CONSTRAINT chk_recommendation_type',
    'SELECT ''Constraint does not exist.'' AS msg'
);
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새로운 체크 제약 조건 추가
ALTER TABLE recommendation_history
ADD CONSTRAINT chk_recommendation_type 
CHECK (recommendation_type IN ('DAILY_CODE_MATCHING', 'CODE_TIME'));
