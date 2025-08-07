package codel.signal.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.response.MemberProfileResponse
import codel.member.presentation.response.UnlockedMemberProfileResponse
import codel.signal.business.SignalService
import codel.signal.presentation.request.HideSignalRequest
import codel.signal.presentation.request.SendSignalRequest
import codel.signal.presentation.response.SignalMemberResponse
import codel.signal.presentation.response.SignalResponse
import codel.signal.presentation.swagger.SignalControllerSwagger
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/signals")
class SignalController(
    private val signalService: SignalService
) : SignalControllerSwagger {
    @PostMapping
    override fun sendSignal(
        @LoginMember fromMember: Member,
        @RequestBody request: SendSignalRequest
    ): ResponseEntity<SignalResponse> {
        val signal = signalService.sendSignal(fromMember, request.toMemberId, request.message)
        return ResponseEntity.ok(SignalResponse.fromSend(signal))
    }

    @GetMapping("/received")
    override fun getReceiveSignalForMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<SignalMemberResponse>> {
        val signals = signalService.getReceivedSignals(me, page, size)
        return ResponseEntity.ok(signals.map { SignalMemberResponse.fromReceive(it) });
    }

    @GetMapping("/send")
    override fun getSendSignalByMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<SignalMemberResponse>> {
        val signals = signalService.getSendSignalByMe(me, page, size)
        return ResponseEntity.ok(signals.map { SignalMemberResponse.fromSend(it) })
    }

    // Deprecated - 사용하게되면 수정해야함(송신자 , 수신자에 따른 상태 다르게 반환해야하기 때문에)
    @GetMapping("/approved")
    override fun getAcceptedSignal(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<SignalMemberResponse>> {
        val acceptedSignals = signalService.getAcceptedSignals(me, page, size);
        return ResponseEntity.ok(acceptedSignals.map { SignalMemberResponse.fromSend(it) })
    }

    @PostMapping("/{id}/approve")
    override fun acceptSignal(
        @LoginMember me: Member,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        signalService.acceptSignal(me, id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/reject")
    override fun rejectSignal(
        @LoginMember me: Member,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        signalService.rejectSignal(me, id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/unlocked")
    override fun getUnlockedSignal(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<UnlockedMemberProfileResponse>> {
        val unlockedMemberProfileResponses = signalService.getUnlockedSignal(me, page, size);
        return ResponseEntity.ok(unlockedMemberProfileResponses)
    }

    @PatchMapping("/{id}/hide")
    override fun hideSignal(
        @LoginMember me: Member,
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        signalService.hideSignal(me, id);
        return ResponseEntity.ok().build()
    }

    @PostMapping("/hide")
    override fun hideSignals(
        @LoginMember me : Member,
        @RequestBody hideSignalRequest : HideSignalRequest
    ): ResponseEntity<Unit> {
        signalService.hideSignals(me, hideSignalRequest.ids)
        return ResponseEntity.ok().build()
    }
}