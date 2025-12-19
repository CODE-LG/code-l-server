-- V15__remove_verification_image_from_member_status.sql
-- Remove VERIFICATION_IMAGE status from member_status enum

-- MySQL enum 타입에서 VERIFICATION_IMAGE 제거
ALTER TABLE `member`
    MODIFY COLUMN `member_status` ENUM(
        'ADMIN',
        'DONE',
        'ESSENTIAL_COMPLETED',
        'HIDDEN_COMPLETED',
        'PENDING',
        'PERSONALITY_COMPLETED',
        'PHONE_VERIFIED',
        'REJECT',
        'SIGNUP',
        'WITHDRAWN'
        ) DEFAULT NULL;

-- 설명:
-- VERIFICATION_IMAGE 상태를 제거하고 회원가입 플로우를 단순화합니다.
-- 변경된 회원가입 플로우: HIDDEN_COMPLETED → (인증 이미지 제출) → PENDING → DONE
-- 인증 이미지 제출은 HIDDEN_COMPLETED 상태에서 이루어지며, 제출 후 바로 PENDING 상태로 전환됩니다.
