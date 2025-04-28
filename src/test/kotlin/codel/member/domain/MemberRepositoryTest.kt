package codel.member.domain

import codel.config.TestFixture
import codel.member.exception.MemberException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MemberRepositoryTest(
    @Autowired
    private val memberRepository: MemberRepository,
) : TestFixture() {
    var profile: Profile =
        Profile(
            codeName = "hogee",
            age = 28,
            job = "백엔드 개발자",
            alcohol = "자주 마심",
            smoke = "비흡연자 - 흡연자와 교류 NO",
            hobby = listOf("영화 & 드라마", "여행 & 캠핑"),
            style = listOf("표현을 잘하는 직진형", "상대가 필요할 때 항상 먼저 연락하는 스타일"),
            bigCity = "경기도",
            smallCity = "성남시",
            mbti = "isfj",
            introduce = "잘부탁드립니다!",
        )

    @DisplayName("프로필을 저장하면 memberStatus 가 CODE_SURVEY 로 바뀐다.")
    @Test
    fun saveProfileTest() {
        val updateMember = memberSignup.updateProfile(profile)
        memberRepository.updateMember(updateMember)
        val findMember = memberRepository.findMember(updateMember.oauthType, updateMember.oauthId)

        assertAll(
            { assertThat(findMember).isNotNull },
            { assertThat(findMember.memberStatus).isEqualTo(MemberStatus.CODE_SURVEY) },
        )
    }

    @DisplayName("멤버 아이디에 해당하는 멤버가 없으면 예외를 반환한다.")
    @Test
    fun saveProfileWithoutMemberTest() {
        val seokMember =
            Member(
                id = 100L,
                oauthType = OauthType.APPLE,
                oauthId = "seok",
            )
        seokMember.updateProfile(profile)
        assertThatThrownBy { memberRepository.updateMember(seokMember) }
            .isInstanceOf(MemberException::class.java)
            .hasMessage("해당 id에 일치하는 멤버가 없습니다.")
    }

    @DisplayName("바꾸고자 하는 멤버 아이디가 없으면 예외를 반환한다.")
    @Test
    fun changeMemberStatusWithoutMemberIdTest() {
        val seokMember =
            Member(
                oauthType = OauthType.APPLE,
                oauthId = "seok",
            )
        seokMember.updateProfile(profile)
        assertThatThrownBy { memberRepository.updateMember(seokMember) }
            .isInstanceOf(MemberException::class.java)
            .hasMessage("id가 없는 멤버 입니다.")
    }
}
