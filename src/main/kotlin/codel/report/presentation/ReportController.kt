package codel.report.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.report.business.ReportService
import codel.report.presentation.request.ReportRequest
import codel.report.presentation.swagger.ReportControllerSwagger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/reports")
class ReportController(
    val reportService : ReportService
) : ReportControllerSwagger {

    @PostMapping
    override fun reportMember(
        @LoginMember member : Member,
        @RequestBody reportRequest : ReportRequest
    ) : ResponseEntity<Unit>{
        reportService.report(member, reportRequest.reportedId, reportRequest.reason)
        return ResponseEntity.ok().build()
    }
}