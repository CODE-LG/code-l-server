package codel.member.domain

import codel.member.exception.MemberException
import jakarta.persistence.*
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

@Entity
@Table(name = "profiles")
class Profile(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    // ===== 기본 프로필 섹션 (Essential Profile) =====
    var codeName: String? = null,
    
    @Column(nullable = true)
    var birthDate: LocalDate? = null,  // 생년월일로 변경
    
    var sido: String? = null,
    var sigugun: String? = null,
    var jobCategory: String? = null,
    var interests: String? = null,
    
    @Column(length = 1000)
    var codeImage: String? = null,
    
    @Column(nullable = false)
    var essentialCompleted: Boolean = false,
    
    @Column
    var essentialCompletedAt: LocalDateTime? = null,
    
    // ===== 성격/취향 프로필 섹션 (Personality Profile) =====
    var hairLength: String? = null,
    var bodyType: String? = null,
    var height: Int? = null,
    var styles: String? = null,
    var mbti: String? = null,
    var drinkingStyle: String? = null,
    var smokingStyle: String? = null,
    var personalities: String? = null,
    var question: String? = null,
    var answer: String? = null,
    
    @Column(nullable = false)
    var personalityCompleted: Boolean = false,
    
    @Column
    var personalityCompletedAt: LocalDateTime? = null,
    
    // ===== 히든 프로필 섹션 (Hidden Profile) =====
    var loveLanguage: String? = null,
    var affectionStyle: String? = null,
    var contactStyle: String? = null,
    var dateStyle: String? = null,
    var conflictResolutionStyle: String? = null,
    var relationshipValues: String? = null,
    
    @Column(length = 1000)
    var faceImage: String? = null,
    
    @Column(nullable = false)
    var hiddenCompleted: Boolean = false,
    
    @Column
    var hiddenCompletedAt: LocalDateTime? = null,
    
    // ===== 기타 =====
    var introduce: String? = null,     // 자기소개 (선택사항)
    
    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "member_id")
    var member: Member? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    companion object {
        // List ↔ String 변환 유틸리티
        private fun serializeList(list: List<String>): String = 
            list.filter { it.isNotBlank() }.joinToString(",")
        
        private fun deserializeString(str: String?): List<String> = 
            str?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    
    // ===== 나이 계산 =====
    fun getAge(): Int {
        return birthDate?.let { 
            Period.between(it, LocalDate.now()).years 
        } ?: throw IllegalStateException("생년월일이 설정되지 않았습니다.")
    }
    
    // ===== 자기 관리 메서드들 =====
    fun updateEssentialProfile(
        codeName: String,
        birthDate: LocalDate,
        sido: String,
        sigugun: String,
        jobCategory: String,
        codeImages: List<String>,
        interests: List<String>
    ) {
        validateEssentialDomainRules(birthDate, codeImages, interests)
        
        this.codeName = codeName
        this.birthDate = birthDate
        this.sido = sido
        this.sigugun = sigugun
        this.jobCategory = jobCategory
        this.codeImage = serializeList(codeImages)
        this.interests = serializeList(interests)
        
        this.essentialCompleted = true
        this.essentialCompletedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
    
    fun updatePersonalityProfile(
        hairLength: String?,
        bodyType: String?,
        height: Int?,
        styles: List<String>,
        mbti: String?,
        drinkingStyle: String?,
        smokingStyle: String?,
        personalities: List<String>,
        question: String?,
        answer: String?
    ) {
        validatePersonalityDomainRules(personalities, height, mbti)
        
        this.hairLength = hairLength
        this.bodyType = bodyType
        this.height = height
        this.styles = serializeList(styles)
        this.mbti = mbti
        this.drinkingStyle = drinkingStyle
        this.smokingStyle = smokingStyle
        this.personalities = serializeList(personalities)
        this.question = question
        this.answer = answer
        
        this.personalityCompleted = true
        this.personalityCompletedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
    
    fun updateHiddenProfile(
        loveLanguage: String,
        affectionStyle: String,
        contactStyle: String,
        dateStyle: String,
        conflictResolutionStyle: String,
        relationshipValues: String,
        faceImages: List<String>
    ) {
        validateHiddenDomainRules(faceImages)
        
        this.loveLanguage = loveLanguage
        this.affectionStyle = affectionStyle
        this.contactStyle = contactStyle
        this.dateStyle = dateStyle
        this.conflictResolutionStyle = conflictResolutionStyle
        this.relationshipValues = relationshipValues
        this.faceImage = serializeList(faceImages)
        
        this.hiddenCompleted = true
        this.hiddenCompletedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
    
    // ===== 상태 조회 =====
    fun isPublicProfileComplete(): Boolean = essentialCompleted && personalityCompleted
    fun isFullProfileComplete(): Boolean = essentialCompleted && personalityCompleted && hiddenCompleted
    
    fun getNextRequiredStep(): String? {
        return when {
            !essentialCompleted -> "ESSENTIAL"
            !personalityCompleted -> "PERSONALITY"
            !hiddenCompleted -> "HIDDEN"
            else -> null
        }
    }
    
    // ===== 리스트 접근 메서드들 =====
    fun getInterestsList(): List<String> = deserializeString(interests)
    fun getPersonalitiesList(): List<String> = deserializeString(personalities)
    fun getStylesList(): List<String> = deserializeString(styles)
    fun getCodeImageList(): List<String> = deserializeString(codeImage)
    fun getFaceImageList(): List<String> = deserializeString(faceImage)
    
    // 기존 호환성 메서드 유지
    fun getCodeImageOrThrow(): List<String> {
        val images = getCodeImageList()
        if (images.isEmpty()) {
            throw MemberException(HttpStatus.BAD_REQUEST, "코드 이미지가 존재하지 않습니다.")
        }
        return images
    }
    
    fun getFaceImageOrThrow(): List<String> {
        val images = getFaceImageList()
        if (images.isEmpty()) {
            throw MemberException(HttpStatus.BAD_REQUEST, "얼굴 이미지가 존재하지 않습니다.")
        }
        return images
    }
    
    // ===== 도메인 검증 (비즈니스 규칙만) =====
    private fun validateEssentialDomainRules(
        birthDate: LocalDate,
        codeImages: List<String>,
        interests: List<String>
    ) {
        require(!birthDate.isAfter(LocalDate.now())) {
            "생년월일은 미래 날짜일 수 없습니다"
        }
        require(codeImages.isNotEmpty()) { "코드 이미지가 필요합니다" }
        require(interests.isNotEmpty()) { "관심사가 필요합니다" }
    }
    
    private fun validatePersonalityDomainRules(
        personalities: List<String>,
        height: Int?,
        mbti: String?
    ) {
        require(essentialCompleted) { "기본 프로필을 먼저 완성해야 합니다" }
        require(personalities.isNotEmpty()) { "성격을 최소 1개는 선택해야 합니다" }
        
        height?.let {
            require(it in 120..220) { "키는 120-220cm 사이여야 합니다" }
        }
        
        mbti?.let {
            require(it.length == 4 && it.all { char -> char.isLetter() }) {
                "MBTI는 4자리 영문이어야 합니다"
            }
        }
    }
    
    private fun validateHiddenDomainRules(faceImages: List<String>) {
        require(isPublicProfileComplete()) { "공개 프로필을 먼저 완성해야 합니다" }
        require(faceImages.isNotEmpty()) { "얼굴 이미지가 필요합니다" }
    }
}
