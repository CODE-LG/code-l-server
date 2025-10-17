package codel.report.business

import codel.block.domain.BlockMemberRelation
import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.chat.domain.ChatRoomStatus
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.Member
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.report.domain.Report
import codel.report.exception.ReportException
import codel.report.infrastructure.ReportJpaRepository
import codel.signal.domain.SignalStatus
import codel.signal.infrastructure.SignalJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReportService(
    val reportJpaRepository: ReportJpaRepository,
    val memberJpaRepository: MemberJpaRepository,
    val signalJpaRepository: SignalJpaRepository,
    val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    val blockMemberRelationJpaRepository: BlockMemberRelationJpaRepository
) {
    fun report(reporter: Member,
               reportedId: Long,
               reason: String) {
        if(reporter.getIdOrThrow() == reportedId){
            throw ReportException(HttpStatus.BAD_REQUEST, "자기 자신을 신고할 수 없습니다.")
        }

        val reported = memberJpaRepository.findById(reportedId)
            .orElseThrow{MemberException(HttpStatus.BAD_REQUEST, "신고 대상을 찾을 수 없습니다.")}

        // 1. 신고 내역 저장
        val report = Report(reporter = reporter, reported = reported, reason = reason)
        reportJpaRepository.save(report)

        // 2. 시그널 확인
        val signalFromReporter = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(reporter, reported)
        val signalToReporter = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(reported, reporter)
        
        // 3. 채팅방 확인
        val chatRoomMembers = chatRoomMemberJpaRepository.findCommonChatRoomMembers(
            reporter.getIdOrThrow(), 
            reported.getIdOrThrow()
        )
        
        when {
            // 시그널 전송 + 채팅방 있는 경우 -> 채팅방 DISABLED
            chatRoomMembers.isNotEmpty() -> {
                chatRoomMembers.forEach { chatRoomMember ->
                    if (chatRoomMember.chatRoom.status != ChatRoomStatus.DISABLED) {
                        chatRoomMember.chatRoom.closeConversation()
                    }
                }
            }
            // 시그널 전송 + 채팅방 없는 경우 -> 신고 + 차단 + 시그널 REJECT 상태
            (signalFromReporter != null || signalToReporter != null) -> {
                // 차단 처리 (BlockService 대신 직접 처리)
                val blockMemberRelation = BlockMemberRelation(blockerMember = reporter, blockedMember = reported)
                blockMemberRelationJpaRepository.save(blockMemberRelation)
                
                // 시그널 REJECT 상태로 변경
                signalFromReporter?.let {
                    if (it.senderStatus != SignalStatus.REJECTED) {
                        it.reject()
                    }
                }
                signalToReporter?.let {
                    if (it.receiverStatus != SignalStatus.REJECTED) {
                        it.reject()
                    }
                }
            }
            // 시그널 미전송 + 채팅방 없는 경우 -> 신고 + 차단
            else -> {
                val blockMemberRelation = BlockMemberRelation(blockerMember = reporter, blockedMember = reported)
                blockMemberRelationJpaRepository.save(blockMemberRelation)
            }
        }
    }
}