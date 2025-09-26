-- recommendation_time_slot 컬럼이 이미 존재하므로 인덱스 부분만 처리

-- 기존에 잘못된 인덱스가 있을 수 있으니 안전하게 처리
-- MySQL에서는 DROP IF EXISTS를 지원하지 않으므로, 조건부로 처리
SET @exist := (SELECT count(*) FROM information_schema.statistics WHERE table_schema=database() AND table_name='recommendation_history' AND index_name='idx_user_recommended_date');
SET @sqlstmt := IF(@exist > 0, 'DROP INDEX idx_user_recommended_date ON recommendation_history', 'SELECT ''Index does not exist.'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT count(*) FROM information_schema.statistics WHERE table_schema=database() AND table_name='recommendation_history' AND index_name='idx_user_date');
SET @sqlstmt := IF(@exist > 0, 'DROP INDEX idx_user_date ON recommendation_history', 'SELECT ''Index does not exist.'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT count(*) FROM information_schema.statistics WHERE table_schema=database() AND table_name='recommendation_history' AND index_name='idx_recommended_date');
SET @sqlstmt := IF(@exist > 0, 'DROP INDEX idx_recommended_date ON recommendation_history', 'SELECT ''Index does not exist.'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새 인덱스 생성 (이미 존재할 수 있으니 조건부로 처리)
SET @exist := (SELECT count(*) FROM information_schema.statistics WHERE table_schema=database() AND table_name='recommendation_history' AND index_name='idx_user_recommended_at');
SET @sqlstmt := IF(@exist = 0, 'CREATE INDEX idx_user_recommended_at ON recommendation_history (user_id, recommended_user_id, recommended_at)', 'SELECT ''Index already exists.'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT count(*) FROM information_schema.statistics WHERE table_schema=database() AND table_name='recommendation_history' AND index_name='idx_user_recommended_at_only');
SET @sqlstmt := IF(@exist = 0, 'CREATE INDEX idx_user_recommended_at_only ON recommendation_history (user_id, recommended_at)', 'SELECT ''Index already exists.'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT count(*) FROM information_schema.statistics WHERE table_schema=database() AND table_name='recommendation_history' AND index_name='idx_recommended_at');
SET @sqlstmt := IF(@exist = 0, 'CREATE INDEX idx_recommended_at ON recommendation_history (recommended_at)', 'SELECT ''Index already exists.'' AS msg');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
