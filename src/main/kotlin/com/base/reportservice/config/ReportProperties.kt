package com.base.reportservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.report")
data class ReportProperties(
    /** Email-адреса администраторов для еженедельного отчёта. */
    val adminEmails: List<String> = emptyList(),
    /** Cron-расписание ежедневного отчёта. */
    val cron: String = "0 0 8 * * *",
    /** Адрес отправителя (должен быть зарегистрирован в email-integration-service). */
    val fromEmail: String = "reports@system-alerts.ru",
    /** Email клиентского менеджера — получает уведомления о приближении/нарушении SLA. */
    val clientManagerEmail: String = "",
    /**
     * Порог предупреждения SLA в процентах от срока.
     * Например, 80 — уведомление шлётся когда израсходовано ≥80% отведённого времени.
     */
    val slaWarningThresholdPercent: Int = 80,
    /** Cron мониторинга SLA (по умолчанию — каждый час). */
    val slaMonitoringCron: String = "0 0 * * * *",
)

@ConfigurationProperties(prefix = "app.email-service")
data class EmailServiceProperties(
    val baseUrl: String = "http://localhost:8082",
)
