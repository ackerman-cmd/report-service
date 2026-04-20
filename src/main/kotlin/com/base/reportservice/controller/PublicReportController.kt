package com.base.reportservice.controller

import com.base.reportservice.domain.ReportHistory
import com.base.reportservice.repository.ReportHistoryRepository
import com.base.reportservice.service.ReportEmailService
import com.base.reportservice.service.S3StorageService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.format.DateTimeFormatter
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reports")
class PublicReportController(
    private val reportHistoryRepository: ReportHistoryRepository,
    private val reportEmailService: ReportEmailService,
    private val s3StorageService: S3StorageService,
) {
    private val fileNameFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<ReportHistoryDto> =
        reportHistoryRepository
            .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt")))
            .map { ReportHistoryDto.from(it) }

    @GetMapping("/{id}/download")
    fun download(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val report = reportHistoryRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found: $id") }

        val s3Key = report.s3Key
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "PDF not available for this report")

        val bytes = s3StorageService.download(s3Key)
        val fileName = report.periodEnd?.format(fileNameFmt)?.let { "report-$it.pdf" } ?: "report.pdf"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes)
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun generate() {
        reportEmailService.generateAndSend()
    }
}

data class ReportHistoryDto(
    val id: UUID,
    val reportType: String,
    val generatedAt: String,
    val sentAt: String?,
    val status: String,
    val periodStart: String?,
    val periodEnd: String?,
    val totalAppeals: Int?,
    val slaBreachesCount: Int?,
    val s3Url: String?,
    val fileSizeBytes: Long?,
) {
    companion object {
        private val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        fun from(r: ReportHistory) = ReportHistoryDto(
            id = r.id,
            reportType = r.reportType,
            generatedAt = r.generatedAt.format(fmt),
            sentAt = r.sentAt?.format(fmt),
            status = r.status,
            periodStart = r.periodStart?.format(fmt),
            periodEnd = r.periodEnd?.format(fmt),
            totalAppeals = r.totalAppeals,
            slaBreachesCount = r.slaBreachesCount,
            s3Url = r.s3Url,
            fileSizeBytes = r.fileSizeBytes,
        )
    }
}
