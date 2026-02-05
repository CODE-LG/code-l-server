package codel.question.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*

@Entity
class Question(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 500)
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    var category: QuestionCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var questionGroup: QuestionGroup = QuestionGroup.RANDOM,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = true, length = 1000)
    var description: String? = null
) : BaseTimeEntity() {
    
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("질문이 존재하지 않습니다.")
    
    fun isAvailable(): Boolean = isActive
    
    fun isSameCategory(other: Question): Boolean = this.category == other.category
    
    fun updateContent(newContent: String) {
        this.content = newContent
    }
    
    fun updateCategory(newCategory: QuestionCategory) {
        this.category = newCategory
    }

    fun updateQuestionGroup(newQuestionGroup: QuestionGroup) {
        this.questionGroup = newQuestionGroup
    }

    fun updateDescription(newDescription: String?) {
        this.description = newDescription
    }
    
    fun toggleActive() {
        this.isActive = !this.isActive
    }
    
    fun updateIsActive(newIsActive: Boolean) {
        this.isActive = newIsActive
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Question
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
}
