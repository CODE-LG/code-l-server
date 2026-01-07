-- 현재 열려있는 채팅방 수 컬럼 추가
ALTER TABLE daily_kpi
    ADD COLUMN current_open_chatrooms_count INT DEFAULT 0 NOT NULL COMMENT 'endDate 시점 기준 열려있는 채팅방 수 (DISABLED 아닌)';
