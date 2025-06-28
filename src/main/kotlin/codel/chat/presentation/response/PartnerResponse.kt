package codel.chat.presentation.response

import codel.member.domain.Member

data class PartnerResponse(
    val memberId: Long,
    val codeImage: String,
    val name: String,
) {
    companion object {
        fun of(partner: Member): PartnerResponse =
            PartnerResponse(
                memberId = partner.getIdOrThrow(),
                codeImage = partner.getProfileOrThrow().getFirstCodeImage(),
                name = partner.getProfileOrThrow().codeName,
            )
    }
}
