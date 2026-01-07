package codel.kpi.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class KpiException(
    status: HttpStatus,
    message: String
) : CodelException(status, message)
