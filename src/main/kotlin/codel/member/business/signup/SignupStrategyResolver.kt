package codel.member.business.signup

import codel.config.Loggable
import org.springframework.stereotype.Component

/**
 * 회원가입 전략 선택 Resolver
 *
 * 앱 버전을 기반으로 적절한 SignupStrategy를 선택합니다.
 * - 1.2.0 미만: 본인인증 기능 추가 전 전략 (PreVerificationStrategy)
 * - 1.2.0 이상: 본인인증 기능 추가 후 전략 (PostVerificationStrategy)
 */
@Component
class SignupStrategyResolver(
    private val postVerificationStrategy: PostVerificationStrategy,
    private val preVerificationStrategy: PreVerificationStrategy
) : Loggable {

    /**
     * 앱 버전에 따라 적절한 전략을 선택합니다.
     *
     * @param appVersion 앱 버전 (X-App-Version 헤더)
     * @return 선택된 전략
     */
    fun resolveStrategy(appVersion: String?): SignupStrategy {
        log.debug {
            "전략 선택 시작 - appVersion: $appVersion"
        }

        return when {
            // 신규 앱 (1.2.0 이상) → 본인인증 후 전략
            isNewApp(appVersion) -> {
                log.info {
                    "PostVerificationStrategy 선택 - appVersion: $appVersion"
                }
                postVerificationStrategy
            }

            // 구버전 앱 (1.2.0 미만) → 본인인증 전 전략
            else -> {
                log.info {
                    "PreVerificationStrategy 선택 - appVersion: ${appVersion ?: "null"}"
                }
                preVerificationStrategy
            }
        }
    }

    /**
     * 신규 앱 버전인지 판단
     *
     * 1.2.0 이상이면 신규 앱으로 간주합니다.
     *
     * @param version 앱 버전 문자열 (예: "1.2.0")
     * @return 신규 앱 여부
     */
    private fun isNewApp(version: String?): Boolean {
        if (version == null) {
            log.debug { "앱 버전 null → 구버전으로 간주" }
            return false
        }

        return try {
            val parts = version.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0

            // 1.2.0 이상이면 신규 앱
            val isNew = major > 1 || (major == 1 && minor >= 2)

            log.debug {
                "앱 버전 파싱: $version → major=$major, minor=$minor, isNew=$isNew"
            }

            isNew
        } catch (e: Exception) {
            log.warn(e) { "앱 버전 파싱 실패: $version → 구버전으로 간주" }
            false  // 파싱 실패 시 안전하게 구버전으로 간주
        }
    }
}
