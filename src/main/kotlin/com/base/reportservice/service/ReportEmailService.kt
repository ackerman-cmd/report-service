package com.base.reportservice.service

import com.base.reportservice.config.ReportProperties
import com.base.reportservice.domain.ReportHistory
import com.base.reportservice.event.DailyReportEvent
import com.base.reportservice.repository.ReportHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ReportEmailService(
    private val reportDataService: ReportDataService,
    private val reportContentService: ReportContentService,
    private val pdfGenerationService: PdfGenerationService,
    private val s3StorageService: S3StorageService,
    private val reportEventPublisher: ReportEventPublisher,
    private val reportHistoryRepository: ReportHistoryRepository,
    private val reportProperties: ReportProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val fileNameFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun generateAndSend() {
        if (reportProperties.adminEmails.isEmpty()) {
            log.warn("No admin emails configured — skipping report")
            return
        }

        log.info("Generating daily report for {} recipients", reportProperties.adminEmails.size)
        val stats = reportDataService.buildWeeklyStats()
        val htmlBody = reportContentService.buildHtml(stats)
        val subject = "Ежедневный отчёт по обращениям: ${stats.periodStart.format(dateFmt)} — ${stats.periodEnd.format(dateFmt)}"

        val history = ReportHistory(
            reportType = "DAILY",
            recipientEmails = reportProperties.adminEmails.joinToString(","),
            periodStart = stats.periodStart,
            periodEnd = stats.periodEnd,
            totalAppeals = stats.totalAppeals,
            slaBreachesCount = stats.sla.responseBreached + stats.sla.resolutionBreached,
        )
        reportHistoryRepository.save(history)

        try {
            val pdfBytes = pdfGenerationService.generatePdf(htmlBody)
            val s3Key = "${stats.periodEnd.format(fileNameFmt)}-${history.id}.pdf"
            val s3Url = s3StorageService.upload(s3Key, pdfBytes, "application/pdf")

            history.s3Key = s3Key
            history.s3Url = s3Url
            history.fileSizeBytes = pdfBytes.size.toLong()

            val event = DailyReportEvent(
                reportId = history.id.toString(),
                subject = subject,
                recipientEmails = reportProperties.adminEmails,
                htmlBody = htmlBody,
                s3Url = s3Url,
                periodStart = stats.periodStart.format(dateFmt),
                periodEnd = stats.periodEnd.format(dateFmt),
                totalAppeals = stats.totalAppeals,
                slaBreachesCount = stats.sla.responseBreached + stats.sla.resolutionBreached,
            )
            reportEventPublisher.publishDailyReport(event)

            history.status = "SENT"
            history.sentAt = LocalDateTime.now()
            log.info("Daily report event published for {} recipients", reportProperties.adminEmails.size)
        } catch (ex: Exception) {
            history.status = "FAILED"
            history.errorMessage = ex.message?.take(1000)
            log.error("Failed to generate/send daily report", ex)
        } finally {
            reportHistoryRepository.save(history)
        }
    }
}
