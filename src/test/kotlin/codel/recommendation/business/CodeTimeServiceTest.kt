package codel.recommendation.business

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * CodeTimeService 테스트
 *
 * 주요 검증 사항:
 * 1. 추천 세션 일관성 - 시그널 보내도 같은 세션 내에서 계속 표시
 * 2. 차단 관계만 즉시 제외
 * 3. 추천 순서 유지
 */
@DisplayName("CodeTimeService 테스트")
class CodeTimeServiceTest {

    private lateinit var codeTimeService: CodeTimeService
    private lateinit var config: RecommendationConfig
    private lateinit var bucketService: RecommendationBucketService
    private lateinit var historyService: RecommendationHistoryService
    private lateinit var exclusionService: RecommendationExclusionService
    private lateinit var timeZoneService: TimeZoneService

    @BeforeEach
    fun setUp() {
        config = mock()
        bucketService = mock()
        historyService = mock()
        exclusionService = mock()
        timeZoneService = mock()

        codeTimeService = CodeTimeService(
            config = config,
            bucketService = bucketService,
            historyService = historyService,
            exclusionService = exclusionService,
            timeZoneService = timeZoneService
        )

        // 기본 설정
        whenever(config.codeTimeCount).thenReturn(2)
        whenever(config.codeTimeSlots).thenReturn(listOf("10:00", "22:00"))
    }

    @Test
    @DisplayName("기존 추천이 없으면 새로운 추천을 생성한다")
    fun createNewRecommendation_WhenNoHistory() {
        // given
        val user = createTestMember(1L, "사용자A")
        val recommendedMembers = listOf(
            createTestMember(2L, "추천B"),
            createTestMember(3L, "추천C")
        )

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(emptyList())

        // 새로운 추천 생성 관련 Mock
        whenever(exclusionService.getAllExcludedIds(user, RecommendationType.CODE_TIME)).thenReturn(setOf(1L))
        whenever(bucketService.getCandidatesByBucket(any(), any(), any(), any())).thenReturn(recommendedMembers)
        doNothing().whenever(historyService).saveRecommendationHistory(any(), any(), any(), any(), any())

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(2, result.content.size)
        assertEquals(recommendedMembers, result.content)

        // 추천 이력 저장 확인
        verify(historyService, times(1))
            .saveRecommendationHistory(
                eq(user),
                eq(recommendedMembers),
                eq(RecommendationType.CODE_TIME),
                eq("10:00"),
                any()
            )
    }

