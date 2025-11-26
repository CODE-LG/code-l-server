-- ================================================
-- V11: 거절 이력 관리 시스템
-- ================================================
-- 작성일: 2025-01-15
-- 설명: 프로필 심사 거절 이력을 차수별로 관리하기 위한 테이블 생성
--       S3 이미지 URL을 보존하여 과거 거절 내역 조회 가능
-- ================================================

-- 1. rejection_histories 테이블 생성
CREATE TABLE rejection_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거절 이력 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    rejection_round INT NOT NULL COMMENT '거절 차수 (1, 2, 3...)',
    image_type VARCHAR(50) NOT NULL COMMENT '거절된 이미지 타입 (FACE_IMAGE, CODE_IMAGE)',
    image_id BIGINT NOT NULL COMMENT '거절된 이미지의 실제 ID',
    image_url VARCHAR(500) NOT NULL COMMENT '거절 당시 이미지 URL (S3에 보존)',
    image_order INT NOT NULL COMMENT '이미지 순서 (1부터 시작)',
    reason VARCHAR(1000) NOT NULL COMMENT '거절 사유',
    rejected_at DATETIME NOT NULL COMMENT '거절 시각',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',

    -- 외래키
    CONSTRAINT fk_rejection_history_member
        FOREIGN KEY (member_id) REFERENCES member(id)
        ON DELETE CASCADE,

    -- 인덱스
    INDEX idx_member_id (member_id),
    INDEX idx_member_rejection_round (member_id, rejection_round),
    INDEX idx_created_at (created_at),
    INDEX idx_rejected_at (rejected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='프로필 심사 거절 이력';

-- ================================================
-- 설명:
-- 
-- 이 테이블은 회원의 프로필 거절 이력을 관리합니다.
-- 
-- 주요 특징:
-- 1. 거절 차수(rejection_round)로 여러 번의 거절을 구분
-- 2. S3 이미지 URL을 보존하여 과거 이미지 확인 가능
-- 3. 이미지 타입(얼굴/코드), 순서, 거절 사유 모두 기록
-- 4. 회원 삭제 시 이력도 함께 삭제 (CASCADE)
-- 
-- 사용 예시:
-- - 회원 A가 1차 거절: rejection_round = 1
-- - 이미지 재제출 후 2차 거절: rejection_round = 2
-- - 관리자가 과거 거절 이력 조회 시 모든 차수의 이력 확인 가능
-- ================================================
