package codel.notification.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class NotificationException(
    status: HttpStatus,
    message: String,
) : CodelException(status, message)
