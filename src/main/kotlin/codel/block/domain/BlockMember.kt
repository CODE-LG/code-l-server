package codel.block.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class BlockMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    var blockerMember: Member,
    @ManyToOne
    var blockedMember: Member,

    @Enumerated(EnumType.STRING)
    var status : BlockStatus = BlockStatus.BLOCKED,
) : BaseTimeEntity()
