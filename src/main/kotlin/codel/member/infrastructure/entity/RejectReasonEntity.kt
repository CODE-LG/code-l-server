package codel.member.infrastructure.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne

@Entity
class RejectReasonEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,
    @OneToOne
    var memberEntity: MemberEntity,
    var reason: String,
) {
    companion object {
        fun toEntity(
            memberEntity: MemberEntity,
            reason: String,
        ): RejectReasonEntity =
            RejectReasonEntity(
                memberEntity = memberEntity,
                reason = reason,
            )
    }
}
