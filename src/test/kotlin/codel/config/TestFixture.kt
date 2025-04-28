package codel.config

import codel.auth.TokenProvider
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.infrastructure.RejectReasonJpaRepository
import codel.member.infrastructure.entity.MemberEntity
import codel.member.infrastructure.entity.ProfileEntity
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TestFixture {
    lateinit var memberSignup: Member
    lateinit var memberCodeSurvey: Member
    lateinit var memberCodeProfileImage: Member
    lateinit var memberPending: Member
    lateinit var memberDone: Member

    lateinit var token: String

    @Autowired
    lateinit var tokenProvider: TokenProvider

    @Autowired
    lateinit var profileJpaRepository: ProfileJpaRepository

    @Autowired
    lateinit var memberJpaRepository: MemberJpaRepository

    @Autowired
    lateinit var rejectReasonJpaRepository: RejectReasonJpaRepository

    @BeforeEach
    fun setUp() {
        rejectReasonJpaRepository.deleteAll()
        memberJpaRepository.deleteAll()
        profileJpaRepository.deleteAll()

        memberSignup = saveMemberSignup()
        memberCodeSurvey = saveMemberCodeSurvey()
        memberCodeProfileImage = saveMemberCodeProfileImage()
        memberPending = saveMemberPending()
        memberDone = saveMemberDone()

        token = tokenProvider.provide(memberSignup)
    }

    private fun saveMemberSignup(): Member {
        val memberEntity =
            MemberEntity(
                oauthType = OauthType.KAKAO,
                oauthId = "hogee1",
                memberStatus = MemberStatus.SIGNUP,
            )
        val saveEntity = memberJpaRepository.save(memberEntity)
        return saveEntity.toDomain()
    }

    private fun saveMemberCodeSurvey(): Member {
        val profileEntity =
            ProfileEntity(
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
            )
        val saveProfileEntity = profileJpaRepository.save(profileEntity)
        val memberEntity =
            MemberEntity(
                profileEntity = saveProfileEntity,
                oauthType = OauthType.KAKAO,
                oauthId = "hogee2",
                memberStatus = MemberStatus.CODE_SURVEY,
            )
        val saveEntity = memberJpaRepository.save(memberEntity)
        return saveEntity.toDomain()
    }

    private fun saveMemberCodeProfileImage(): Member {
        val profileEntity =
            ProfileEntity(
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
            )
        val saveProfileEntity = profileJpaRepository.save(profileEntity)
        val memberEntity =
            MemberEntity(
                profileEntity = saveProfileEntity,
                oauthType = OauthType.KAKAO,
                oauthId = "hogee3",
                memberStatus = MemberStatus.CODE_PROFILE_IMAGE,
            )
        val saveEntity = memberJpaRepository.save(memberEntity)
        return saveEntity.toDomain()
    }

    private fun saveMemberPending(): Member {
        val profileEntity =
            ProfileEntity(
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
            )
        val saveProfileEntity = profileJpaRepository.save(profileEntity)
        val memberEntity =
            MemberEntity(
                profileEntity = saveProfileEntity,
                oauthType = OauthType.KAKAO,
                oauthId = "hogee4",
                memberStatus = MemberStatus.PENDING,
            )
        val saveEntity = memberJpaRepository.save(memberEntity)
        return saveEntity.toDomain()
    }

    private fun saveMemberDone(): Member {
        val profileEntity =
            ProfileEntity(
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
            )
        val saveProfileEntity = profileJpaRepository.save(profileEntity)
        val memberEntity =
            MemberEntity(
                profileEntity = saveProfileEntity,
                oauthType = OauthType.KAKAO,
                oauthId = "hogee5",
                memberStatus = MemberStatus.DONE,
            )
        val saveEntity = memberJpaRepository.save(memberEntity)
        return saveEntity.toDomain()
    }
}
