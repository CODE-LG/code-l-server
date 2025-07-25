package codel.block.domain

import codel.block.exception.BlockException
import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.springframework.http.HttpStatus

@Entity
class BlockMemberRelation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    var blockerMember: Member,
    @ManyToOne
    var blockedMember: Member,

    @Enumerated(EnumType.STRING)
    var status : BlockStatus = BlockStatus.BLOCKED,
) : BaseTimeEntity() {

    fun unblock() {
        if(status == BlockStatus.UNBLOCKED){
            throw BlockException(HttpStatus.BAD_REQUEST, "이미 차단해제된 회원입니다.")
        }
        status = BlockStatus.UNBLOCKED
    }
}
