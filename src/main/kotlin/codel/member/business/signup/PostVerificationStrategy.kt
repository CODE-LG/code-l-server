package codel.member.business.signup

import codel.config.Loggable
import codel.member.business.SignupService
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * 본인인증 기능 추가 후 전략
 *
 * 본인인증 이미지 제출 기능이 추가된 신규 앱(1.2.0 이상)용 전략입니다.
 * 히든 프로필 이미지 제출 후, 별도의 본인인증 이미지 제출이 필요합니다.
 * 재심사의 경우 새로운 재심사 전용 API(/v1/profile/review/resubmit)를 사용해야 합니다.
 */
@Component
class PostVerificationStrategy(
    private val signupService: SignupService
) : SignupStrategy, Loggable {

    override fun handleHiddenImages(
        member: Member,
        images: List<MultipartFile>
    ): ResponseEntity<Any> {
        log.info {
            "본인인증 후 플로우 - userId: ${member.getIdOrThrow()}, " +
            "appVersion: >= 1.2.0"
        }

        // SignupService의 registerHiddenImages 호출 (히든 이미지만 등록)
        signupService.registerHiddenImages(member, images)

        return ResponseEntity.ok().build()
    }
}
