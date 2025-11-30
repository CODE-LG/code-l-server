package codel.verification.presentation

import codel.verification.business.VerificationImageService
import codel.verification.presentation.response.StandardVerificationImageResponse
import codel.verification.presentation.swagger.VerificationImageControllerSwagger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/verification")
class VerificationImageController(
    private val verificationImageService: VerificationImageService
) : VerificationImageControllerSwagger {

    @GetMapping("/standard-image")
    override fun getRandomStandardImage(): ResponseEntity<StandardVerificationImageResponse> {
        val standardImage = verificationImageService.getRandomStandardImage()
        return ResponseEntity.ok(standardImage)
    }
}
