package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.MemberStatus

data class SignUpStatusResponse(
    val memberId: Long,
    val currentStep: MemberStatus,
    val nextStep: MemberStatus?,
    val completedSteps: List<MemberStatus>,
    val isRegistrationComplete: Boolean
) {
    companion object {
        fun from(member: Member): SignUpStatusResponse {
            val completedSteps = getCompletedSteps(member.memberStatus)
            val nextStep = getNextStep(member.memberStatus)
            
            return SignUpStatusResponse(
                memberId = member.getIdOrThrow(),
                currentStep = member.memberStatus,
                nextStep = nextStep,
                completedSteps = completedSteps,
                isRegistrationComplete = member.memberStatus == MemberStatus.DONE
            )
        }
        
        private fun getCompletedSteps(status: MemberStatus): List<MemberStatus> {
            return when (status) {
                MemberStatus.SIGNUP -> emptyList()
                MemberStatus.PHONE_VERIFIED -> listOf(MemberStatus.PHONE_VERIFIED)
                MemberStatus.ESSENTIAL_COMPLETED -> listOf(
                    MemberStatus.PHONE_VERIFIED, MemberStatus.ESSENTIAL_COMPLETED
                )
                MemberStatus.PERSONALITY_COMPLETED -> listOf(
                    MemberStatus.PHONE_VERIFIED, MemberStatus.ESSENTIAL_COMPLETED, 
                    MemberStatus.PERSONALITY_COMPLETED
                )
                MemberStatus.HIDDEN_COMPLETED -> listOf(
                    MemberStatus.PHONE_VERIFIED, MemberStatus.ESSENTIAL_COMPLETED, 
                    MemberStatus.PERSONALITY_COMPLETED, MemberStatus.HIDDEN_COMPLETED
                )
                MemberStatus.PENDING, MemberStatus.REJECT, MemberStatus.DONE -> 
                    MemberStatus.values().filter { it != MemberStatus.SIGNUP }
            }
        }
        
        private fun getNextStep(status: MemberStatus): MemberStatus? {
            return when (status) {
                MemberStatus.SIGNUP -> MemberStatus.PHONE_VERIFIED
                MemberStatus.PHONE_VERIFIED -> MemberStatus.ESSENTIAL_COMPLETED
                MemberStatus.ESSENTIAL_COMPLETED -> MemberStatus.PERSONALITY_COMPLETED
                MemberStatus.PERSONALITY_COMPLETED -> MemberStatus.HIDDEN_COMPLETED
                MemberStatus.HIDDEN_COMPLETED -> MemberStatus.PENDING
                else -> null
            }
        }
    }
}
