package codel.chat.infrastructure

import codel.chat.infrastructure.entity.ChatRoomEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomJpaRepository : JpaRepository<ChatRoomEntity, Long>
