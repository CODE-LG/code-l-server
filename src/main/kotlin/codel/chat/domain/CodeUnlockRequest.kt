package codel.chat.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "code_unlock_request")
class CodeUnlockRequest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    val chatRoom: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: Member,

    @Enumerated(EnumType.STRING)
    var status: UnlockRequestStatus = UnlockRequestStatus.PENDING,

    val requestedAt: LocalDateTime = LocalDateTime.now(),

    // 2단계에서 사용할 필드들 (미리 준비)
    var processedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    var processedBy: Member? = null
) : BaseTimeEntity() {

    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("CodeUnlockRequest ID가 존재하지 않습니다.")

    fun isPending(): Boolean = status == UnlockRequestStatus.PENDING

    /**
     * 코드해제 요청 승인 (2단계)
     */
    fun approve(processor: Member) {
        if (status != UnlockRequestStatus.PENDING) {
            throw IllegalStateException("대기 중인 요청만 승인할 수 있습니다.")
        }
        
        status = UnlockRequestStatus.APPROVED
        processedAt = LocalDateTime.now()
        processedBy = processor
        
        // ChatRoom의 잠금 해제
        chatRoom.unlock()
    }
    
    /**
     * 코드해제 요청 거절 (2단계)
     */
    fun reject(processor: Member) {
        if (status != UnlockRequestStatus.PENDING) {
            throw IllegalStateException("대기 중인 요청만 거절할 수 있습니다.")
        }
        
        status = UnlockRequestStatus.REJECTED
        processedAt = LocalDateTime.now()
        processedBy = processor
    }
}
