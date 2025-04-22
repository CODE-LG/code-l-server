package codel.config.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CodelException::class)
    fun handleCodelException(
        e: CodelException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = e.status.value(),
                path = request.requestURI,
                message = e.message,
                stackTrace = e.stackTraceToString(),
            )
        return ResponseEntity.status(e.status).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                path = request.requestURI,
                message = e.message ?: "예상치 못한 오류가 발생했습니다.",
                stackTrace = e.stackTraceToString(),
            )
        return ResponseEntity.status(500).body(response)
    }

    data class ErrorResponse(
        val timestamp: String,
        val status: Int,
        val path: String,
        val message: String,
        val stackTrace: String,
    )
}
