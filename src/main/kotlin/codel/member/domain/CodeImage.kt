package codel.member.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "code_images")
class CodeImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Column(nullable = false, length = 500)
    val url: String,

    @Column(nullable = false)
    var order: Int,

    @Column(nullable = false)
    var isApproved: Boolean = true,

    @Column(length = 1000)
    var rejectionReason: String? = null
) : BaseTimeEntity() {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeImage) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "CodeImage(id=$id, url='$url', order=$order, isApproved=$isApproved)"
    }
}
