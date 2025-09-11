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

        if(me.getIdOrThrow() == partnerId){
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
    private fun handleOnlyPartnerSignalExists(me: Member, partner: Member, partnerSignal: Signal): MemberProfileDetailResponse {
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
    private fun handleApprovedSignal(me: Member, partner: Member, signalStatus: SignalStatus): MemberProfileDetailResponse {
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
    fun withdrawMember(member: Member) {
        member.withdraw()
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
}
