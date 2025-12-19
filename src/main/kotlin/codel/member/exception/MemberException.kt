package codel.member.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class MemberException(
    status: HttpStatus,
    message: String,
) : CodelException(status, message)
