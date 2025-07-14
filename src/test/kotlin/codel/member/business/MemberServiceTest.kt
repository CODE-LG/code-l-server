package codel.member.business

import codel.config.TestFixture
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
import org.mockito.BDDMockito.given

@SpringBootTest
class MemberServiceTest : TestFixture() {
    @Autowired
    lateinit var memberService: MemberService

    @Autowired
    override lateinit var memberJpaRepository: MemberJpaRepository

    @Autowired
    override lateinit var profileJpaRepository: ProfileJpaRepository

    @MockBean
    lateinit var imageUploader: ImageUploader

    @DisplayName("회원가입 시 추가된 프로필일 경우 멤버상태가 CODE_SURVEY로 상태가 변경된다")
    @Test
    fun changeMemberStatusByInsertProfileAtSignup() {
        // given
        val member = memberSignup
        val profile = createProfile()

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
        val member = memberJpaRepository.findById(memberCodeSurvey.id!!).get()
        val updatedProfile = createProfile(codeName = "updateName").apply {
            age = 22
            job = "운동선수"
            alcohol = "자주"
            smoke = "비흡연"
            hobby = "독서"
            style = "활발함"
            bigCity = "서울"
            smallCity = "송파구"
            mbti = "ISTP"
            introduce = "연락 주세요"
        }

        // when
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
        val member = memberSignup // memberSignup은 profile이 없는 상태

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
        val member = memberCodeSurvey
        val file1 = createMockFile("test1.png", "이미지1")
        val file2 = createMockFile("test2.png", "이미지2")
        val files = listOf(file1, file2)
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
        val member = memberCodeProfileImage // profile+codeImage가 이미 연결된 상태
        val faceFile1 = createMockFile("face1.png", "얼굴1")
        val faceFile2 = createMockFile("face2.png", "얼굴2")
        val faceFile3 = createMockFile("face3.png", "얼굴3")
        val faceFiles = listOf(faceFile1, faceFile2, faceFile3)
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


    @DisplayName("멤버 상태가 PENDING인 상황에서 관리자가 거절하면 상태가 REJECT으로 변경된다.")
    @Test
    fun rejectMemberProfile(){
        //given
        val member = memberPending
        val reason = "이미지를 다시 업로드 부탁드립니다."
        //when
        memberService.rejectMember(member.id!!, reason)

        //then
        val rejectMember = memberJpaRepository.findById(member.id!!).get()
        assertThat(rejectMember.memberStatus).isEqualTo(MemberStatus.REJECT)
    }

    @DisplayName("멤버 상태가 PENDING인 상황에서 관리자가 승인하면 상태가 DONE으로 변경된다.")
    @Test
    fun approveMemberProfile(){
        //given
        val member = memberPending
        val reason = "이미지를 다시 업로드 부탁드립니다."
        //when
        memberService.approveMember(member.id!!)

        //then
        val rejectMember = memberJpaRepository.findById(member.id!!).get()
        assertThat(rejectMember.memberStatus).isEqualTo(MemberStatus.DONE)
    }
}