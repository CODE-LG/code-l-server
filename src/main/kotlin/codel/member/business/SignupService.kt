package codel.member.business

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.presentation.request.EssentialProfileRequest
import codel.member.presentation.request.PhoneVerificationRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@Service
@Transactional
class SignupService(
    private val memberService: MemberService,
    private val imageUploader: ImageUploader
) {

    /**
     * 전화번호 인증 완료 처리
     */
    fun completePhoneVerification(member: Member, request: PhoneVerificationRequest) {
        // TODO: 실제 전화번호 인증 로직 (SMS 코드 검증 등)
        // 여기서 request.phoneNumber와 request.verificationCode 검증
        
        val currentMember = memberService.findMember(member.getIdOrThrow())
        currentMember.completePhoneVerification()
    }

    /**
     * Essential Profile 정보 등록
     */
    fun registerEssentialProfile(member: Member, request: EssentialProfileRequest) {
        val currentMember = memberService.findMember(member.getIdOrThrow())
        
        // 단계별 검증
        currentMember.validateCanProceedToEssential()
        
        // 요청 데이터 검증
        request.validateSelf()
        
        // Profile 정보 업데이트
        val profile = currentMember.getProfileOrThrow()
        profile.updateEssentialProfileInfo(
            codeName = request.codeName,
            birthDate = LocalDate.parse(request.birthDate),
            sido = request.sido,
            sigugun = request.sigugun,
            jobCategory = request.jobCategory,
            interests = request.interests
        )
    }

    /**
     * Essential Profile 이미지 등록 및 완료 처리
     */
    fun registerEssentialImages(member: Member, images: List<MultipartFile>) {
        val currentMember = memberService.findMember(member.getIdOrThrow())
        
        // 기본 정보가 먼저 등록되어 있는지 검증
        val profile = currentMember.getProfileOrThrow()
        require(profile.codeName != null) {
            "Essential Profile 정보를 먼저 등록해주세요"
        }
        
        // 기존 이미지 업로드 로직 재활용
        val codeImage = uploadCodeImage(images)
        
        // Profile 이미지 업데이트 및 완료 처리
        profile.updateEssentialProfileImages(codeImage.urls)
        
        // Essential Profile 완료 상태로 변경
        currentMember.completeEssentialProfile()
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
}
