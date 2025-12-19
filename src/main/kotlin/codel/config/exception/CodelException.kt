package codel.config.exception

import org.springframework.http.HttpStatus

open class CodelException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)
