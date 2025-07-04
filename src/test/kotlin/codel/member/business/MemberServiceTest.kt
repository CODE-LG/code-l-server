package codel.member.business

import codel.member.domain.*
import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.annotation.Transactional
import org.mockito.BDDMockito.given

@SpringBootTest
@Transactional
class MemberServiceTest {
    @Autowired
    lateinit var memberService: MemberService
    @Autowired
    lateinit var memberJpaRepository: MemberJpaRepository
    @Autowired
    lateinit var profileJpaRepository: ProfileJpaRepository
    @MockBean
    lateinit var imageUploader: ImageUploader

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

    @DisplayName("프로필이 없는 경우 코드 이미지를 등록할 수 없다")
    @Test
    fun requireProfileBeforeRegisteringCodeImage() {
        // given
        val member = memberJpaRepository.save(
            Member(
                oauthType = OauthType.KAKAO,
                oauthId = "hogee2",
                memberStatus = MemberStatus.SIGNUP,
                email = "hogee2@hogee",
            )
        )

        // when & then
        val exception = assertThrows<MemberException> {
            memberService.saveCodeImage(member, emptyList())
        }

        assertThat(exception.message).isEqualTo("프로필이 존재하지 않습니다. 먼저 프로필을 등록해주세요.")
    }

    @DisplayName("코드 이미지 등록 시 프로필이 존재하면 코드 이미지가 등록되고 멤버 상태가 CODE_PROFILE_SURVEY로 변한다.")
    @Test
    fun saveCodeImageWithValidProfile() {
        // given
        val file1 = MockMultipartFile("file", "test1.png", "image/png", "이미지1".toByteArray())
        val file2 = MockMultipartFile("file", "test2.png", "image/png", "이미지2".toByteArray())
        val files = listOf(file1, file2)

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

        given(imageUploader.uploadFile(file1)).willReturn("https://test-bucket.s3.amazonaws.com/image1.png")
        given(imageUploader.uploadFile(file2)).willReturn("https://test-bucket.s3.amazonaws.com/image2.png")

        // when
        memberService.saveCodeImage(member, files)

        // then
        val updatedMember = memberJpaRepository.findById(member.id!!).get()
        val savedProfile = updatedMember.profile!!
        assertThat(updatedMember.memberStatus).isEqualTo(MemberStatus.CODE_PROFILE_IMAGE)
        assertThat(savedProfile.codeImage).isNotNull
        assertThat(savedProfile.getCodeImageOrThrow()).contains("https://test-bucket.s3.amazonaws.com/image1.png")
        assertThat(savedProfile.getCodeImageOrThrow()).contains("https://test-bucket.s3.amazonaws.com/image2.png")
    }

    @DisplayName("얼굴 이미지 등록 시 프로필과 코드 이미지가 존재하면 얼굴 이미지가 등록되고 멤버 상태가 PENDING으로 변한다.")
    @Test
    fun saveFaceImageWithValidProfileAndCodeImage() {
        // given
        val faceFile1 = MockMultipartFile("file", "face1.png", "image/png", "얼굴1".toByteArray())
        val faceFile2 = MockMultipartFile("file", "face2.png", "image/png", "얼굴2".toByteArray())
        val faceFile3 = MockMultipartFile("file", "face3.png", "image/png", "얼굴3".toByteArray())
        val faceFiles = listOf(faceFile1, faceFile2, faceFile3)

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

        // 코드 이미지 등록 (상태: CODE_PROFILE_IMAGE)
        val codeFile1 = MockMultipartFile("file", "test1.png", "image/png", "이미지1".toByteArray())
        val codeFile2 = MockMultipartFile("file", "test2.png", "image/png", "이미지2".toByteArray())
        val codeFiles = listOf(codeFile1, codeFile2)
        given(imageUploader.uploadFile(codeFile1)).willReturn("https://test-bucket.s3.amazonaws.com/codeImage1.png")
        given(imageUploader.uploadFile(codeFile2)).willReturn("https://test-bucket.s3.amazonaws.com/codeImage2.png")
        memberService.saveCodeImage(member, codeFiles)

        // 얼굴 이미지 mocking
        given(imageUploader.uploadFile(faceFile1)).willReturn("https://test-bucket.s3.amazonaws.com/faceImage1.png")
        given(imageUploader.uploadFile(faceFile2)).willReturn("https://test-bucket.s3.amazonaws.com/faceImage2.png")
        given(imageUploader.uploadFile(faceFile3)).willReturn("https://test-bucket.s3.amazonaws.com/faceImage3.png")

        // when
        memberService.saveFaceImage(member, faceFiles)

        // then
        val updatedMember = memberJpaRepository.findById(member.id!!).get()
        val savedProfile = updatedMember.profile!!
        assertThat(updatedMember.memberStatus).isEqualTo(MemberStatus.PENDING)
        assertThat(savedProfile.faceImage).isNotNull
        assertThat(savedProfile.getFaceImageOrThrow()).contains("https://test-bucket.s3.amazonaws.com/faceImage1.png")
        assertThat(savedProfile.getFaceImageOrThrow()).contains("https://test-bucket.s3.amazonaws.com/faceImage2.png")
        assertThat(savedProfile.getFaceImageOrThrow()).contains("https://test-bucket.s3.amazonaws.com/faceImage3.png")
    }

    
}