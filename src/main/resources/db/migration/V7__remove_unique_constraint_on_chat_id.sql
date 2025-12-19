SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'chat_room_member'
    AND index_name = 'idx_chat_room_member_chat_id'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_chat_room_member_chat_id ON chat_room_member (chat_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 유니크 인덱스가 있으면 드롭
SET @uk_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'chat_room_member'
    AND index_name = 'UKc3bwd8ohk6yni9mjeryembv4g'
);
SET @sql := IF(@uk_exists > 0,
  'ALTER TABLE chat_room_member DROP INDEX UKc3bwd8ohk6yni9mjeryembv4g',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;