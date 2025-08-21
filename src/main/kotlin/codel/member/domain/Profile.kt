package codel.member.domain

import codel.common.domain.BaseTimeEntity
import codel.member.exception.MemberException
import codel.question.domain.Question
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

    var bigCity: String? = null,
    var smallCity: String? = null,
    var job: String? = null,
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
    var style: String? = null,
    var mbti: String? = null,
    var alcohol: String? = null,
    var smoke: String? = null,
    var personalities: String? = null,
    
    // 대표 질문 (Question 엔티티와 1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_question_id")
    var representativeQuestion: Question? = null,

    var representativeAnswer: String? = null,

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
) : BaseTimeEntity(){
    
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
    fun updateEssentialProfileInfo(
        codeName: String,
        birthDate: LocalDate,
        sido: String,
        sigugun: String,
        jobCategory: String,
        interests: List<String>
    ) {
        validateEssentialInfoDomainRules(birthDate, interests)
        
        this.codeName = codeName
        this.birthDate = birthDate
        this.bigCity = sido
        this.smallCity = sigugun
        this.job = jobCategory
        this.interests = serializeList(interests)
        
        this.updatedAt = LocalDateTime.now()
    }
    
    fun updateEssentialProfileImages(
        codeImages: List<String>
    ) {
        require(codeImages.isNotEmpty()) { "코드 이미지가 필요합니다" }
        require(codeName != null) { "기본 정보를 먼저 입력해주세요" }
        
        this.codeImage = serializeList(codeImages)
        this.essentialCompleted = true
        this.essentialCompletedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
    
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
        this.bigCity = sido
        this.smallCity = sigugun
        this.job = jobCategory
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
        alcohol: String?,
        smoke: String?,
        personalities: List<String>,
        representativeQuestion: codel.question.domain.Question?,
        representativeAnswer: String?
    ) {
        validatePersonalityDomainRules(personalities, height, mbti)
        
        this.hairLength = hairLength
        this.bodyType = bodyType
        this.height = height
        this.style = serializeList(styles)
        this.mbti = mbti
        this.alcohol = alcohol
        this.smoke = smoke
        this.personalities = serializeList(personalities)
        this.representativeQuestion = representativeQuestion
        this.representativeAnswer = representativeAnswer
        
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

    fun updateHiddenProfileInfo(
        loveLanguage: String,
        affectionStyle: String,
        contactStyle: String,
        dateStyle: String,
        conflictResolutionStyle: String,
        relationshipValues: String
    ) {
        validateHiddenInfoDomainRules()
        
        this.loveLanguage = loveLanguage
        this.affectionStyle = affectionStyle
        this.contactStyle = contactStyle
        this.dateStyle = dateStyle
        this.conflictResolutionStyle = conflictResolutionStyle
        this.relationshipValues = relationshipValues
        
        this.updatedAt = LocalDateTime.now()
    }
    
    fun updateHiddenProfileImages(
        faceImages: List<String>
    ) {
        require(faceImages.isNotEmpty()) { "얼굴 이미지가 필요합니다" }
        require(loveLanguage != null) { "Hidden Profile 정보를 먼저 입력해주세요" }
        
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
    fun getStylesList(): List<String> = deserializeString(style)
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
    private fun validateEssentialInfoDomainRules(
        birthDate: LocalDate,
        interests: List<String>
    ) {
        require(!birthDate.isAfter(LocalDate.now())) {
            "생년월일은 미래 날짜일 수 없습니다"
        }
        require(interests.isNotEmpty()) { "관심사가 필요합니다" }
    }
    
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
    
    private fun validateHiddenInfoDomainRules() {
        require(isPublicProfileComplete()) { "공개 프로필을 먼저 완성해야 합니다" }
    }
    
    private fun validateHiddenDomainRules(faceImages: List<String>) {
        require(isPublicProfileComplete()) { "공개 프로필을 먼저 완성해야 합니다" }
        require(faceImages.isNotEmpty()) { "얼굴 이미지가 필요합니다" }
    }


    // ===== get...OrThrow 메서드들 =====
    
    // Essential Profile
    fun getCodeNameOrThrow(): String = codeName ?: throw MemberException(HttpStatus.BAD_REQUEST, "닉네임이 설정되지 않았습니다.")
    fun getBirthDateOrThrow(): LocalDate = birthDate ?: throw MemberException(HttpStatus.BAD_REQUEST, "생년월일이 설정되지 않았습니다.")
    fun getBigCityOrThrow(): String = bigCity ?: throw MemberException(HttpStatus.BAD_REQUEST, "시/도가 설정되지 않았습니다.")
    fun getSmallCityOrThrow(): String = smallCity ?: throw MemberException(HttpStatus.BAD_REQUEST, "시/군/구가 설정되지 않았습니다.")
    fun getJobOrThrow(): String = job ?: throw MemberException(HttpStatus.BAD_REQUEST, "직업이 설정되지 않았습니다.")
    // Personality Profile
    fun getHairLengthOrThrow(): String = hairLength ?: throw MemberException(HttpStatus.BAD_REQUEST, "헤어 길이가 설정되지 않았습니다.")
    fun getBodyTypeOrThrow(): String = bodyType ?: throw MemberException(HttpStatus.BAD_REQUEST, "체형이 설정되지 않았습니다.")
    fun getHeightOrThrow(): Int = height ?: throw MemberException(HttpStatus.BAD_REQUEST, "키가 설정되지 않았습니다.")
    fun getStyleOrThrow(): String = style ?: throw MemberException(HttpStatus.BAD_REQUEST, "스타일이 설정되지 않았습니다.")
    fun getMbtiOrThrow(): String = mbti ?: throw MemberException(HttpStatus.BAD_REQUEST, "MBTI가 설정되지 않았습니다.")
    fun getAlcoholOrThrow(): String = alcohol ?: throw MemberException(HttpStatus.BAD_REQUEST, "음주 스타일이 설정되지 않았습니다.")
    fun getSmokeOrThrow(): String = smoke ?: throw MemberException(HttpStatus.BAD_REQUEST, "흡연 스타일이 설정되지 않았습니다.")
    fun getPersonalitiesOrThrow(): String = personalities ?: throw MemberException(HttpStatus.BAD_REQUEST, "성격이 설정되지 않았습니다.")
    fun getRepresentativeQuestionOrThrow():Question = representativeQuestion ?: throw MemberException(HttpStatus.BAD_REQUEST, "대표 질문이 설정되지 않았습니다.")
    fun getRepresentativeAnswerOrThrow(): String = representativeAnswer ?: throw MemberException(HttpStatus.BAD_REQUEST, "대표 답변이 설정되지 않았습니다.")
    
    // Hidden Profile
    fun getLoveLanguageOrThrow(): String = loveLanguage ?: throw MemberException(HttpStatus.BAD_REQUEST, "사랑의 언어가 설정되지 않았습니다.")
    fun getAffectionStyleOrThrow(): String = affectionStyle ?: throw MemberException(HttpStatus.BAD_REQUEST, "애정 표현 스타일이 설정되지 않았습니다.")
    fun getContactStyleOrThrow(): String = contactStyle ?: throw MemberException(HttpStatus.BAD_REQUEST, "연락 스타일이 설정되지 않았습니다.")
    fun getDateStyleOrThrow(): String = dateStyle ?: throw MemberException(HttpStatus.BAD_REQUEST, "데이트 스타일이 설정되지 않았습니다.")
    fun getConflictResolutionStyleOrThrow(): String = conflictResolutionStyle ?: throw MemberException(HttpStatus.BAD_REQUEST, "갈등 해결 스타일이 설정되지 않았습니다.")
    fun getRelationshipValuesOrThrow(): String = relationshipValues ?: throw MemberException(HttpStatus.BAD_REQUEST, "연애 가치관이 설정되지 않았습니다.")
    
    // 기타
    fun getIntroduceOrThrow(): String = introduce ?: throw MemberException(HttpStatus.BAD_REQUEST, "자기소개가 설정되지 않았습니다.")
    fun getMemberOrThrow(): Member = member ?: throw MemberException(HttpStatus.BAD_REQUEST, "회원 정보가 설정되지 않았습니다.")

}
