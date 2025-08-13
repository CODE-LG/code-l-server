package codel.chat.infrastructure

import codel.chat.domain.*
import codel.member.domain.Member
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.LocalDateTime

@DataJpaTest
class ChatJpaRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var chatJpaRepository: ChatJpaRepository

    @Test
    fun `읽지 않은 메시지 수 계산에서 시스템 메시지는 선별적으로 포함된다`() {
        // Given
        val chatRoom = createChatRoom()
        val member = createMember()
        val chatRoomMember = createChatRoomMember(chatRoom, member)
        
        entityManager.persistAndFlush(chatRoom)
        entityManager.persistAndFlush(member)
        entityManager.persistAndFlush(chatRoomMember)

        // 다양한 타입의 메시지 생성
        val userMessage = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = chatRoomMember,
            message = "안녕하세요",
            senderType = ChatSenderType.USER,
            chatContentType = ChatContentType.TEXT,
            sentAt = LocalDateTime.now()
        )

        val codeQuestion = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = chatRoomMember,
            message = "💭 좋아하는 영화는?",
            senderType = ChatSenderType.SYSTEM,
            chatContentType = ChatContentType.CODE_QUESTION,
            sentAt = LocalDateTime.now()
        )

        val unlockRequest = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = chatRoomMember,
            message = "코드해제 요청이 왔습니다",
            senderType = ChatSenderType.SYSTEM,
            chatContentType = ChatContentType.CODE_UNLOCKED_REQUEST,
            sentAt = LocalDateTime.now()
        )

        val matchedMessage = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = null, // 시스템 메시지는 null
            message = "코드 매칭에 성공했어요!",
            senderType = ChatSenderType.SYSTEM,
            chatContentType = ChatContentType.CODE_MATCHED,
            sentAt = LocalDateTime.now()
        )

        val timeMessage = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = null,
            message = "2025-08-13",
            senderType = ChatSenderType.SYSTEM,
            chatContentType = ChatContentType.TIME,
            sentAt = LocalDateTime.now()
        )

        chatJpaRepository.saveAll(listOf(userMessage, codeQuestion, unlockRequest, matchedMessage, timeMessage))
        entityManager.flush()

        // When
        val unreadCount = chatJpaRepository.countByChatRoomAfterLastChat(chatRoom)

        // Then
        // 포함되어야 하는 메시지: userMessage, codeQuestion, unlockRequest (3개)
        // 제외되어야 하는 메시지: matchedMessage, timeMessage (2개)
        assertThat(unreadCount).isEqualTo(3)
    }

    @Test
    fun `특정 시간 이후 읽지 않은 메시지 수 계산`() {
        // Given
        val chatRoom = createChatRoom()
        val member = createMember()
        val chatRoomMember = createChatRoomMember(chatRoom, member)
        
        entityManager.persistAndFlush(chatRoom)
        entityManager.persistAndFlush(member)
        entityManager.persistAndFlush(chatRoomMember)

        val baseTime = LocalDateTime.now().minusMinutes(10)

        // 기준 시간 이전 메시지 (카운트되지 않아야 함)
        val oldMessage = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = chatRoomMember,
            message = "오래된 메시지",
            senderType = ChatSenderType.USER,
            chatContentType = ChatContentType.TEXT,
            sentAt = baseTime.minusMinutes(5)
        )

        // 기준 시간 이후 메시지 (카운트되어야 함)
        val newMessage = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = chatRoomMember,
            message = "새로운 메시지",
            senderType = ChatSenderType.USER,
            chatContentType = ChatContentType.TEXT,
            sentAt = baseTime.plusMinutes(5)
        )

        chatJpaRepository.saveAll(listOf(oldMessage, newMessage))
        entityManager.flush()

        // When
        val unreadCount = chatJpaRepository.countByChatRoomAfterLastChat(chatRoom, baseTime)

        // Then
        assertThat(unreadCount).isEqualTo(1) // newMessage만 카운트
    }

    private fun createChatRoom(): ChatRoom {
        return ChatRoom()
    }

    private fun createMember(): Member {
        // Member 생성 로직 (실제 Member 엔티티에 맞게 조정 필요)
        return Member(
            oauthId = "test123",
            oauthType = OauthType.KAKAO,
            // 기타 필수 필드들...
        )
    }

    private fun createChatRoomMember(chatRoom: ChatRoom, member: Member): ChatRoomMember {
        return ChatRoomMember(
            chatRoom = chatRoom,
            member = member
        )
    }
}
