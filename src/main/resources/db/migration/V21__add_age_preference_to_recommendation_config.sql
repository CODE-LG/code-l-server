-- 추천 시스템 나이 우선순위 설정 필드 추가

ALTER TABLE recommendation_config
    ADD COLUMN age_preferred_max_diff INT NOT NULL DEFAULT 5
        COMMENT '우선 추천 최대 나이 차이 (0~N살, 기본: 5)' AFTER allow_duplicate,
    ADD COLUMN age_cutoff_diff INT NOT NULL DEFAULT 6
        COMMENT '컷오프 기준 나이 차이 (N살 이상 제외, 기본: 6)' AFTER age_preferred_max_diff,
    ADD COLUMN age_allow_cutoff_when_insufficient BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT '후보 부족 시 컷오프 대상 허용 여부 (기본: true)' AFTER age_cutoff_diff;

-- 기존 레코드 업데이트 (이미 DEFAULT 값이 설정되므로 필요 없지만 명시적으로)
UPDATE recommendation_config
SET age_preferred_max_diff = 5,
    age_cutoff_diff = 6,
    age_allow_cutoff_when_insufficient = TRUE
WHERE id = 1;
