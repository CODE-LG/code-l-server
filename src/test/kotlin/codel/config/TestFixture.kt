package codel.config

import codel.member.domain.*
import org.springframework.mock.web.MockMultipartFile

open class TestFixture {
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
    )

    fun createMockFile(
        name: String,
        content: String = "data"
    ) = MockMultipartFile("file", name, "image/png", content.toByteArray())
}