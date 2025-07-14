package codel.signal.presentation

import codel.member.domain.Member
import codel.signal.business.SignalService
import codel.signal.presentation.request.SendSignalRequest
import codel.signal.presentation.response.SignalResponse
import codel.config.argumentresolver.LoginMember
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/signals")
class SignalController(
    private val signalService: SignalService
) {
    @PostMapping
    fun sendSignal(
        @LoginMember fromMember: Member,
        @RequestBody request: SendSignalRequest
    ): ResponseEntity<SignalResponse> {
        val signal = signalService.sendSignal(fromMember, request.toMemberId)
        return ResponseEntity.ok(SignalResponse.from(signal))
    }
}