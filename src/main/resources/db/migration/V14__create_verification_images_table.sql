-- V14__create_verification_images_table.sql
-- Create verification_images table for user-submitted verification images

CREATE TABLE `verification_images` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `member_id` bigint NOT NULL,
    `standard_verification_image_id` bigint NOT NULL,
    `user_image_url` varchar(1000) NOT NULL COMMENT '사용자가 촬영한 인증 이미지 S3 URL (/verification_images/)',
    `created_at` datetime(6) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    `deleted_at` datetime(6) DEFAULT NULL COMMENT '소프트 딜리트 (회원 탈퇴 시)',
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_standard_image_id` (`standard_verification_image_id`),
    KEY `idx_deleted_at` (`deleted_at`),
    CONSTRAINT `fk_verification_image_member`
        FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
    CONSTRAINT `fk_verification_image_standard`
        FOREIGN KEY (`standard_verification_image_id`)
        REFERENCES `standard_verification_images` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='사용자가 제출한 인증 이미지 (재제출 가능, 이력 관리)';

-- 설명:
-- 재제출 가능하므로 UNIQUE 제약 없음
-- 한 회원이 여러 번 제출 가능 (이력 관리)
-- 최신 이미지: ORDER BY created_at DESC LIMIT 1
-- 소프트 딜리트: deleted_at NOT NULL인 레코드는 삭제된 것으로 간주
