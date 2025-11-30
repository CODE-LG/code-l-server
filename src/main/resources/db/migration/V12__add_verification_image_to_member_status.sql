-- V12__add_verification_image_to_member_status.sql
-- Add VERIFICATION_IMAGE status to member_status enum

-- MySQL enum 타입에 새로운 값 추가
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
        'VERIFICATION_IMAGE',  -- 신규 추가
        'WITHDRAWN'
        ) DEFAULT NULL;

-- 설명:
-- VERIFICATION_IMAGE 상태는 HIDDEN_COMPLETED와 PENDING 사이의 단계입니다.
-- 회원이 히든 프로필(얼굴 이미지)을 완료한 후, 인증 이미지를 제출하는 단계를 의미합니다.
-- 회원가입 플로우: HIDDEN_COMPLETED → VERIFICATION_IMAGE → PENDING → DONE
