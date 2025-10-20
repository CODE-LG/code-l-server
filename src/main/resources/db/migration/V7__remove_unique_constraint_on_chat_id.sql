-- 채팅방 멤버의 lastReadChat에서 UNIQUE 제약조건 제거
-- 여러 사용자가 동일한 메시지를 마지막으로 읽을 수 있어야 함
ALTER TABLE chat_room_member DROP INDEX UKc3bwd8ohk6yni9mjeryembv4g;
