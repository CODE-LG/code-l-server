-- V8: 신고 테이블에 처리 상태 및 관리자 메모 필드 추가

-- 1. status 컬럼 추가 (ENUM 타입)
ALTER TABLE report
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
COMMENT '신고 처리 상태: PENDING(미처리), IN_PROGRESS(검토중), RESOLVED(처리완료), DISMISSED(반려), DUPLICATE(중복신고)';

-- 2. admin_note 컬럼 추가 (관리자 메모)
ALTER TABLE report
ADD COLUMN admin_note TEXT NULL
COMMENT '관리자가 작성한 처리 메모';

-- 3. processed_at 컬럼 추가 (처리 완료 일시)
ALTER TABLE report
ADD COLUMN processed_at DATETIME NULL
COMMENT '신고 처리 완료 일시';

-- 4. reason 컬럼 타입 변경 (VARCHAR → TEXT)
ALTER TABLE report
MODIFY COLUMN reason TEXT NOT NULL
COMMENT '신고 사유';

-- 5. status 컬럼에 인덱스 추가 (필터링 성능 향상)
CREATE INDEX idx_report_status ON report(status);

-- 6. created_at 컬럼에 인덱스 추가 (날짜 범위 검색 성능 향상)
CREATE INDEX idx_report_created_at ON report(created_at);

-- 7. reported_id와 status 복합 인덱스 (피신고자별 상태 조회 최적화)
CREATE INDEX idx_report_reported_status ON report(reported_id, status);

-- 8. status 값 체크 제약 조건 추가
ALTER TABLE report
ADD CONSTRAINT chk_report_status
CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED', 'DUPLICATE'));