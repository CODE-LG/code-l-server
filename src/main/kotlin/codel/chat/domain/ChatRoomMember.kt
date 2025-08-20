package codel.chat.domain

import codel.chat.exception.ChatException
import codel.member.domain.Member
import jakarta.persistence.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["chat_room_id", "member_id"])])
class ChatRoomMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoom,
    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,
    @OneToOne
    @JoinColumn(name = "chat_id")
    var lastReadChat: Chat? = null,
    
    // 새로 추가된 필드들
    @Enumerated(EnumType.STRING)
    var memberStatus: ChatRoomMemberStatus = ChatRoomMemberStatus.ACTIVE,
    
    var leftAt: LocalDateTime? = null,
) {
    fun leave() {
        this.memberStatus = ChatRoomMemberStatus.LEFT
        this.leftAt = LocalDateTime.now()
    }

    fun block() {
        this.memberStatus = ChatRoomMemberStatus.LEFT
        this.leftAt = LocalDateTime.now()
    }

    fun isActive(): Boolean = memberStatus == ChatRoomMemberStatus.ACTIVE
    
    fun hasLeft(): Boolean = memberStatus == ChatRoomMemberStatus.LEFT

    fun closeConversation() {
        this.chatRoom.closeConversation();
    }
}
