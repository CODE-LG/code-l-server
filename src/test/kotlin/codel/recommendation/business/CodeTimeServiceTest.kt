package codel.recommendation.business

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.recommendation.domain.RecommendationConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * CodeTimeService 테스트
 */
@DisplayName("CodeTimeService 테스트")
class CodeTimeServiceTest {

    private lateinit var codeTimeService: CodeTimeService
    private lateinit var config: RecommendationConfig
    private lateinit var bucketService: RecommendationBucketService
    private lateinit var historyService: RecommendationHistoryService
    private lateinit var exclusionService: RecommendationExclusionService
    private lateinit var timeZoneService: TimeZoneService
    private lateinit var agePreferenceResolver: AgePreferenceResolver

    private val testTimeSlot = "10:00"
    private val testStartTime: LocalDateTime = LocalDateTime.of(2025, 1, 28, 1, 0)
    private val testEndTime: LocalDateTime = LocalDateTime.of(2025, 1, 28, 13, 0)

    @BeforeEach
    fun setUp() {
        config = mock(RecommendationConfig::class.java)
        bucketService = mock(RecommendationBucketService::class.java)
        historyService = mock(RecommendationHistoryService::class.java)
        exclusionService = mock(RecommendationExclusionService::class.java)
        timeZoneService = mock(TimeZoneService::class.java)
        agePreferenceResolver = mock(AgePreferenceResolver::class.java)

        codeTimeService = CodeTimeService(
            config = config,
            bucketService = bucketService,
            historyService = historyService,
            exclusionService = exclusionService,
            timeZoneService = timeZoneService,
            agePreferenceResolver = agePreferenceResolver
        )

        // 기본 설정
        Mockito.`when`(config.codeTimeCount).thenReturn(2)
        Mockito.`when`(config.codeTimeSlots).thenReturn(listOf("10:00", "22:00"))
    }

    private fun setupTimeZoneForTest() {
        Mockito.lenient().`when`(timeZoneService.getCurrentTimeSlot(anyNullable()))
            .thenReturn(testTimeSlot)
        Mockito.lenient().`when`(timeZoneService.getTimeSlotRangeInUTC(Mockito.anyString(), anyNullable()))
            .thenReturn(Pair(testStartTime, testEndTime))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNullable(): T = Mockito.any<T>() as T

    @Test
    @DisplayName("기존 추천이 있으면 반환한다")
    fun returnExistingRecommendation() {
        // given
        setupTimeZoneForTest()

        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val existingMembers = listOf(
            createTestMember(2L, "추천B"),
            createTestMember(3L, "추천C"),
            createTestMember(4L, "추천D")
        )

        Mockito.`when`(historyService.getCodeTimeIdsByTimeRange(
            anyNullable(), Mockito.anyString(), anyNullable(), anyNullable()
        )).thenReturn(existingIds)

        Mockito.`when`(exclusionService.getBlockedMemberIds(anyNullable())).thenReturn(emptySet())
        Mockito.`when`(exclusionService.getRecentSignalMemberIds(anyNullable())).thenReturn(emptySet())
        Mockito.`when`(bucketService.getMembersByIds(existingIds)).thenReturn(existingMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(3, result.content.size)
        assertEquals(existingMembers, result.content)
    }

    @Test
    @DisplayName("차단한 사용자는 필터링된다")
    fun filterBlockedMembers() {
        // given
        setupTimeZoneForTest()

        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val memberB = createTestMember(2L, "추천B")
        val memberC = createTestMember(3L, "추천C")
        val memberD = createTestMember(4L, "추천D")
        val allMembers = listOf(memberB, memberC, memberD)

        Mockito.`when`(historyService.getCodeTimeIdsByTimeRange(
            anyNullable(), Mockito.anyString(), anyNullable(), anyNullable()
        )).thenReturn(existingIds)

        Mockito.`when`(exclusionService.getBlockedMemberIds(anyNullable())).thenReturn(setOf(2L))
        Mockito.`when`(exclusionService.getRecentSignalMemberIds(anyNullable())).thenReturn(emptySet())
        // 첫 번째 호출: 모든 멤버 반환 (WITHDRAWN 필터링용)
        Mockito.`when`(bucketService.getMembersByIds(existingIds)).thenReturn(allMembers)
        // 두 번째 호출: 필터링된 멤버만 반환
        val filteredMembers = listOf(memberC, memberD)
        Mockito.`when`(bucketService.getMembersByIds(listOf(3L, 4L))).thenReturn(filteredMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(2, result.content.size)
        assertFalse(result.content.any { it.id == 2L })
        assertTrue(result.content.any { it.id == 3L })
        assertTrue(result.content.any { it.id == 4L })
    }

    @Test
    @DisplayName("추천 순서가 유지된다")
    fun maintainRecommendationOrder() {
        // given
        setupTimeZoneForTest()

        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L, 4L)
        val orderedMembers = listOf(
            createTestMember(2L, "B1-강남"),
            createTestMember(3L, "B1-강남2"),
            createTestMember(4L, "B2-홍대")
        )

        Mockito.`when`(historyService.getCodeTimeIdsByTimeRange(
            anyNullable(), Mockito.anyString(), anyNullable(), anyNullable()
        )).thenReturn(existingIds)

        Mockito.`when`(exclusionService.getBlockedMemberIds(anyNullable())).thenReturn(emptySet())
        Mockito.`when`(exclusionService.getRecentSignalMemberIds(anyNullable())).thenReturn(emptySet())
        Mockito.`when`(bucketService.getMembersByIds(existingIds)).thenReturn(orderedMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(3, result.content.size)
        assertEquals(2L, result.content[0].id)
        assertEquals(3L, result.content[1].id)
        assertEquals(4L, result.content[2].id)
    }

    @Test
    @DisplayName("모든 추천이 필터링되면 빈 페이지를 반환한다")
    fun returnEmptyPage_WhenAllFiltered() {
        // given
        setupTimeZoneForTest()

        val user = createTestMember(1L, "사용자A")
        val existingIds = listOf(2L, 3L)
        val existingMembers = listOf(
            createTestMember(2L, "추천B"),
            createTestMember(3L, "추천C")
        )

        Mockito.`when`(historyService.getCodeTimeIdsByTimeRange(
            anyNullable(), Mockito.anyString(), anyNullable(), anyNullable()
        )).thenReturn(existingIds)

        Mockito.`when`(exclusionService.getBlockedMemberIds(anyNullable())).thenReturn(setOf(2L, 3L))
        Mockito.`when`(exclusionService.getRecentSignalMemberIds(anyNullable())).thenReturn(emptySet())
        Mockito.`when`(bucketService.getMembersByIds(existingIds)).thenReturn(existingMembers)

        // when
        val result = codeTimeService.getCodeTimeRecommendation(user, 0, 10)

        // then
        assertEquals(0, result.content.size)
        assertEquals(0, result.totalElements)
    }

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