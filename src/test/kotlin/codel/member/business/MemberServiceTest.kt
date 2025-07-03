package codel.member.business

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class MemberServiceTest @Autowired constructor(
    private val memberService: MemberService,
    private val memberJpaRepository: MemberJpaRepository,
    private val profileJpaRepository: ProfileJpaRepository,
) {
    @DisplayName("회원가입 시 추가된 프로필일 경우 멤버상태가 CODE_SURVEY로 상태가 변경된다")
    @Test
    fun changeMemberStatusByInsertProfileAtSignup() {
        // given
        val member = memberJpaRepository.save(
            Member(
                oauthType = OauthType.KAKAO,
                oauthId = "hogee1",
                memberStatus = MemberStatus.SIGNUP,
                email = "hogee@hogee",
            )
        )
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
        )

        // when
        memberService.upsertProfile(member, profile)

        // then
        val findMember = memberJpaRepository.findByOauthTypeAndOauthId(member.oauthType, member.oauthId)
        assertThat(findMember.memberStatus).isEqualTo(MemberStatus.CODE_SURVEY)
    }

    @DisplayName("기존 프로필이 정상적으로 수정된다")
    @Test
    fun upsertProfile_update() {
        // given
        val member = memberJpaRepository.save(
            Member(
                oauthType = OauthType.KAKAO,
                oauthId = "hogee1",
                memberStatus = MemberStatus.SIGNUP,
                email = "hogee@hogee",
            )
        )
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
        )
        memberService.upsertProfile(member, profile)

        // when: 프로필 정보 수정
        val updatedProfile = Profile(
            codeName = "updateName",
            age = 22,
            job = "운동선수",
            alcohol = "자주",
            smoke = "비흡연",
            hobby = "독서",
            style = "활발함",
            bigCity = "서울",
            smallCity = "송파구",
            mbti = "ISTP",
            introduce = "연락 주세요"
        )
        memberService.upsertProfile(member, updatedProfile)

        // then
        val updatedMember = memberJpaRepository.findById(member.id!!).get()
        assertAll(
            { assertThat(updatedMember.profile).isNotNull },
            { assertThat(updatedMember.profile!!.codeName).isEqualTo("updateName") },
            { assertThat(updatedMember.profile!!.job).isEqualTo("운동선수") },
            { assertThat(updatedMember.profile!!.introduce).isEqualTo("연락 주세요") }
        )
    }
}