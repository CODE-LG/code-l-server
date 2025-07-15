package codel.signal.presentation

import codel.member.domain.Member
import codel.signal.business.SignalService
import codel.signal.presentation.request.SendSignalRequest
import codel.signal.presentation.response.SignalResponse
import codel.config.argumentresolver.LoginMember
import codel.signal.presentation.response.SignalMemberResponse
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
    fun getReceiveSignalForMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) : ResponseEntity<Page<SignalMemberResponse>>{
        val signals = signalService.getReceivedSignals(me, page, size)
        return ResponseEntity.ok(signals.map { SignalMemberResponse.from(it)});
    }

    @GetMapping("/send")
    fun getSendSignalByMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) : ResponseEntity<Page<SignalMemberResponse>>{
        val signals = signalService.getSendSignalByMe(me, page, size)
        return ResponseEntity.ok(signals.map { SignalMemberResponse.from(it) })
    }

    @PostMapping("/{id}/accpet")
    fun acceptSignal(
        @LoginMember me : Member,
        @PathVariable id : Long
    ) : ResponseEntity<Unit>{
        signalService.acceptSignal(me, id)
        return ResponseEntity.ok().build<Unit>()
    }
}