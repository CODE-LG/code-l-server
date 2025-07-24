package codel.block.presentation

import codel.block.business.BlockService
import codel.block.presentation.request.BlockMemberRequest
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/block")
class BlockController(
    val blockService: BlockService
) {

    @PostMapping
    fun blockMember(
        @LoginMember blocker : Member,
        @RequestBody blockMemberRequest : BlockMemberRequest
    ) : ResponseEntity<Unit>{
        blockService.blockMember(blocker, blockMemberRequest.blockedMemberId)
        return ResponseEntity.ok().build()
    }
}