package codel.member.business.signup

import codel.config.Loggable
import codel.member.business.SignupService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 본인인증 기능 추가 전 전략
 *
 * 본인인증 이미지 제출 기능이 없던 구버전 앱(1.2.0 미만)용 전략입니다.
 * 히든 프로필 이미지 제출 시 회원 상태에 따라 다르게 처리합니다:
 * - PERSONALITY_COMPLETED: 히든 이미지 등록 후 HIDDEN_COMPLETED 상태로 변경 (정상 회원가입 완료)
 */
@Component
class PreVerificationStrategy(
    private val signupService: SignupService,
    private val memberJpaRepository: MemberJpaRepository,
    private val asyncNotificationService: IAsyncNotificationService
) : SignupStrategy, Loggable {

    @Transactional
    override fun handleHiddenImages(
        member: Member,
        images: List<MultipartFile>
    ): ResponseEntity<Any> {
        log.info {
            "본인인증 전 플로우 - userId: ${member.getIdOrThrow()}, " +
            "status: ${member.memberStatus}, appVersion: < 1.2.0"
        }

        // 히든 이미지 등록 (기존 SignupService 로직 재활용)
        signupService.registerHiddenImages(member, images)

        member.completeHiddenProfile()
        memberJpaRepository.save(member)
        log.info {
            "정상 가입 플로우 완료 - userId: ${member.getIdOrThrow()}, " +
            "status: HIDDEN_COMPLETED"
        }

        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getIdOrThrow().toString(),
                    title = "${member.getProfileOrThrow().getCodeNameOrThrow()}님이 심사를 요청하였습니다.",
                    body = "code:L 프로필 심사 요청이 왔습니다.",
                ),
        )

        return ResponseEntity.ok().build()
    }
}
