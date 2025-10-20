package codel.recommendation.business

import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.DailySeedProvider
import codel.member.domain.Member
import codel.member.infrastructure.MemberJpaRepository
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationType
import codel.signal.infrastructure.SignalJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 추천 시스템의 제외 로직을 전담하는 서비스
 *
 * 제외 대상:
 * 1. 본인
 * 2. 추천 이력 기반 중복 방지 (N일 내 추천받은 사용자)
 * 3. 차단 관계 (내가 차단 + 나를 차단)
 * 4. 최근 시그널 관계 (7일 내 시그널 주고받음)
 * 5. 채팅방 관계 (현재 또는 과거 채팅방에서 만난 사용자)
 */
@Service
class RecommendationExclusionService(
    private val signalJpaRepository: SignalJpaRepository,
    private val blockMemberRelationJpaRepository: BlockMemberRelationJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
    private val historyService: RecommendationHistoryService,
    private val config: RecommendationConfig
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 추천에서 제외해야 할 모든 사용자 ID를 반환합니다.
     *
     * @param user 기준 사용자
     * @param type 추천 타입 (DAILY_CODE_MATCHING or CODE_TIME)
     * @return 제외해야 할 사용자 ID Set
     */
    fun getAllExcludedIds(
        user: Member,
        type: RecommendationType
    ): Set<Long> {
        val excludeIds = mutableSetOf<Long>()

        // 1. 본인 제외
        val userId = user.getIdOrThrow()
        excludeIds.add(userId)
        logger.debug { "제외 - 본인: [$userId]" }

        // 2. 추천 이력 기반 중복 방지
        val historyExcludeIds = if (config.allowDuplicate) {
            historyService.getExcludedUserIds(user)
        } else {
            historyService.getExcludedUserIdsByType(user, type)
        }
        excludeIds.addAll(historyExcludeIds)
        logger.debug {
            "제외 - ${config.repeatAvoidDays}일 내 추천 이력: $historyExcludeIds (${historyExcludeIds.size}명)"
        }

        // 3. 차단 관계
        val blockedIds = getBlockedMemberIds(user)
        excludeIds.addAll(blockedIds)
        logger.debug {
            "제외 - 차단 관계: $blockedIds (${blockedIds.size}명)"
        }

        // 4. 최근 시그널 관계
        val signalIds = getRecentSignalMemberIds(user)
        excludeIds.addAll(signalIds)
        logger.debug {
            "제외 - 7일 내 시그널: $signalIds (${signalIds.size}명)"
        }

        // 5. 채팅방 관계 (새로 추가)
        val chatRoomIds = getChatRoomMemberIds(user)
        excludeIds.addAll(chatRoomIds)
        logger.debug {
            "제외 - 채팅방 관계: $chatRoomIds (${chatRoomIds.size}명)"
        }

        logger.info {
            "제외 대상 조회 완료 - userId: ${user.getIdOrThrow()}, " +
                    "type: $type, 전체 제외: ${excludeIds.size}개 " +
                    "(본인:1, history:${historyExcludeIds.size}, " +
                    "blocked:${blockedIds.size}, signal:${signalIds.size}, " +
                    "chatroom:${chatRoomIds.size}), " +
                    "제외 ID 목록: $excludeIds"
        }

        return excludeIds
    }

    /**
     * 차단 관계에 있는 사용자 ID를 반환합니다.
     * - 내가 차단한 사용자
     * - 나를 차단한 사용자
     *
     * @param user 기준 사용자
     * @return 차단 관계 사용자 ID Set
     */
    fun getBlockedMemberIds(user: Member): Set<Long> {
        val userId = user.getIdOrThrow()

        // 내가 차단한 사용자들
        val blockedByMe = blockMemberRelationJpaRepository
            .findBlockMembersBy(userId)
            .mapNotNull { it.blockedMember.id }

        // 나를 차단한 사용자들
        val blockingMe = blockMemberRelationJpaRepository
            .findBlockerMembersTo(userId)
            .mapNotNull { it.blockedMember.id }

        val result = (blockedByMe + blockingMe).toSet()

        logger.debug {
            "차단 관계 조회 - userId: $userId, " +
                    "blockedByMe: ${blockedByMe.size}명, blockingMe: ${blockingMe.size}명, " +
                    "total: ${result.size}명"
        }

        return result
    }

    /**
     * 최근 시그널을 주고받은 사용자 ID를 반환합니다.
     * 7일 이내 시그널 관계가 있는 사용자를 제외합니다.
     *
     * MemberService의 로직과 동일하게 동작합니다:
     * - 가장 최근 추천 시간(10시 또는 22시) 기준으로 제외
     * - 7일 내 시그널 주고받은 사용자 제외
     *
     * @param user 기준 사용자
     * @return 시그널 관계 사용자 ID Set
     */
    fun getRecentSignalMemberIds(user: Member): Set<Long> {
        val userId = user.getIdOrThrow()
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)
        val targetTime = getLastRecommendationTime(LocalDateTime.now())

        // 내가 시그널 보낸 사용자들 (최근 추천 시간 기준)
        val sentSignalIds = signalJpaRepository.findExcludedFromMemberIdsAtMidnight(
            user, sevenDaysAgo, targetTime
        )

        // 내가 시그널 받은 사용자들 (최근 추천 시간 기준)
        val receivedSignalIds = signalJpaRepository.findExcludedToMemberIdsAtMidnight(
            user, sevenDaysAgo, targetTime
        )

        val result = (sentSignalIds + receivedSignalIds).toSet()

        logger.debug {
            "시그널 관계 조회 - userId: $userId, " +
                    "targetTime: $targetTime, " +
                    "sent: ${sentSignalIds.size}명, received: ${receivedSignalIds.size}명, " +
                    "total: ${result.size}명"
        }

        return result
    }

    /**
     * 채팅방에서 만난 적이 있는 사용자 ID를 반환합니다.
     * 현재 활성 채팅방뿐만 아니라 나간 채팅방의 사용자도 모두 제외합니다.
     *
     * @param user 기준 사용자
     * @return 채팅방 관계 사용자 ID Set
     */
    fun getChatRoomMemberIds(user: Member): Set<Long> {
        val userId = user.getIdOrThrow()

        // 사용자가 속한 모든 채팅방의 ChatRoomMember 조회 (나간 채팅방 포함)
        val myChatRoomMembers = chatRoomMemberJpaRepository.findAllByMember(user)

        // 각 채팅방의 상대방 ID 추출
        val partnerIds = myChatRoomMembers.mapNotNull { myChatRoomMember ->
            val chatRoomId = myChatRoomMember.chatRoom.getIdOrThrow()

            // 같은 채팅방의 다른 멤버 찾기
            val otherMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMemberNot(
                chatRoomId, user
            )

            otherMember?.member?.id
        }.toSet()

        logger.debug {
            "채팅방 관계 조회 - userId: $userId, " +
                    "myChatRooms: ${myChatRoomMembers.size}개, " +
                    "partners: ${partnerIds.size}명, " +
                    "partnerIds: $partnerIds"
        }

        return partnerIds
    }

    /**
     * 가장 최근의 추천 시간을 구합니다. (10시 또는 22시)
     * MemberService의 로직과 동일합니다.
     *
     * @param now 현재 시간
     * @return 가장 최근 추천 시간 (10시 또는 22시)
     */
    private fun getLastRecommendationTime(now: LocalDateTime): LocalDateTime {
        val currentHour = now.hour
        val today = now.toLocalDate()

        return when {
            currentHour >= 22 -> today.atTime(22, 0)  // 오늘 22시
            currentHour >= 10 -> today.atTime(10, 0)  // 오늘 10시
            else -> today.minusDays(1).atTime(22, 0)  // 어제 22시
        }
    }

    /**
     * 제외 로직 통계 정보 조회 (디버깅/모니터링용)
     *
     * @param user 기준 사용자
     * @param type 추천 타입
     * @return 제외 통계 정보
     */
    fun getExclusionStatistics(user: Member, type: RecommendationType): Map<String, Any> {
        val userId = user.getIdOrThrow()

        val historyExcludeIds = if (config.allowDuplicate) {
            historyService.getExcludedUserIds(user)
        } else {
            historyService.getExcludedUserIdsByType(user, type)
        }

        val blockedIds = getBlockedMemberIds(user)
        val signalIds = getRecentSignalMemberIds(user)
        val allExcludeIds = getAllExcludedIds(user, type)

        return mapOf(
            "userId" to userId,
            "type" to type.name,
            "excludedCounts" to mapOf(
                "self" to 1,
                "history" to historyExcludeIds.size,
                "blocked" to blockedIds.size,
                "signal" to signalIds.size,
                "total" to allExcludeIds.size
            ),
            "targetTime" to getLastRecommendationTime(LocalDateTime.now()).toString(),
            "config" to mapOf(
                "repeatAvoidDays" to config.repeatAvoidDays,
                "allowDuplicate" to config.allowDuplicate
            )
        )
    }
}
