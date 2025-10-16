-- 추천 시스템 설정 테이블 생성
CREATE TABLE IF NOT EXISTS recommendation_config
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    daily_code_count     INT          NOT NULL DEFAULT 3 COMMENT '오늘의 코드매칭 추천 인원 수',
    code_time_count      INT          NOT NULL DEFAULT 2 COMMENT '코드타임 추천 인원 수',
    code_time_slots      VARCHAR(500) NOT NULL DEFAULT '10:00,22:00' COMMENT '코드타임 시간대 목록',
    daily_refresh_time   VARCHAR(5)   NOT NULL DEFAULT '00:00' COMMENT '오늘의 코드매칭 갱신 시점',
    repeat_avoid_days    INT          NOT NULL DEFAULT 3 COMMENT '중복 방지 기간 (일)',
    allow_duplicate      BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '타입 간 중복 허용 여부',
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) COMMENT '추천 시스템 설정';

-- 기본 설정 데이터 삽입
INSERT INTO recommendation_config
(id, daily_code_count, code_time_count, code_time_slots, daily_refresh_time, repeat_avoid_days, allow_duplicate)
VALUES (1, 3, 2, '10:00,22:00', '00:00', 3, TRUE);
