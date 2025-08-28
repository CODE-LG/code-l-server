package codel.member.business

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.Profile
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.presentation.request.EssentialProfileRequest
import codel.member.presentation.request.HiddenProfileRequest
import codel.member.presentation.request.PersonalityProfileRequest
import codel.question.business.QuestionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@Service
@Transactional
class SignupService(
    private val imageUploader: ImageUploader,
    private val questionService: QuestionService,
    private val memberJpaRepository: MemberJpaRepository,
    private val profileJpaRepository: ProfileJpaRepository
) {

    /**
     * 전화번호 인증 완료 처리
     */
    fun completePhoneVerification(member: Member) {
        member.completePhoneVerification()
        
        // Profile 객체 생성 (빈 상태로)
        member.createEmptyProfile()
        
        memberJpaRepository.save(member)
    }

    /**
     * Essential Profile 정보 등록
     */
    fun registerEssentialProfile(member: Member, request: EssentialProfileRequest) {
        // 단계별 검증
        member.validateCanProceedToEssential()
        
        // 요청 데이터 검증
        request.validateSelf()

        val profile = member.getProfileOrThrow()
        profile.updateEssentialProfileInfo(
            codeName = request.codeName,
            birthDate = LocalDate.parse(request.birthDate),
            sido = request.bigCity,
            sigugun = request.smallCity,
            jobCategory = request.jobCategory,
        )
        memberJpaRepository.save(member)
    }

    /**
     * Essential Profile 이미지 등록 및 완료 처리
     */
    fun registerEssentialImages(member: Member, images: List<MultipartFile>) {
        // 기본 정보가 먼저 등록되어 있는지 검증
        val profile = member.getProfileOrThrow()
        require(profile.codeName != null) {
            "Essential Profile 정보를 먼저 등록해주세요"
        }
        
        // 기존 이미지 업로드 로직 재활용
        val codeImage = uploadCodeImage(images)
        
        // Profile 이미지 업데이트 및 완료 처리
        profile.updateEssentialProfileImages(codeImage.urls)

        // Essential Profile 완료 상태로 변경
//        member.completeEssentialProfile()

        // 거절당했을 때,
        if(member.memberStatus == MemberStatus.REJECT){
            member.memberStatus = MemberStatus.PENDING
        }

        memberJpaRepository.save(member)
    }

    /**
     * 코드 이미지 업로드 (기존 MemberService 로직 재활용)
     */
    private fun uploadCodeImage(files: List<MultipartFile>): CodeImage {
        return CodeImage(files.map { file -> imageUploader.uploadFile(file) })
    }

    /**
     * 얼굴 이미지 업로드 (추후 Hidden Profile에서 사용)
     */
    private fun uploadFaceImage(files: List<MultipartFile>): FaceImage {
        return FaceImage(files.map { file -> imageUploader.uploadFile(file) })
    }

    /**
     * Personality Profile 등록 및 완료 처리
     */
    fun registerPersonalityProfile(member: Member, request: PersonalityProfileRequest) {
        // 단계별 검증
        member.validateCanProceedToPersonality()
        
        // 요청 데이터 검증
        request.validateSelf()
        
        // Question 조회 (질문이 있는 경우)
        val representativeQuestion = request.questionId?.let { 
            questionService.findQuestionById(it)
        }
        
        // Profile 정보 업데이트
        val profile = member.getProfileOrThrow()
        profile.updatePersonalityProfile(
            hairLength = request.hairLength,
            bodyType = request.bodyType,
            height = request.height,
            styles = request.styles,
            mbti = request.mbti,
            alcohol = request.drinkingStyle,
            smoke = request.smokingStyle,
            personalities = request.personalities,
            representativeQuestion = representativeQuestion,
            representativeAnswer = request.answer,
            interests = request.interests,
        )
        
        //TODO ::  Personality Profile 완료 상태로 변경  ( 다음 업데이트까지 세이브포인트 생략 )
//        member.completePersonalityProfile()

        if(member.memberStatus == MemberStatus.REJECT){
            member.memberStatus = MemberStatus.PENDING
        }

        memberJpaRepository.save(member)
    }

    /**
     * Hidden Profile 정보 등록
     */
    fun registerHiddenProfile(member: Member, request: HiddenProfileRequest) {
        // 단계별 검증
        member.validateCanProceedToHidden()
        
        // Profile 정보 업데이트 (이미지 제외)
        val profile = member.getProfileOrThrow()
        profile.updateHiddenProfileInfo(
            loveLanguage = request.loveLanguage,
            affectionStyle = request.affectionStyle,
            contactStyle = request.contactStyle,
            dateStyle = request.dateStyle,
            conflictResolutionStyle = request.conflictResolutionStyle,
            relationshipValues = request.relationshipValues
        )
        memberJpaRepository.save(member)
    }

    /**
     * Hidden Profile 이미지 등록 및 완료 처리
     */
    fun registerHiddenImages(member: Member, images: List<MultipartFile>) {
        // Hidden Profile 정보가 먼저 등록되어 있는지 검증
        val profile = member.getProfileOrThrow()
        require(profile.loveLanguage != null) {
            "Hidden Profile 정보를 먼저 등록해주세요"
        }

        // 기존 이미지 업로드 로직 재활용
        val faceImage = uploadFaceImage(images)
        
        // Profile 이미지 업데이트 및 완료 처리
        profile.updateHiddenProfileImages(faceImage.urls)
        
        // Hidden Profile 완료 상태로 변경 (PENDING 상태로)
        member.completeHiddenProfile()

        memberJpaRepository.save(member)
    }
}
