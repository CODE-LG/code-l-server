package codel.chat.infrastructure

import codel.chat.domain.LastReadChat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LastReadChatJpaRepository : JpaRepository<LastReadChat, Long>
