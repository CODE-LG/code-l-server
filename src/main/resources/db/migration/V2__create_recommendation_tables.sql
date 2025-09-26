-- 추천 이력 관리 테이블
CREATE TABLE IF NOT EXISTS recommendation_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommended_user_id BIGINT NOT NULL,
    recommended_date DATE NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    recommendation_time_slot VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 인덱스 생성
    INDEX idx_user_recommended_date (user_id, recommended_user_id, recommended_date),
    INDEX idx_user_date (user_id, recommended_date),
    INDEX idx_recommended_date (recommended_date),

    -- 외래키 제약조건 (MySQL에서는 members 테이블로)
    FOREIGN KEY (user_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (recommended_user_id) REFERENCES members(id) ON DELETE CASCADE
);

-- 테이블 코멘트
ALTER TABLE recommendation_histories COMMENT = '추천 이력 관리 테이블 - 중복 방지 및 추천 결과 추적';
