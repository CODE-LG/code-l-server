package codel.member.business

import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.chat.domain.ChatRoomStatus
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.*
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.presentation.response.FullProfileResponse
import codel.member.presentation.response.MemberProfileDetailResponse
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.signal.domain.SignalStatus.*
import codel.signal.infrastructure.SignalJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Transactional
@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val imageUploader: ImageUploader,
    private val memberJpaRepository: MemberJpaRepository,
    private val profileJpaRepository: ProfileJpaRepository,
    private val signalJpaRepository: SignalJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val notificationService: NotificationService,
    private val blockMemberRelationJpaRepository: BlockMemberRelationJpaRepository,
) {
    fun loginMember(member: Member): Member {
        val loginMember = memberRepository.loginMember(member)

        if (loginMember.isWithdrawn()) {
            val withdrawDate = loginMember.getUpdateDate()
            val formattedDate = "%04d-%02d-%02d".format(
                withdrawDate.year,
                withdrawDate.monthValue,
                withdrawDate.dayOfMonth
            )

            val errorMessage = "해당 계정은 $formattedDate 에 탈퇴 처리되어 로그인이 불가능합니다."

            throw MemberException(HttpStatus.FORBIDDEN, errorMessage)
        }

        return loginMember
    }

    @Transactional(readOnly = true)
    fun findMember(
        oauthType: OauthType,
        oauthId: String,
    ): Member = memberRepository.findMember(oauthType, oauthId)

    @Transactional(readOnly = true)
    fun findMember(memberId: Long): Member = memberRepository.findMember(memberId)

    private fun uploadCodeImage(files: List<MultipartFile>): CodeImage = CodeImage(files.map { file -> imageUploader.uploadFile(file) })

    private fun sendNotification(member: Member) {
        notificationService.send(
            Notification(
                type = NotificationType.DISCORD,
                targetId = member.getIdOrThrow().toString(), // DISCORD는 없어도 됨
                title = "[회원가입 요청]",
                body = member.getProfileOrThrow().getCodeNameOrThrow() + "님이 회원가입을 요청했습니다.",
            ),
        )
    }

    private fun uploadFaceImage(files: List<MultipartFile>): FaceImage = FaceImage(files.map { file -> imageUploader.uploadFile(file) })

    fun saveFcmToken(
        member: Member,
        fcmToken: String,
    ) {
        memberRepository.updateMemberFcmToken(member, fcmToken)
    }

    @Transactional(readOnly = true)
    fun findPendingMembers(): List<Member> = memberRepository.findPendingMembers()

    fun approveMember(memberId: Long): Member {
        val member = memberRepository.findMember(memberId)

        member.memberStatus = MemberStatus.DONE
        return memberRepository.updateMember(member)
    }

    fun rejectMember(
        memberId: Long,
        reason: String,
    ): Member {
        val member = memberRepository.findMember(memberId)

        memberRepository.saveRejectReason(member, reason)
        member.reject(reason)

        return memberRepository.updateMember(member)
    }

    @Transactional(readOnly = true)
    fun findRejectReason(member: Member): String = memberRepository.findRejectReason(member)

    @Transactional(readOnly = true)
    fun findMyProfile(member: Member): FullProfileResponse {
        val memberId = member.getIdOrThrow()
        val findMember = memberRepository.findMember(memberId)
        return FullProfileResponse.createFull(findMember, isMyProfile = true)
    }

    @Transactional(readOnly = true)
    fun recommendMembers(member: Member): List<Member> {
        val now = LocalDateTime.now()
        val todayMidnight = now.toLocalDate().atStartOfDay()
        val sevenDaysAgo = now.minusDays(7)
        val seed = DailySeedProvider.generateDailySeedForMember(member.getIdOrThrow())
        val candidates = memberJpaRepository.findRandomMembersStatusDoneWithProfile(member.getIdOrThrow(), seed)

        if (candidates.isEmpty()) return emptyList()

        // 1. 가장 최근 추천 시간 기준으로 추천 풀 생성
        val lastRecommendationTime = getLastRecommendationTime(now)
        val excludeIds = makeExcludesMemberIds(member, candidates, sevenDaysAgo, lastRecommendationTime)

        val recommendationPool = candidates.filter { candidate ->
            candidate.id !in excludeIds
        }.take(5)

        // 2. 현재 차단된 사용자들을 실시간으로 제외
        val currentlyBlockedIds = blockMemberRelationJpaRepository.findBlockMembersBy(member.getIdOrThrow())
            .mapNotNull { it.blockedMember.id }

        return recommendationPool.filter { candidate ->
            candidate.id !in currentlyBlockedIds
        }
    }

    /**
     * 가장 최근의 추천 시간을 구합니다. (10시 또는 22시)
     */
    private fun getLastRecommendationTime(now: LocalDateTime): LocalDateTime {
        val currentHour = now.hour
        val today = now.toLocalDate()

        return when {
            currentHour >= 22 -> today.atTime(22, 0) // 오늘 22시
            currentHour >= 10 -> today.atTime(10, 0) // 오늘 10시
            else -> today.minusDays(1).atTime(22, 0) // 어제 22시
        }
    }

    private fun makeExcludesMemberIds(
        member: Member,
        candidates: List<Member>,
        sevenDaysAgo: LocalDateTime,
        targetTime: LocalDateTime
    ): MutableList<Long> {
        val excludeIdsByTimeAndStatus =
            signalJpaRepository
                .findExcludedToMemberIdsAtMidnight(
                    member,
                    candidates,
                    sevenDaysAgo,
                    targetTime,
                ).toMutableList()

        // targetTime 기준으로 차단된 사용자들만 포함 (실시간 차단은 별도 처리)
        val blockedMemberIds = blockMemberRelationJpaRepository.findBlockMembersBeforeTime(member.getIdOrThrow(), targetTime)

        val allExcludeIds = excludeIdsByTimeAndStatus
        allExcludeIds += blockedMemberIds
        return allExcludeIds
    }

    @Transactional(readOnly = true)
    fun getRandomMembers(
        member: Member,
        page: Int,
        size: Int,
    ): Page<Member> {
        val now = LocalDateTime.now()
        val todayMidnight = now.toLocalDate().atStartOfDay()
        val sevenDaysAgo = now.minusDays(7)

        val seed = DailySeedProvider.generateMemberSeedEveryTenHours(member.getIdOrThrow())
        val candidates = memberJpaRepository.findRandomMembersStatusDone(member.getIdOrThrow(),seed)

        val allExcludeIds = makeExcludesMemberIds(member, candidates, sevenDaysAgo, todayMidnight)


        val currentlyBlockedIds = blockMemberRelationJpaRepository.findBlockMembersBy(member.getIdOrThrow())
            .mapNotNull { it.blockedMember.id }

        allExcludeIds += currentlyBlockedIds

        val result = candidates.filter { candidate ->
            candidate.id !in allExcludeIds
        }.take(size)

        val pageable = PageRequest.of(page, size)

        return PageImpl(result, pageable, result.size.toLong())
    }

    fun findMembersWithFilter(
        keyword: String?,
        status: String?,
        pageable: Pageable,
    ): Page<Member> {
        val statusEnum = status?.let { runCatching { MemberStatus.valueOf(it) }.getOrNull() }
        return memberJpaRepository.findMembersWithFilter(keyword, statusEnum, pageable)
    }

    fun countAllMembers(): Long = memberJpaRepository.count()

    fun countPendingMembers(): Long = memberJpaRepository.countByMemberStatus(MemberStatus.PENDING)

    fun findMemberProfile(
        me: Member,
        partnerId: Long,
    ): MemberProfileDetailResponse {
        val member =
            memberJpaRepository.findByMemberId(partnerId) ?: throw MemberException(
                HttpStatus.BAD_REQUEST,
                "해당 id에 일치하는 멤버가 없습니다.",
            )

        val findTopByFromMemberAndToMemberOrderByIdDesc =
            signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(me, member)

        if (findTopByFromMemberAndToMemberOrderByIdDesc != null) {
            val status = findTopByFromMemberAndToMemberOrderByIdDesc.senderStatus
            if (status == REJECTED) {
                val updatedAt = findTopByFromMemberAndToMemberOrderByIdDesc.updatedAt.toLocalDate()
                val now = LocalDate.now()
                val daysBetween = ChronoUnit.DAYS.between(updatedAt, now)

                if (daysBetween > 7) {
                    return MemberProfileDetailResponse.create(member, NONE)
                }
            }

            if (status == APPROVED) {
                // 상대와 관련된 ChatRoom을 찾아와서 ChatRoomId값을 찾는다.
                val findChatRoomByMembers =
                    chatRoomJpaRepository.findChatRoomByMembers(member.getIdOrThrow(), partnerId)
                        ?: throw ChatException(HttpStatus.BAD_REQUEST, "상대방과 관련된 채팅방을 찾을 수 없습니다.")

                return when (findChatRoomByMembers.status) {
                    ChatRoomStatus.UNLOCKED -> MemberProfileDetailResponse.create(member, status, true)
                    else -> MemberProfileDetailResponse.create(member, status, false)
                }
            }
            return MemberProfileDetailResponse.create(member, status)
        }
        return MemberProfileDetailResponse.create(member, NONE)
    }

    fun completePhoneVerification(member: Member) {
        member.completePhoneVerification()
    }

    /**
     * 회원 탈퇴 처리
     */
    fun withdrawMember(member: Member) {
        member.withdraw()
        memberRepository.updateMember(member)
        
        // TODO: JWT 토큰 블랙리스트 처리 고려
        // TODO: 필요시 추가 처리 (알림, 로깅 등)
    }
}
