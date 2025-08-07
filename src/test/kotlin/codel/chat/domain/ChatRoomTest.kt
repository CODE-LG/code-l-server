package codel.chat.domain

import codel.chat.exception.ChatException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class ChatRoomTest {
    @DisplayName("LOCKED 상태에서 unlock을 하면 LOCKED_REQUESTED로 전이된다")
    @Test
    fun unlock_from_locked() {
        // given
        val chatRoom = ChatRoom(
            id = 1L,
            status = ChatRoomStatus.LOCKED
        )
        val memberId = 100L

        // when
        chatRoom.unlock(memberId)

        // then
        assertThat(chatRoom.status).isEqualTo(ChatRoomStatus.UNLOCKED_REQUESTED)
        assertThat(chatRoom.unlockedRequestedBy).isEqualTo(memberId)
        assertThat(chatRoom.unlockedUpdateAt).isNotNull()
    }

    @DisplayName("LOCKED_REQUESTED 상태에서 unlock을 다른 사용자가 하면 UNLOCKED로 전이된다")
    @Test
    fun unlock_from_lockedRequested_by_other() {
        // given
        val chatRoom = ChatRoom(
            id = 1L,
            status = ChatRoomStatus.UNLOCKED_REQUESTED,
            unlockedRequestedBy = 100L,
            unlockedUpdateAt = LocalDateTime.now().minusMinutes(1)
        )
        val otherMemberId = 200L

        // when
        chatRoom.unlock(otherMemberId)

        // then
        assertThat(chatRoom.status).isEqualTo(ChatRoomStatus.UNLOCKED)
        assertThat(chatRoom.unlockedUpdateAt).isNotNull()
    }

    @DisplayName("LOCKED_REQUESTED 상태에서 unlock을 같은 사용자가 하면 예외가 발생한다")
    @Test
    fun unlock_from_lockedRequested_by_sameUser() {
        // given
        val memberId = 100L
        val chatRoom = ChatRoom(
            id = 1L,
            status = ChatRoomStatus.UNLOCKED_REQUESTED,
            unlockedRequestedBy = memberId,
            unlockedUpdateAt = LocalDateTime.now().minusMinutes(1)
        )

        // when & then
        val exception = assertThrows(ChatException::class.java) {
            chatRoom.unlock(memberId)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("이미 코드해제 요청을 보낸 상태입니다.")
    }

    @DisplayName("UNLOCKED 상태에서 unlock을 하면 예외가 발생한다")
    @Test
    fun unlock_from_unlocked() {
        // given
        val chatRoom = ChatRoom(
            id = 1L,
            status = ChatRoomStatus.UNLOCKED,
            unlockedRequestedBy = 100L,
            unlockedUpdateAt = LocalDateTime.now().minusMinutes(1)
        )
        val memberId = 200L

        // when & then
        val exception = assertThrows(ChatException::class.java) {
            chatRoom.unlock(memberId)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("이미 코드해제된 방입니다.")
    }
} 