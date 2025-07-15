package codel.signal.presentation

import codel.member.domain.Member
import codel.signal.business.SignalService
import codel.signal.presentation.request.SendSignalRequest
import codel.signal.presentation.response.SignalResponse
import codel.config.argumentresolver.LoginMember
import codel.member.presentation.response.MemberResponse
import codel.signal.presentation.response.ReceivedSignalResponse
import org.springframework.data.domain.Page
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

    @GetMapping("/received")
    fun getMemberReceiveSignalForMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) : ResponseEntity<Page<ReceivedSignalResponse>>{
        val signals = signalService.getReceivedSignals(me, page, size)
        return ResponseEntity.ok(signals.map { ReceivedSignalResponse.from(it)});
    }
}