package codel.chat.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*

@Entity
class Question(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 500)
    val content: String,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chatRoomQuestions: MutableList<ChatRoomQuestion> = mutableListOf()
) : BaseTimeEntity() {
    
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("질문이 존재하지 않습니다.")
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Question
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
}
