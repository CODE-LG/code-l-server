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

-- 지역 매핑 테이블
CREATE TABLE IF NOT EXISTS region_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    main_region VARCHAR(20) NOT NULL,
    adjacent_region VARCHAR(20) NOT NULL,
    priority_order INT NOT NULL
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_main_region_priority ON region_mappings (main_region, priority_order);

-- 유니크 제약 추가
ALTER TABLE region_mappings ADD CONSTRAINT IF NOT EXISTS uk_region_mapping UNIQUE (main_region, adjacent_region);
