package com.base.reportservice.scheduler

import com.base.reportservice.service.ReportEmailService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReportScheduler(
    private val reportEmailService: ReportEmailService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Cron задаётся через `app.report.cron`.
     * По умолчанию: каждый день в 08:00.
     */
    @Scheduled(cron = "\${app.report.cron:0 0 8 * * *}")
    fun sendDailyReport() {
        log.info("Scheduled daily report triggered")
        reportEmailService.generateAndSend()
    }
}
