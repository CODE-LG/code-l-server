package codel.member.business

import codel.admin.presentation.request.ImageRejection
import codel.block.infrastructure.BlockMemberRelationJpaRepository
import codel.chat.domain.ChatRoomStatus
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.*
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.infrastructure.FaceImageRepository
import codel.member.infrastructure.CodeImageRepository
import codel.member.presentation.response.FullProfileResponse
import codel.member.presentation.response.MemberProfileDetailResponse
import codel.member.presentation.response.UpdateCodeImagesResponse
import codel.member.presentation.response.UpdateRepresentativeQuestionResponse
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
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
    private val faceImageRepository: FaceImageRepository,
    private val codeImageRepository: CodeImageRepository,
    private val questionJpaRepository: codel.question.infrastructure.QuestionJpaRepository,
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

    /**
     * 관리자용: 이미지 포함해서 회원 조회 (Fetch Join)
     * MultipleBagFetchException 방지를 위해 2단계로 조회
     */
    @Transactional(readOnly = true)
    fun findMemberWithImages(memberId: Long): Member {
        // 1단계: Member + Profile만 조회
        val member = memberJpaRepository.findMemberWithProfile(memberId)
            ?: throw MemberException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다: $memberId")

        // 2단계: 이미지들을 강제로 초기화 (트랜잭션 범위 내에서)
        member.profile?.let { profile ->
            profile.codeImages.size  // Lazy Loading 초기화
            profile.faceImages.size  // Lazy Loading 초기화
        }

        return member
    }

    private fun uploadCodeImage(files: List<MultipartFile>): CodeImageVO =
        CodeImageVO(files.map { file -> imageUploader.uploadFile(file) })

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

    private fun uploadFaceImage(files: List<MultipartFile>): FaceImageVO =
        FaceImageVO(files.map { file -> imageUploader.uploadFile(file) })

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

        // 내가 차단한, 나를 차단한 사용자들을 실시간으로 제외
        val currentlyBlockedIds = blockMemberRelationJpaRepository.findBlockMembersBy(member.getIdOrThrow())
            .mapNotNull { it.blockedMember.id }
        val currentlyBlockerIds = blockMemberRelationJpaRepository.findBlockerMembersTo(member.getIdOrThrow())
            .mapNotNull { it.blockedMember.id }

        return recommendationPool.filter { candidate ->
            candidate.id !in currentlyBlockedIds + currentlyBlockerIds
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
        val excludeFromMemberIdsAtMidnight =
            signalJpaRepository
                .findExcludedFromMemberIdsAtMidnight(
                    member,
                    candidates,
                    sevenDaysAgo,
                    targetTime,
                ).toMutableList()

        val excludeToMemberIdsAtMidnight =
            signalJpaRepository
                .findExcludedToMemberIdsAtMidnight(
                    member,
                    candidates,
                    sevenDaysAgo,
                    targetTime,
                ).toMutableList()

        // targetTime 기준으로 차단된 사용자들만 포함 (실시간 차단은 별도 처리)
        val blockedMemberIdsByMe =
            blockMemberRelationJpaRepository.findBlockedMemberIdByMeBeforeTime(member.getIdOrThrow(), targetTime)
        val blockerMemberIdsToMe =
            blockMemberRelationJpaRepository.findBlockMembersByOtherBeforeTime(member.getIdOrThrow(), targetTime)

        val allExcludeIds = excludeFromMemberIdsAtMidnight
        allExcludeIds += excludeToMemberIdsAtMidnight

        allExcludeIds += blockedMemberIdsByMe
        allExcludeIds += blockerMemberIdsToMe

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
        val candidates = memberJpaRepository.findRandomMembersStatusDone(member.getIdOrThrow(), seed)

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

        if (me.getIdOrThrow() == partnerId) {
            return MemberProfileDetailResponse.createMyProfileResponse(memberRepository.findMember(me.getIdOrThrow()))
        }
        val partner = memberJpaRepository.findByMemberId(partnerId)
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "해당 id에 일치하는 멤버가 없습니다.")

        // 1. 차단 상태 확인 (시그널 상태보다 우선)
        if (isBlockedRelationship(me, partner)) {
            throw MemberException(HttpStatus.FORBIDDEN, "차단된 사용자입니다.")
        }

        // 2. 양방향 시그널 상태 확인 - 더 최근 시그널을 우선으로
        val myLatestSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(me, partner)
        val partnerLatestSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(partner, me)

        // 3. 시그널 상태에 따른 처리
        return when {
            // 양쪽 모두 시그널이 없는 경우
            myLatestSignal == null && partnerLatestSignal == null -> {
                MemberProfileDetailResponse.create(partner, SignalStatus.NONE)
            }

            // 양쪽 모두 시그널이 있는 경우 - 더 최근 것을 기준으로 판단
            myLatestSignal != null && partnerLatestSignal != null -> {
                handleBothSignalsExist(me, partner, myLatestSignal, partnerLatestSignal)
            }

            // 내가 보낸 시그널만 있는 경우
            myLatestSignal != null && partnerLatestSignal == null -> {
                handleOnlyMySignalExists(me, partner, myLatestSignal)
            }

            // 상대가 보낸 시그널만 있는 경우  
            myLatestSignal == null && partnerLatestSignal != null -> {
                handleOnlyPartnerSignalExists(me, partner, partnerLatestSignal)
            }

            else -> {
                MemberProfileDetailResponse.create(partner, SignalStatus.NONE)
            }
        }
    }

    /**
     * 차단 관계 확인
     */
    private fun isBlockedRelationship(me: Member, partner: Member): Boolean {
        val myBlockedMemberIds = blockMemberRelationJpaRepository.findBlockMembersBy(me.getIdOrThrow())
            .mapNotNull { it.blockedMember.id }
        val partnerBlockedMemberIds = blockMemberRelationJpaRepository.findBlockMembersBy(partner.getIdOrThrow())
            .mapNotNull { it.blockedMember.id }

        return partner.getIdOrThrow() in myBlockedMemberIds ||
                me.getIdOrThrow() in partnerBlockedMemberIds
    }

    /**
     * 내가 보낸 시그널만 있는 경우
     */
    private fun handleOnlyMySignalExists(me: Member, partner: Member, mySignal: Signal): MemberProfileDetailResponse {
        return when (mySignal.senderStatus) {
            PENDING, PENDING_HIDDEN -> {
                // 내 시그널이 대기 중
                MemberProfileDetailResponse.create(partner, mySignal.senderStatus)
            }

            REJECTED -> {
                // 내가 거절당한 경우 - 7일 쿨다운 확인
                if (isRejectionCooldownExpired(mySignal)) {
                    MemberProfileDetailResponse.create(partner, NONE)
                } else {
                    MemberProfileDetailResponse.create(partner, REJECTED)
                }
            }

            APPROVED -> {
                // 내 시그널이 승인된 경우 - 채팅방 상태 확인
                handleApprovedSignal(me, partner, mySignal.senderStatus)
            }

            NONE -> {
                MemberProfileDetailResponse.create(partner, NONE)
            }
        }
    }

    /**
     * 상대가 보낸 시그널만 있는 경우
     */
    private fun handleOnlyPartnerSignalExists(
        me: Member,
        partner: Member,
        partnerSignal: Signal
    ): MemberProfileDetailResponse {
        return when (partnerSignal.senderStatus) {
            PENDING, PENDING_HIDDEN -> {
                // 상대의 시그널이 대기 중 - 내가 응답해야 함
                MemberProfileDetailResponse.create(partner, PENDING)
            }

            REJECTED -> {
                // 상대가 나를 거절한 경우 - 7일 쿨다운 확인  
                if (isRejectionCooldownExpired(partnerSignal)) {
                    MemberProfileDetailResponse.create(partner, NONE)
                } else {
                    MemberProfileDetailResponse.create(partner, REJECTED)
                }
            }

            APPROVED -> {
                // 상대가 나를 승인한 경우 - 채팅방 상태 확인
                handleApprovedSignal(me, partner, partnerSignal.senderStatus)
            }

            NONE -> {
                MemberProfileDetailResponse.create(partner, NONE)
            }
        }
    }

    /**
     * 양쪽 모두 시그널이 있는 경우
     */
    private fun handleBothSignalsExist(
        me: Member,
        partner: Member,
        mySignal: Signal,
        partnerSignal: Signal
    ): MemberProfileDetailResponse {

        // 더 최근 시그널을 기준으로 상태 결정
        val latestSignal = if (mySignal.updatedAt.isAfter(partnerSignal.updatedAt)) {
            mySignal
        } else {
            partnerSignal
        }

        val isMySignalLatest = (latestSignal == mySignal)

        return when {
            // 둘 다 승인 상태인 경우
            mySignal.senderStatus in listOf(APPROVED) &&
                    partnerSignal.senderStatus in listOf(APPROVED) -> {
                handleApprovedSignal(me, partner, APPROVED)
            }

            // 최신 시그널이 거절인 경우
            latestSignal.senderStatus == REJECTED -> {
                if (isRejectionCooldownExpired(latestSignal)) {
                    MemberProfileDetailResponse.create(partner, NONE)
                } else {
                    MemberProfileDetailResponse.create(partner, REJECTED)
                }
            }

            // 최신 시그널이 대기 중인 경우
            latestSignal.senderStatus in listOf(PENDING, PENDING_HIDDEN) -> {
                MemberProfileDetailResponse.create(partner, PENDING)
            }

            // 한쪽은 승인, 한쪽은 대기/거절인 경우
            else -> {
                // 승인된 시그널이 있다면 채팅방 상태 확인
                val approvedSignal = listOf(mySignal, partnerSignal).find {
                    it.senderStatus in listOf(APPROVED)
                }

                if (approvedSignal != null) {
                    handleApprovedSignal(me, partner, approvedSignal.senderStatus)
                } else {
                    // 둘 다 승인이 아닌 경우 최신 상태 반환
                    MemberProfileDetailResponse.create(partner, latestSignal.senderStatus)
                }
            }
        }
    }

    /**
     * 승인된 시그널 처리 - 채팅방 상태 확인
     */
    private fun handleApprovedSignal(
        me: Member,
        partner: Member,
        signalStatus: SignalStatus
    ): MemberProfileDetailResponse {
        val chatRoom = chatRoomJpaRepository.findChatRoomByMembers(
            me.getIdOrThrow(),
            partner.getIdOrThrow()
        ) ?: throw ChatException(HttpStatus.BAD_REQUEST, "승인된 관계이지만 채팅방을 찾을 수 없습니다.")

        val isUnlocked = (chatRoom.status == ChatRoomStatus.UNLOCKED)
        return MemberProfileDetailResponse.create(partner, signalStatus, isUnlocked)
    }

    /**
     * 거절 쿨다운 만료 확인
     */
    private fun isRejectionCooldownExpired(signal: Signal): Boolean {
        val daysSinceRejection = ChronoUnit.DAYS.between(
            signal.updatedAt.toLocalDate(),
            LocalDate.now()
        )
        return daysSinceRejection > 7
    }

    /**
     * 시그널 전송 가능 여부 확인 (별도 메서드로 제공)
     * 프론트엔드에서 버튼 활성화 여부 판단에 사용
     */
    fun canSendSignalTo(me: Member, partnerId: Long): Boolean {
        return try {
            val partner = memberJpaRepository.findByMemberId(partnerId) ?: return false

            // 차단된 관계면 불가능
            if (isBlockedRelationship(me, partner)) return false

            val myLatestSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(me, partner)
            val partnerLatestSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(partner, me)

            when {
                // 시그널이 아예 없으면 가능
                myLatestSignal == null && partnerLatestSignal == null -> true

                // 내 시그널만 있는 경우
                myLatestSignal != null && partnerLatestSignal == null -> {
                    when (myLatestSignal.senderStatus) {
                        REJECTED -> isRejectionCooldownExpired(myLatestSignal)
                        APPROVED -> false // 이미 승인됨
                        else -> false // 대기 중이면 불가능
                    }
                }

                // 상대 시그널만 있는 경우 - 응답할 수 있음
                myLatestSignal == null && partnerLatestSignal != null -> {
                    when (partnerLatestSignal.senderStatus) {
                        PENDING, PENDING_HIDDEN -> true // 응답 가능
                        REJECTED -> isRejectionCooldownExpired(partnerLatestSignal)
                        else -> false
                    }
                }

                // 둘 다 있는 경우 - 복잡한 로직이므로 안전하게 false
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun completePhoneVerification(member: Member) {
        member.completePhoneVerification()
    }

    /**
     * 회원 탈퇴 처리
     */
    fun withdrawMember(member: Member, reason :  String) {
        member.withdraw(reason)
        memberRepository.updateMember(member)

        // TODO: JWT 토큰 블랙리스트 처리 고려
        // TODO: 필요시 추가 처리 (알림, 로깅 등)
    }

    // ========== 통계 관련 메서드 ==========

    /**
     * 일별 가입자 통계 (최근 30일)
     */
    fun getDailySignupStats(): List<Pair<String, Long>> {
        val startDate = LocalDateTime.now().minusDays(30)
        val rawData = memberJpaRepository.getDailySignupStats(startDate)

        return rawData.map { row ->
            val date = row[0].toString()
            val count = (row[1] as Number).toLong()
            date to count
        }
    }

    /**
     * 회원 상태별 통계
     */
    fun getMemberStatusStats(): Map<String, Long> {
        val rawData = memberJpaRepository.getMemberStatusStats()

        return rawData.associate { row ->
            val status = row[0].toString()
            val count = (row[1] as Number).toLong()
            status to count
        }
    }

    /**
     * 월별 가입자 통계 (최근 12개월)
     */
    fun getMonthlySignupStats(): List<Triple<Int, Int, Long>> {
        val startDate = LocalDateTime.now().minusMonths(12)
        val rawData = memberJpaRepository.getMonthlySignupStats(startDate)

        return rawData.map { row ->
            val year = (row[0] as Number).toInt()
            val month = (row[1] as Number).toInt()
            val count = (row[2] as Number).toLong()
            Triple(year, month, count)
        }
    }

    /**
     * 오늘 가입자 수
     */
    fun getTodaySignupCount(): Long = memberJpaRepository.getTodaySignupCount()

    /**
     * 최근 7일 가입자 수
     */
    fun getWeeklySignupCount(): Long {
        val startDate = LocalDateTime.now().minusDays(7)
        return memberJpaRepository.getRecentSignupCount(startDate)
    }

    /**
     * 최근 30일 가입자 수
     */
    fun getMonthlySignupCount(): Long {
        val startDate = LocalDateTime.now().minusDays(30)
        return memberJpaRepository.getRecentSignupCount(startDate)
    }

    /**
     * 고급 필터링을 지원하는 회원 목록 조회
     */
    fun findMembersWithFilter(
        keyword: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
        sort: String?,
        direction: String?,
        pageable: Pageable
    ): Page<Member> {
        val statusEnum = if (!status.isNullOrBlank()) {
            try {
                MemberStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                null
            }
        } else {
            null
        }

        // 정렬 처리를 위한 새로운 Pageable 생성
        val sortedPageable = createSortedPageable(pageable, sort, direction)

        // 새로운 메서드 사용
        return memberJpaRepository.findMembersWithFilterAdvanced(keyword, statusEnum, sortedPageable)
    }

    /**
     * 정렬 옵션에 따른 Pageable 생성
     */
    private fun createSortedPageable(pageable: Pageable, sort: String?, direction: String?): Pageable {
        val sortDirection = if (direction == "asc") {
            org.springframework.data.domain.Sort.Direction.ASC
        } else {
            org.springframework.data.domain.Sort.Direction.DESC
        }

        val sortBy = when (sort) {
            "id" -> "id"
            "email" -> "email"
            "codeName" -> "profile.codeName"
            "memberStatus" -> "memberStatus"
            "createdAt" -> "createdAt"
            else -> "createdAt"
        }

        return PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            org.springframework.data.domain.Sort.by(sortDirection, sortBy)
        )
    }

    /**
     * 상태별 회원 수 조회
     */
    fun countMembersByStatus(status: String): Long {
        return try {
            val statusEnum = MemberStatus.valueOf(status)
            memberJpaRepository.countByMemberStatus(statusEnum)
        } catch (e: IllegalArgumentException) {
            0L
        }
    }

    /**
     * 이미지별 거절 처리 (신규)
     */
    fun rejectMemberWithImages(
        memberId: Long,
        faceImageRejections: List<ImageRejection>?,
        codeImageRejections: List<ImageRejection>?
    ) : Member{
        val member = findMember(memberId)
        val profile = member.getProfileOrThrow()

        // 얼굴 이미지 거절 처리
        faceImageRejections?.forEach { rejection ->
            val image = faceImageRepository.findById(rejection.imageId)
                .orElseThrow { MemberException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다: ${rejection.imageId}") }

            if (image.profile.id != profile.id) {
                throw MemberException(HttpStatus.BAD_REQUEST, "해당 프로필의 이미지가 아닙니다")
            }

            image.isApproved = false
            image.rejectionReason = rejection.reason
        }

        // 코드 이미지 거절 처리
        codeImageRejections?.forEach { rejection ->
            val image = codeImageRepository.findById(rejection.imageId)
                .orElseThrow { MemberException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다: ${rejection.imageId}") }

            if (image.profile.id != profile.id) {
                throw MemberException(HttpStatus.BAD_REQUEST, "해당 프로필의 이미지가 아닙니다")
            }

            image.isApproved = false
            image.rejectionReason = rejection.reason
        }

        // 회원 상태를 REJECT로 변경
        member.memberStatus = MemberStatus.REJECT

        // Member의 rejectReason도 업데이트 (기존 호환성)
        val reasons = mutableListOf<String>()
        if (!faceImageRejections.isNullOrEmpty()) {
            reasons.add("얼굴 이미지 거절")
        }
        if (!codeImageRejections.isNullOrEmpty()) {
            reasons.add("코드 이미지 거절")
        }
        member.rejectReason = reasons.joinToString(", ")

        memberJpaRepository.save(member)

        return member
    }

    /**
     * 코드 이미지만 수정 (사용자용)
     * - existingIds를 통해 유지할 이미지 지정
     * - 지정되지 않은 기존 이미지는 삭제하고 새 이미지로 대체
     */
    fun updateCodeImages(
        member: Member,
        codeImages: List<MultipartFile>?,
        existingIds: List<Long>?
    ): UpdateCodeImagesResponse {
        // 변경 없음 처리 (프론트가 아무것도 안 보냈을 때)
        if (codeImages.isNullOrEmpty() && existingIds.isNullOrEmpty()) {
            return UpdateCodeImagesResponse(
                uploadedCount = 0,
                profileStatus = member.memberStatus,
                message = "변경된 이미지가 없습니다"
            )
        }

        val findMember = memberJpaRepository
            .findByMemberIdWithProfileAndCodeImages(member.getIdOrThrow())
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "회원을 찾을 수 없습니다.")
        val profile = findMember.getProfileOrThrow()

        val keepIdSet = (existingIds ?: emptyList()).toSet()

        // 1) 유지할 기존 엔티티 (정확히 keepIds에 속하는 것만)
        val kept = profile.codeImages
            .sortedBy { it.orders }
            .filter { it.id != null && it.id in keepIdSet }

        // 2) 신규 업로드 개수/총 개수 검증
        val newCount = codeImages?.size ?: 0
        val total = kept.size + newCount
        if (total !in 1..3) {
            throw MemberException(
                HttpStatus.BAD_REQUEST,
                "코드 이미지는 1~3개여야 합니다. (현재: 유지 ${kept.size}개 + 신규 ${newCount}개 = ${total}개)"
            )
        }

        // 3) 유지하지 않을 기존 이미지는 컬렉션에서 제거 (orphanRemoval로 DB 삭제)
        profile.codeImages
            .filter { it !in kept }
            .toList()
            .forEach { img ->
                profile.codeImages.remove(img)
                // S3 삭제는 트랜잭션 커밋 후 비동기로 권장 (이벤트/리스너)
            }

        // 4) 신규 업로드 → 엔티티 생성 → 컬렉션에 add
        val startOrder = kept.size
        val newEntities = codeImages.orEmpty().mapIndexed { idx, file ->
            val url = imageUploader.uploadFile(file)
            CodeImage(
                profile = profile,            // add 전에 명시해도 무방
                url = url,
                orders = startOrder + idx,
                isApproved = true
            )
        }
        // 컬렉션에 추가하면서 연관 보장
        newEntities.forEach { img ->
            profile.codeImages.add(img)
        }

        // 5) orders 재정렬(혹시 모를 갭/중복 정리)
        profile.codeImages
            .sortedBy { it.orders }
            .forEachIndexed { index, img -> img.orders = index }

        // 6) Dual Write: 문자열 필드 동기화
        profile.updateCodeImageUrls(profile.codeImages.sortedBy { it.orders }.map { it.url })

        // 7) 상태 변경이 필요하면 여기서 처리 (예: 재심사 대기)
        // findMember.memberStatus = MemberStatus.PENDING

        // @Transactional + 영속 상태 → save 호출 불필요
        return UpdateCodeImagesResponse(
            uploadedCount = newCount,
            profileStatus = findMember.memberStatus,
            message = "코드 이미지 ${total}개로 변경되었습니다 (유지: ${kept.size}개, 신규: ${newCount}개)"
        )
    }

    /**
     * 대표 질문 및 답변 수정 (사용자용)
     */
    fun updateRepresentativeQuestion(
        member: Member,
        questionId: Long,
        answer: String
    ): UpdateRepresentativeQuestionResponse {
        val profile = member.getProfileOrThrow()

        // Question 조회
        val question = questionJpaRepository.findById(questionId)
            .orElseThrow { MemberException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다: $questionId") }

        // 질문이 활성화되어 있는지 확인
        if (!question.isActive) {
            throw MemberException(HttpStatus.BAD_REQUEST, "비활성화된 질문은 선택할 수 없습니다")
        }

        // Question과 Answer 업데이트
        profile.representativeQuestion = question
        profile.representativeAnswer = answer

        profileJpaRepository.save(profile)

        return UpdateRepresentativeQuestionResponse(
            representativeQuestion = UpdateRepresentativeQuestionResponse.QuestionInfo(
                id = question.getIdOrThrow(),
                content = question.content,
                category = question.category.name
            ),
            representativeAnswer = answer,
            message = "대표 질문 및 답변이 수정되었습니다."
        )
    }
}
