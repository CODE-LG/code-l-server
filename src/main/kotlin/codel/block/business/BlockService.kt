package codel.block.business

import codel.block.domain.BlockMemberRelation
import codel.block.exception.BlockException
import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.member.domain.Member
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
@Transactional
class BlockService(
    val memberJpaRepository : MemberJpaRepository,
    val blockMemberRelationJpaRepository : BlockMemberRelationJpaRepository
) {

    fun blockMember(blocker: Member, blockedMemberId: Long) {
        if(blocker.getIdOrThrow() == blockedMemberId){
            throw BlockException(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다.")
        }

        val blockedMemberIds = blockMemberRelationJpaRepository.findBlockMembersBy(blocker.getIdOrThrow())
            .map { it.blockedMember.id }

        val blockedMember = memberJpaRepository.findById(blockedMemberId)
            .orElseThrow{ MemberException(HttpStatus.BAD_REQUEST, "차단할 회원을 찾을 수 없습니다.")}

        if(blockedMemberIds.contains(blockedMember.getIdOrThrow())){
            throw BlockException(HttpStatus.BAD_REQUEST, "이미 차단한 회원입니다.")
        }
        val blockMemberRelation = BlockMemberRelation(blockerMember = blocker, blockedMember = blockedMember)
        blockMemberRelationJpaRepository.save(blockMemberRelation)
    }

    fun unBlockMember(blocker: Member, blockedMemberId: Long) {
        if(blocker.getIdOrThrow() == blockedMemberId){
            throw BlockException(HttpStatus.BAD_REQUEST, "자기 자신을 차단 해제할 수 없습니다.")
        }

        val findBlockRelation = blockMemberRelationJpaRepository.findByBlockerMemberAndBlockedMember(blocker.getIdOrThrow(), blockedMemberId)
            ?: throw BlockException(HttpStatus.BAD_REQUEST, "차단한 적이 없는 회원입니다.")

        findBlockRelation.unblock()
    }
}