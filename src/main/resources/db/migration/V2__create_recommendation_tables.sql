-- 추천 이력 관리 테이블
CREATE TABLE IF NOT EXISTS recommendation_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommended_user_id BIGINT NOT NULL,
    recommended_date DATE NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    recommendation_time_slot VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_user_recommended_date ON recommendation_histories (user_id, recommended_user_id, recommended_date);
CREATE INDEX IF NOT EXISTS idx_user_date ON recommendation_histories (user_id, recommended_date);
CREATE INDEX IF NOT EXISTS idx_recommended_date ON recommendation_histories (recommended_date);

-- 테이블 코멘트
COMMENT ON TABLE recommendation_histories IS '추천 이력 관리 테이블 - 중복 방지 및 추천 결과 추적';
