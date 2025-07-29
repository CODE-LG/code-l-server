package codel.report.business

import codel.block.domain.BlockMemberRelation
import codel.block.exception.BlockException
import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.member.domain.Member
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.report.domain.Report
import codel.report.exception.ReportException
import codel.report.infrastructure.ReportJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ReportService(
    val reportJpaRepository: ReportJpaRepository,
    val memberJpaRepository: MemberJpaRepository,
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

        val report = Report(reporter = reporter, reported = reported, reason = reason)

        val findByBlockerMemberAndBlockedMember =
            blockMemberRelationJpaRepository.findByBlockerMemberAndBlockedMember(reporter.getIdOrThrow(), reportedId)

        if(findByBlockerMemberAndBlockedMember == null){
            val blockMemberRelation = BlockMemberRelation(blockerMember = reporter, blockedMember = reported)
            blockMemberRelationJpaRepository.save(blockMemberRelation)
        }else{
            findByBlockerMemberAndBlockedMember.block()
        }

        reportJpaRepository.save(report)
    }
}