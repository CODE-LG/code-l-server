package codel.signal.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class SignalException(
    status: HttpStatus,
    message: String,
) : CodelException(status, message)
