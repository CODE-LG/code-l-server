-- 질문추천 사용 여부별 비교 KPI 필드 추가
ALTER TABLE daily_kpi
    ADD COLUMN question_used_avg_message_count DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN question_not_used_avg_message_count DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN question_used_three_turn_rate DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN question_not_used_three_turn_rate DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN question_used_chat_return_rate DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN question_not_used_chat_return_rate DECIMAL(5,2) DEFAULT 0.00 NOT NULL;
