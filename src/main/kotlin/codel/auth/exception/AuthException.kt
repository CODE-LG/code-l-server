package codel.auth.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class AuthException(
    status: HttpStatus,
    message: String,
) : CodelException(status, message)
