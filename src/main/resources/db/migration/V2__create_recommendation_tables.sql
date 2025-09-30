-- ===========================================================
-- recommendation_history 테이블 생성
-- ===========================================================
CREATE TABLE IF NOT EXISTS `recommendation_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `recommended_user_id` BIGINT NOT NULL,
    `recommended_at` DATETIME(6) NOT NULL,
    `recommendation_type` ENUM('DAILY_CODE_MATCHING', 'CODE_TIME') NOT NULL,
    `recommendation_time_slot` VARCHAR(50) NULL,
    `created_at` DATETIME(6) DEFAULT NULL,
    `updated_at` DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),

    CONSTRAINT `fk_recommendation_history_user`
        FOREIGN KEY (`user_id`) REFERENCES `member`(`id`),
    CONSTRAINT `fk_recommendation_history_recommended_user`
        FOREIGN KEY (`recommended_user_id`) REFERENCES `member`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 인덱스 추가
CREATE INDEX `idx_user_recommended_at`
    ON `recommendation_history` (`user_id`, `recommended_user_id`, `recommended_at`);

CREATE INDEX `idx_user_recommended_at_only`
    ON `recommendation_history` (`user_id`, `recommended_at`);

CREATE INDEX `idx_recommended_at`
    ON `recommendation_history` (`recommended_at`);