package codel.block.business

import codel.block.domain.BlockMember
import codel.block.exception.BlockException
import codel.block.infrastructure.BlockMemberJpaRepository
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
    val blockMemberJpaRepository : BlockMemberJpaRepository
) {

    fun blockMember(blocker: Member, blockedMemberId: Long) {
        if(blocker.getIdOrThrow() == blockedMemberId){
            throw BlockException(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다.")
        }
        val blockedMember = memberJpaRepository.findById(blockedMemberId)
            .orElseThrow{ MemberException(HttpStatus.BAD_REQUEST, "차단할 회원을 찾을 수 없습니다.") }


        val blockMember = BlockMember(blockerMember = blocker, blockedMember = blockedMember)
        blockMemberJpaRepository.save(blockMember)
    }
}