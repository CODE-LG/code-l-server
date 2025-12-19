-- V13__create_standard_verification_images_table.sql
-- Create standard_verification_images table for admin-managed pose guide images

CREATE TABLE `standard_verification_images` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `image_url` varchar(1000) NOT NULL COMMENT '표준 이미지 S3 URL',
    `description` varchar(255) NOT NULL COMMENT '포즈 설명 (예: 정면을 보고 양손을 귀 옆에 올려주세요)',
    `is_active` bit(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부 (비활성화된 이미지는 사용자에게 노출 안됨)',
    `created_at` datetime(6) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='관리자가 등록한 표준 인증 이미지 (사용자 인증용 가이드 포즈)';

-- 설명:
-- 표준 이미지는 관리자 페이지에서 등록/수정/삭제
-- 사용자는 활성화된(is_active=1) 표준 이미지 중 랜덤으로 하나를 조회
