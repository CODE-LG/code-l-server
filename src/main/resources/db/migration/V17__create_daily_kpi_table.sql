-- daily_kpi 테이블 생성
-- 일별 KPI 집계 데이터 저장

CREATE TABLE daily_kpi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 날짜 (한국 시간 기준 집계 날짜)
    target_date DATE NOT NULL UNIQUE COMMENT '한국 시간 기준 집계 날짜',

    -- 1. 시그널 KPI
    signal_sent_count INT DEFAULT 0 COMMENT '시그널 보낸 수',
    signal_accepted_count INT DEFAULT 0 COMMENT '시그널 수락 수',

    -- 2. 채팅 KPI
    open_chatrooms_count INT DEFAULT 0 COMMENT '열린 채팅방 수',
    active_chatrooms_count INT DEFAULT 0 COMMENT '활성 채팅방 수 (7일 내 활동)',
    first_message_rate DECIMAL(5,2) DEFAULT 0 COMMENT '첫 메시지 전송률 (%)',
    three_turn_rate DECIMAL(5,2) DEFAULT 0 COMMENT '3턴 이상 대화 비율 (%)',
    chat_return_rate DECIMAL(5,2) DEFAULT 0 COMMENT '24h 재방문률 (%)',
    avg_message_count DECIMAL(6,2) DEFAULT 0 COMMENT '평균 메시지 수',

    -- 3. 질문추천 KPI
    question_click_count INT DEFAULT 0 COMMENT '질문추천 버튼 클릭 수',
    question_used_chatrooms_count INT DEFAULT 0 COMMENT '질문추천 사용 채팅방 수',

    -- 4. 코드해제 KPI
    code_unlock_request_count INT DEFAULT 0 COMMENT '코드해제 요청 수',
    code_unlock_approved_count INT DEFAULT 0 COMMENT '코드해제 승인 수',

    -- 5. 종료된 채팅방 KPI
    closed_chatrooms_count INT DEFAULT 0 COMMENT '종료된 채팅방 수',
    avg_chat_duration_days DECIMAL(6,2) DEFAULT 0 COMMENT '평균 채팅 유지 기간 (일)',

    -- 메타 정보 (UTC로 저장)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_target_date (target_date DESC)
) COMMENT='일별 KPI 집계 테이블 (한국 시간 기준 날짜)';
