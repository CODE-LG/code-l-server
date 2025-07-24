package codel.report.exception

import codel.config.exception.CodelException
import org.springframework.http.HttpStatus

class ReportException(
    status : HttpStatus,
    message : String
) : CodelException(status, message)