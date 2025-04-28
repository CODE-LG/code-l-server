package codel.config.exception

import codel.config.Loggable
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestControllerAdvice
class GlobalExceptionHandler : Loggable {
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
        log.info {
            """
            ❗ [CodelException 발생]
            - 예외명: ${e::class.simpleName}
            - 메시지: ${e.message}
            - 상태코드: ${e.status}
            - 요청 URI: ${request.requestURI}
            - 요청 방식: ${request.method}
            - 스택트레이스:
            ${e.stackTraceToString()}
            """.trimIndent()
        }
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
        log.warn {
            """
            💥 [Unhandled Exception]
            - 예외명: ${e::class.simpleName}
            - 메시지: ${e.message}
            - 요청 URI: ${request.requestURI}
            - 요청 방식: ${request.method}
            - 스택트레이스:
            ${e.stackTraceToString()}
            """.trimIndent()
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    @ExceptionHandler(Error::class)
    fun handleFatalError(
        e: Error,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                path = request.requestURI,
                message = e.message ?: "치명적인 오류가 발생했습니다.",
                stackTrace = e.stackTraceToString(),
            )

        log.error {
            """
            💩 [Fatal Error 발생]
            - 예외명: ${e::class.simpleName}
            - 메시지: ${e.message}
            - 요청 URI: ${request.requestURI}
            - 요청 방식: ${request.method}
            - 스택트레이스:
            ${e.stackTraceToString()}
            """.trimIndent()
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    data class ErrorResponse(
        val timestamp: String,
        val status: Int,
        val path: String,
        val message: String,
        val stackTrace: String,
    )
}
