package codel.member.domain

import jakarta.persistence.*

@Entity
class RejectReason(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @OneToOne
    var member: Member,
    var reason: String,
)
