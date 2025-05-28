package codel.chat.infrastructure

import codel.chat.infrastructure.entity.ChatRoomMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMemberEntity, Long>
