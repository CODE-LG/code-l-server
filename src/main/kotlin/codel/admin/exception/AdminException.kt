package codel.admin.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class AdminException(
    status: HttpStatus,
    message: String,
) : CodelException(status, message)
