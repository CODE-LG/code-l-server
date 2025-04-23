package codel.admin.presentation.request

import codel.admin.domain.ValidateCode
import codel.member.domain.OauthType

class ValidateRequest(
    val targetOauthType: OauthType,
    val targetOauthId: String,
    val validateCode: ValidateCode,
)
