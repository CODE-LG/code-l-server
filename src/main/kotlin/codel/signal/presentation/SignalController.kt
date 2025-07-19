package codel.signal.presentation

import codel.member.domain.Member
import codel.signal.business.SignalService
import codel.signal.presentation.request.SendSignalRequest
import codel.signal.presentation.response.SignalResponse
import codel.config.argumentresolver.LoginMember
import codel.member.presentation.response.MemberProfileResponse
import codel.member.presentation.response.MemberResponse
import codel.signal.presentation.response.SignalMemberResponse
import codel.signal.presentation.swagger.SignalControllerSwagger
import org.springframework.boot.context.properties.bind.DefaultValue
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
        val signal = signalService.sendSignal(fromMember, request.toMemberId)
        return ResponseEntity.ok(SignalResponse.from(signal))
    }

    @GetMapping("/received")
    override fun getReceiveSignalForMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) : ResponseEntity<Page<SignalMemberResponse>>{
        val signals = signalService.getReceivedSignals(me, page, size)
        return ResponseEntity.ok(signals.map { SignalMemberResponse.from(it)});
    }

    @GetMapping("/send")
    override fun getSendSignalByMe(
        @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) : ResponseEntity<Page<SignalMemberResponse>>{
        val signals = signalService.getSendSignalByMe(me, page, size)
        return ResponseEntity.ok(signals.map { SignalMemberResponse.from(it) })
    }

    @GetMapping("/accepted")
    override fun getAcceptedSignal(
        @LoginMember me : Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size : Int
    ) : ResponseEntity<Page<SignalMemberResponse>>{
        val acceptedSignals =  signalService.getAcceptedSignals(me, page, size);
        return ResponseEntity.ok(acceptedSignals.map { SignalMemberResponse.from(it) })
    }

    @PostMapping("/{id}/accept")
    override fun acceptSignal(
        @LoginMember me : Member,
        @PathVariable id : Long
    ) : ResponseEntity<Unit>{
        signalService.acceptSignal(me, id)
        return ResponseEntity.ok().build<Unit>()
    }

    @PostMapping("/{id}/reject")
    override fun rejectSignal(
        @LoginMember me : Member,
        @PathVariable id : Long
    ) : ResponseEntity<Unit>{
        signalService.rejectSignal(me, id)
        return ResponseEntity.ok().build<Unit>()
    }

    @GetMapping("/unlocked")
    fun getUnlockedSignal(
        @LoginMember me : Member,
        @DefaultValue("0") page : Int,
        @DefaultValue("10") size : Int,
    ) : ResponseEntity<Page<MemberProfileResponse>>{
        val members = signalService.getUnlockedSignal(me, page, size);
        return ResponseEntity.ok(members.map { MemberProfileResponse.toResponse(it)})
    }
}