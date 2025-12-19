package codel.block.business

import codel.block.domain.BlockMemberRelation
import codel.block.exception.BlockException
import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.chat.business.ChatService
import codel.chat.domain.ChatRoomStatus
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.response.SavedChatDto
import codel.member.domain.Member
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.signal.domain.SignalStatus
import codel.signal.infrastructure.SignalJpaRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
@Transactional
class BlockService(
    val memberJpaRepository: MemberJpaRepository,
    val blockMemberRelationJpaRepository: BlockMemberRelationJpaRepository,
    val signalJpaRepository: SignalJpaRepository,
    val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    val chatService: ChatService
) {

    fun blockMember(blocker: Member, blockedMemberId: Long): SavedChatDto? {
        if (blocker.getIdOrThrow() == blockedMemberId) {
            throw BlockException(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다.")
        }

        val blockedMemberIds = blockMemberRelationJpaRepository.findBlockMembersBy(blocker.getIdOrThrow())
            .map { it.blockedMember.id }

        val blockedMember = memberJpaRepository.findById(blockedMemberId)
            .orElseThrow { MemberException(HttpStatus.BAD_REQUEST, "차단할 회원을 찾을 수 없습니다.") }

        if (blockedMemberIds.contains(blockedMember.getIdOrThrow())) {
            throw BlockException(HttpStatus.BAD_REQUEST, "이미 차단한 회원입니다.")
        }

        // 1. 시그널 확인
        val signalFromBlocker = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(blocker, blockedMember)
        val signalToBlocker = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(blockedMember, blocker)

        // 2. 채팅방 확인
        val chatRoom = chatService.findChatRoomBetweenMembers(blocker, blockedMember)

        // 3. 차단 관계 저장
        val blockMemberRelation = BlockMemberRelation(blockerMember = blocker, blockedMember = blockedMember)
        blockMemberRelationJpaRepository.save(blockMemberRelation)

        return when {
            // 채팅방이 있는 경우 -> 시스템 메시지 + WebSocket 전송
            chatRoom != null -> {
                if (chatRoom.status != ChatRoomStatus.DISABLED) {
                    chatRoom.closeConversation()
                    // 시스템 메시지 생성 및 응답 구성
                    chatService.createCloseConversationMessage(chatRoom, blocker, blockedMember)
                } else {
                    null
                }
            }
            // 시그널 전송 + 채팅방 없는 경우 -> 시그널 REJECT 상태로 처리
            (signalFromBlocker != null || signalToBlocker != null) -> {
                signalFromBlocker?.let {
                    if (it.senderStatus != SignalStatus.REJECTED) {
                        it.reject()
                    }
                }
                signalToBlocker?.let {
                    if (it.receiverStatus != SignalStatus.REJECTED) {
                        it.reject()
                    }
                }
                null
            }
            // 시그널 미전송 + 채팅방 없는 경우 -> 차단만 적용
            else -> null
        }
    }

    fun unBlockMember(blocker: Member, blockedMemberId: Long) {
        if (blocker.getIdOrThrow() == blockedMemberId) {
            throw BlockException(HttpStatus.BAD_REQUEST, "자기 자신을 차단 해제할 수 없습니다.")
        }

        val findBlockRelation = blockMemberRelationJpaRepository.findByBlockerMemberAndBlockedMember(
            blocker.getIdOrThrow(),
            blockedMemberId
        )
            ?: throw BlockException(HttpStatus.BAD_REQUEST, "차단한 적이 없는 회원입니다.")

        findBlockRelation.unblock()
    }
}