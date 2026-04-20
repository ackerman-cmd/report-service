package com.base.reportservice.scheduler

import com.base.reportservice.service.SlaNotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SlaMonitoringScheduler(
    private val slaNotificationService: SlaNotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Проверяет SLA всех активных обращений.
     * Cron задаётся через `app.report.sla-monitoring-cron`.
     * По умолчанию — каждый час.
     *
     * Логика:
     * - если израсходовано ≥ [slaWarningThresholdPercent]% срока → шлёт WARNING клиентскому менеджеру
     * - если срок истёк (≥100%) → шлёт BREACH
     * - каждый тип уведомления для одного обращения отправляется ровно один раз
     */
    @Scheduled(cron = "\${app.report.sla-monitoring-cron:0 0 * * * *}")
    fun checkSla() {
        log.debug("SLA monitoring check started")
        slaNotificationService.checkAndNotify()
    }
}