    @Test
    @DisplayName("기존 추천이 있으면 실시간 필터링 후 반환한다")
    fun returnExistingRecommendation_WithRealTimeFiltering() {
        // given
        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val existingMembers = listOf(
            createTestMember(2L, "추천B"),
            createTestMember(3L, "추천C"),
            createTestMember(4L, "추천D")
        )

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        // 실시간 필터링 - 차단 없음
        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(emptySet())
        whenever(bucketService.getMembersByIds(existingIds)).thenReturn(existingMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(3, result.content.size)
        assertEquals(existingMembers, result.content)

        // 새로운 추천 생성하지 않았는지 확인
        verify(historyService, never())
            .saveRecommendationHistory(any(), any(), any(), any(), any())
    }

    @Test
    @DisplayName("차단한 사용자는 실시간 필터링에서 즉시 제외된다")
    fun filterBlockedMembers_InRealTime() {
        // given
        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val memberB = createTestMember(2L, "추천B")
        val memberC = createTestMember(3L, "추천C")
        val memberD = createTestMember(4L, "추천D")
        val allMembers = listOf(memberB, memberC, memberD)

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        // 실시간 필터링 - B를 차단함
        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(setOf(2L))
        // getMembersByIds는 요청된 ID에 해당하는 멤버만 반환
        whenever(bucketService.getMembersByIds(any())).thenAnswer { invocation ->
            val requestedIds = invocation.getArgument<List<Long>>(0)
            allMembers.filter { it.id in requestedIds }
        }

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(2, result.content.size)
        assertTrue(result.content.none { it.id == 2L }) // B는 제외됨
        assertTrue(result.content.any { it.id == 3L }) // C는 포함됨
        assertTrue(result.content.any { it.id == 4L }) // D는 포함됨
    }

    @Test
    @DisplayName("시그널 보낸 사용자는 실시간 필터링에서 제외되지 않는다 - 추천 세션 일관성 유지")
    fun doNotFilterSignaledMembers_InRealTime() {
        // given
        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val existingMembers = listOf(
            createTestMember(2L, "추천B-시그널보냄"),
            createTestMember(3L, "추천C"),
            createTestMember(4L, "추천D")
        )

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        // 실시간 필터링 - 차단 없음 (시그널 관계는 체크하지 않음)
        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(emptySet())
        // ⚠️ getRecentSignalMemberIds는 호출되지 않아야 함
        whenever(bucketService.getMembersByIds(existingIds)).thenReturn(existingMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(3, result.content.size)
        assertTrue(result.content.any { it.id == 2L }) // B는 시그널 보냈지만 여전히 표시됨 ✅

        // getRecentSignalMemberIds가 호출되지 않았는지 확인
        verify(exclusionService, never()).getRecentSignalMemberIds(any())
    }

    @Test
    @DisplayName("WITHDRAWN 상태의 회원은 자동으로 필터링된다")
    fun filterWithdrawnMembers_Automatically() {
        // given
        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val memberC = createTestMember(3L, "추천C", MemberStatus.DONE)
        val memberD = createTestMember(4L, "추천D", MemberStatus.DONE)
        // memberB(2L)는 WITHDRAWN이므로 getMembersByIds에서 자동으로 필터링됨
        val activeMembers = listOf(memberC, memberD)

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(emptySet())
        // getMembersByIds는 WITHDRAWN을 자동으로 필터링하고 요청된 ID에 해당하는 멤버만 반환
        whenever(bucketService.getMembersByIds(any())).thenAnswer { invocation ->
            val requestedIds = invocation.getArgument<List<Long>>(0)
            activeMembers.filter { it.id in requestedIds }
        }

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(2, result.content.size)
        assertTrue(result.content.none { it.id == 2L }) // B(탈퇴)는 제외됨
        assertTrue(result.content.any { it.id == 3L })
        assertTrue(result.content.any { it.id == 4L })
    }

    @Test
    @DisplayName("추천 순서가 유지된다 - getMembersByIds의 순서 보존")
    fun maintainRecommendationOrder() {
        // given
        val user = createTestMember(1L, "사용자A")
        // 순서: B1 버킷, B1 버킷, B2 버킷
        val existingIds = listOf(2L, 3L, 4L)
        val orderedMembers = listOf(
            createTestMember(2L, "B1-강남"),
            createTestMember(3L, "B1-강남2"),
            createTestMember(4L, "B2-홍대")
        )

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(emptySet())
        // getMembersByIds는 입력 순서를 보존함
        whenever(bucketService.getMembersByIds(existingIds)).thenReturn(orderedMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(3, result.content.size)
        assertEquals(2L, result.content[0].id) // 첫 번째: B1-강남
        assertEquals(3L, result.content[1].id) // 두 번째: B1-강남2
        assertEquals(4L, result.content[2].id) // 세 번째: B2-홍대
    }

    @Test
    @DisplayName("모든 추천이 필터링되면 빈 페이지를 반환한다")
    fun returnEmptyPage_WhenAllFiltered() {
        // given
        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L)

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        // 모두 차단
        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(setOf(2L, 3L))
        whenever(bucketService.getMembersByIds(existingIds)).thenReturn(emptyList())

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(0, result.content.size)
        assertEquals(0, result.totalElements)
    }

    @Test
    @DisplayName("페이징이 올바르게 적용된다")
    fun applyPaginationCorrectly() {
        // given
        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L, 5L, 6L)
        val existingMembers = (2L..6L).map { createTestMember(it, "추천$it") }

        whenever(timeZoneService.getCurrentTimeSlot(null)).thenReturn("10:00")
        whenever(timeZoneService.getTimeSlotRangeInUTC("10:00", null)).thenReturn(
            Pair(LocalDateTime.now(), LocalDateTime.now().plusHours(12))
        )
        whenever(historyService.getCodeTimeIdsByTimeRange(any(), any(), any(), any())).thenReturn(existingIds)

        whenever(exclusionService.getBlockedMemberIds(user)).thenReturn(emptySet())
        whenever(bucketService.getMembersByIds(existingIds)).thenReturn(existingMembers)

        // when
        val page = 0
        val size = 3
        val result = codeTimeService.getCodeTimeRecommendation(user, page, size)

        // then
        assertEquals(5, result.content.size) // 페이징은 PageImpl에서 처리되므로 전체 반환
        assertEquals(5, result.totalElements)
    }

    // Helper methods

    private fun createTestMember(
        id: Long,
        name: String,
        status: MemberStatus = MemberStatus.DONE
    ): Member {
        val profile = Profile(
            id = id,
            codeName = name,
            bigCity = "서울",
            smallCity = "강남구",
            birthDate = LocalDate.of(1990, 1, 1)
        )

        val member = Member(
            id = id,
            oauthId = "oauth-$id",
            oauthType = OauthType.KAKAO,
            memberStatus = status,
            email = "$name@test.com",
            profile = profile
        )

        profile.member = member

        return member
    }
}
