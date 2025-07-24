package codel.report.infrastructure

import codel.report.domain.Report
import org.springframework.data.jpa.repository.JpaRepository

interface ReportJpaRepository : JpaRepository<Report, Long> {
}