package codel.config

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.notification.business.NotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile

@SpringBootTest
@ExtendWith(DataCleanerExtension::class)
class TestFixture {
    lateinit var memberSignup: Member
    lateinit var memberCodeSurvey: Member
    lateinit var memberCodeProfileImage: Member
    lateinit var memberPending: Member
    lateinit var memberDone: Member

    @MockBean
    lateinit var notificationService: NotificationService

    @Autowired
    lateinit var profileJpaRepository: ProfileJpaRepository

    @Autowired
    lateinit var memberJpaRepository: MemberJpaRepository

    @BeforeEach
    fun setUp() {
        memberSignup = saveMemberSignup()
        memberCodeSurvey = saveMemberCodeSurvey()
        memberCodeProfileImage = saveMemberCodeProfileImage()
        memberPending = saveMemberPending()
        memberDone = saveMemberDone()
    }

    private fun saveMemberSignup(): Member {
        val member = Member(
            oauthType = OauthType.KAKAO,
            oauthId = "hogee1",
            memberStatus = MemberStatus.SIGNUP,
            email = "hogee@hogee"
        )
        return memberJpaRepository.save(member)
    }

    private fun saveMemberCodeSurvey(): Member {
        val profile = Profile(
            codeName = "hogee",
            age = 28,
            job = "백엔드 개발자",
            alcohol = "자주 마심",
            smoke = "비흡연자 - 흡연자와 교류 NO",
            hobby = "영화 & 드라마,여행 & 캠핑",
            style = "표현을 잘하는 직진형,상대가 필요할 때 항상 먼저 연락하는 스타일",
            bigCity = "경기도",
            smallCity = "성남시",
            mbti = "isfj",
            introduce = "잘부탁드립니다!",
            question = "주로 여행은 어떤 스타일이신가요?",
            answer = "계획 없이 즉흥적으로 돌아다니는 걸 좋아해요"
        )
        val member = Member(
            profile = profile,
            oauthType = OauthType.KAKAO,
            oauthId = "hogee2",
            memberStatus = MemberStatus.CODE_SURVEY,
            email = "hogee@hogee"
        )
        profile.member = member
        return memberJpaRepository.save(member)
    }

    private fun saveMemberCodeProfileImage(): Member {
        val profile = Profile(
            codeName = "hogee",
            age = 28,
            job = "백엔드 개발자",
            alcohol = "자주 마심",
            smoke = "비흡연자 - 흡연자와 교류 NO",
            hobby = "영화 & 드라마,여행 & 캠핑",
            style = "표현을 잘하는 직진형,상대가 필요할 때 항상 먼저 연락하는 스타일",
            bigCity = "경기도",
            smallCity = "성남시",
            mbti = "isfj",
            introduce = "잘부탁드립니다!",
            codeImage = "www.s3.aws.com1",
            question = "주로 여행은 어떤 스타일이신가요?",
            answer = "계획 없이 즉흥적으로 돌아다니는 걸 좋아해요"
        )
        val member = Member(
            profile = profile,
            oauthType = OauthType.KAKAO,
            oauthId = "hogee3",
            memberStatus = MemberStatus.CODE_PROFILE_IMAGE,
            email = "hogee@hogee"
        )
        profile.member = member
        return memberJpaRepository.save(member)
    }

    private fun saveMemberPending(): Member {
        val profile = Profile(
            codeName = "hogee",
            age = 28,
            job = "백엔드 개발자",
            alcohol = "자주 마심",
            smoke = "비흡연자 - 흡연자와 교류 NO",
            hobby = "영화 & 드라마,여행 & 캠핑",
            style = "표현을 잘하는 직진형,상대가 필요할 때 항상 먼저 연락하는 스타일",
            bigCity = "경기도",
            smallCity = "성남시",
            mbti = "isfj",
            introduce = "잘부탁드립니다!",
            codeImage = "www.s3.aws.com1",
            faceImage = "www.s3.aws1,www.s3.aws2,www.s3.aws3",
            question = "주로 여행은 어떤 스타일이신가요?",
            answer = "계획 없이 즉흥적으로 돌아다니는 걸 좋아해요"
        )
        val member = Member(
            profile = profile,
            oauthType = OauthType.KAKAO,
            oauthId = "hogee4",
            memberStatus = MemberStatus.PENDING,
            email = "hogee@hogee"
        )
        profile.member = member
        return memberJpaRepository.save(member)
    }

    private fun saveMemberDone(): Member {
        val profile = Profile(
            codeName = "hogee",
            age = 28,
            job = "백엔드 개발자",
            alcohol = "자주 마심",
            smoke = "비흡연자 - 흡연자와 교류 NO",
            hobby = "영화 & 드라마,여행 & 캠핑",
            style = "표현을 잘하는 직진형,상대가 필요할 때 항상 먼저 연락하는 스타일",
            bigCity = "경기도",
            smallCity = "성남시",
            mbti = "isfj",
            introduce = "잘부탁드립니다!",
            codeImage = "www.s3.aws.com1",
            faceImage = "www.s3.aws1,www.s3.aws2,www.s3.aws3",
            question = "주로 여행은 어떤 스타일이신가요?",
            answer = "계획 없이 즉흥적으로 돌아다니는 걸 좋아해요"
        )
        val member = Member(
            profile = profile,
            oauthType = OauthType.KAKAO,
            oauthId = "hogee5",
            memberStatus = MemberStatus.DONE,
            email = "hogee@hogee"
        )
        profile.member = member
        return memberJpaRepository.save(member)
    }

    fun createMember(
        oauthId: String = "hogee1",
        status: MemberStatus = MemberStatus.SIGNUP,
        email: String = "hogee@hogee"
    ) = Member(
        oauthType = OauthType.KAKAO,
        oauthId = oauthId,
        memberStatus = status,
        email = email
    )

    fun createProfile(
        codeName: String = "hogee"
    ) = Profile(
        codeName = codeName,
        age = 28,
        job = "백엔드 개발자",
        alcohol = "자주 마심",
        smoke = "비흡연자 - 흡연자와 교류 NO",
        hobby = "영화 & 드라마,여행 & 캠핑",
        style = "표현을 잘하는 직진형,상대가 필요할 때 항상 먼저 연락하는 스타일",
        bigCity = "경기도",
        smallCity = "성남시",
        mbti = "isfj",
        introduce = "잘부탁드립니다!",
        question = "주로 여행은 어떤 스타일이신가요?",
        answer = "계획 없이 즉흥적으로 돌아다니는 걸 좋아해요"
    )

    fun createMockFile(
        name: String,
        content: String = "data"
    ) = MockMultipartFile("file", name, "image/png", content.toByteArray())
}