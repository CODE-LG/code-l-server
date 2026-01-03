package codel.chat.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import codel.question.domain.Question
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "chat_room_question",
    uniqueConstraints = [UniqueConstraint(columnNames = ["chat_room_id", "question_id"])]
)
class ChatRoomQuestion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    val chatRoom: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: Question,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_member_id", nullable = true)
    val requestedBy: Member? = null,

    @Column(nullable = false)
    val isUsed: Boolean = false,

    val usedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val isInitial: Boolean = false
) : BaseTimeEntity() {
    
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방 질문이 존재하지 않습니다.")
    
    fun markAsUsed(requestedBy: Member): ChatRoomQuestion {
        return ChatRoomQuestion(
            id = this.id,
            chatRoom = this.chatRoom,
            question = this.question,
            requestedBy = requestedBy,
            isUsed = true,
            usedAt = LocalDateTime.now(),
            isInitial = this.isInitial
        )
    }
    
    companion object {
        /**
         * 일반 질문하기 버튼 클릭 시 (KPI 집계 대상)
         */
        fun create(chatRoom: ChatRoom, question: Question, requestedBy: Member): ChatRoomQuestion {
            return ChatRoomQuestion(
                chatRoom = chatRoom,
                question = question,
                requestedBy = requestedBy,
                isUsed = true,
                usedAt = LocalDateTime.now(),
                isInitial = false
            )
        }

        /**
         * 초기 질문 생성 (시그널 수락 시, KPI 제외 대상)
         */
        fun createInitial(chatRoom: ChatRoom, question: Question, requestedBy: Member): ChatRoomQuestion {
            return ChatRoomQuestion(
                chatRoom = chatRoom,
                question = question,
                requestedBy = requestedBy,
                isUsed = true,
                usedAt = LocalDateTime.now(),
                isInitial = true
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChatRoomQuestion
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
}
