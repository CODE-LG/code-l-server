package codel.block.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class BlockException(
    status : HttpStatus,
    message : String,
) : CodelException(status, message)