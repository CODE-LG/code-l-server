package codel.chat.business.strategy

import codel.config.Loggable
import org.springframework.stereotype.Component

/**
 * 질문 추천 전략 선택 Resolver
 *
 * 앱 버전을 기반으로 적절한 QuestionRecommendStrategy를 선택합니다.
 * - 1.3.0 미만: 기존 랜덤 질문 추천 (LegacyRandomQuestionStrategy)
 * - 1.3.0 이상: 카테고리 기반 질문 추천 (CategoryBasedQuestionStrategy)
 */
@Component
class QuestionRecommendStrategyResolver(
    private val categoryBasedStrategy: CategoryBasedQuestionStrategy,
    private val legacyRandomStrategy: LegacyRandomQuestionStrategy
) : Loggable {

    companion object {
        private const val CATEGORY_FEATURE_VERSION_MAJOR = 1
        private const val CATEGORY_FEATURE_VERSION_MINOR = 3
    }

    /**
     * 앱 버전에 따라 적절한 전략을 선택합니다.
     *
     * @param appVersion 앱 버전 (X-App-Version 헤더)
     * @return 선택된 전략
     */
    fun resolveStrategy(appVersion: String?): QuestionRecommendStrategy {
        log.debug { "질문 추천 전략 선택 시작 - appVersion: $appVersion" }

        return when {
            isNewApp(appVersion) -> {
                log.info { "CategoryBasedQuestionStrategy 선택 - appVersion: $appVersion" }
                categoryBasedStrategy
            }
            else -> {
                log.info { "LegacyRandomQuestionStrategy 선택 - appVersion: ${appVersion ?: "null"}" }
                legacyRandomStrategy
            }
        }
    }

    /**
     * 1.3.0 이상이면 신규 앱으로 간주
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

            val isNew = major > CATEGORY_FEATURE_VERSION_MAJOR ||
                (major == CATEGORY_FEATURE_VERSION_MAJOR && minor >= CATEGORY_FEATURE_VERSION_MINOR)

            log.debug { "앱 버전 파싱: $version → major=$major, minor=$minor, isNew=$isNew" }
            isNew
        } catch (e: Exception) {
            log.warn(e) { "앱 버전 파싱 실패: $version → 구버전으로 간주" }
            false
        }
    }
}
