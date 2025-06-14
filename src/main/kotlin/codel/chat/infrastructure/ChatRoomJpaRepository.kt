package codel.chat.infrastructure

import codel.chat.domain.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomJpaRepository : JpaRepository<ChatRoom, Long>
