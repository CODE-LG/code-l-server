package codel.question.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*

@Entity
class Question(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 500)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    val category: QuestionCategory,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = true, length = 1000)
    val description: String? = null  // 질문 설명
) : BaseTimeEntity() {
    
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("질문이 존재하지 않습니다.")
    
    fun isAvailable(): Boolean = isActive
    
    fun isSameCategory(other: Question): Boolean = this.category == other.category
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Question
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
}
