package codel.member.infrastructure

import codel.config.TestFixture
import codel.member.domain.MemberStatus
import codel.member.infrastructure.entity.MemberEntity
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException

@SpringBootTest
class MemberJpaRepositoryTest : TestFixture() {
    @DisplayName("중복된 멤버에 대해 유니크 제약 조건을 발생시킨다.")
    @Test
    fun saveMemberTest() {
        val newEntity =
            MemberEntity(
                oauthType = memberSignup.oauthType,
                oauthId = memberSignup.oauthId,
                memberStatus = MemberStatus.SIGNUP,
                email = "hogee@hogee",
            )
        assertThrows(DataIntegrityViolationException::class.java) {
            memberJpaRepository.save(newEntity)
        }
    }
}
