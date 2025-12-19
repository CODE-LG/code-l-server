package codel.chat.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class ChatException(
    status: HttpStatus,
    message: String,
) : CodelException(status, message)
