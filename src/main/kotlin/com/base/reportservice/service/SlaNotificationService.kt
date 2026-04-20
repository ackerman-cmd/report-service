package com.base.reportservice.service

import com.base.reportservice.config.ReportProperties
import com.base.reportservice.domain.AppealPriority
import com.base.reportservice.domain.AppealStatus
import com.base.reportservice.domain.SlaNotification
import com.base.reportservice.domain.SlaPolicy
import com.base.reportservice.integration.armsupport.AppealViewRepository
import com.base.reportservice.integration.email.EmailServiceClient
import com.base.reportservice.repository.SlaNotificationRepository
import com.base.reportservice.repository.SlaPolicyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val TYPE_WARNING = "SLA_WARNING"
private const val TYPE_BREACH = "SLA_BREACH"

@Service
class SlaNotificationService(
    private val appealViewRepository: AppealViewRepository,
    private val slaPolicyRepository: SlaPolicyRepository,
    private val slaNotificationRepository: SlaNotificationRepository,
    private val emailServiceClient: EmailServiceClient,
    private val reportProperties: ReportProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun checkAndNotify() {
        val recipient = reportProperties.clientManagerEmail
        if (recipient.isBlank()) {
            log.debug("clientManagerEmail not configured — skipping SLA monitoring")
            return
        }

        val policies = slaPolicyRepository.findAll().associateBy { it.priority }
        val terminalStatuses = listOf(AppealStatus.CLOSED, AppealStatus.SPAM)
        val activeAppeals = appealViewRepository.findAllByStatusNotIn(terminalStatuses)

        if (activeAppeals.isEmpty()) return

        val now = LocalDateTime.now()
        var warningsSent = 0
        var breachesSent = 0

        for (appeal in activeAppeals) {
            val policy = policies[appeal.priority] ?: continue
            val ageHours = ChronoUnit.HOURS.between(appeal.createdAt, now)

            // ── Нарушение SLA (resolution) ─────────────────────────────────
            if (ageHours >= policy.resolutionHours) {
                val alreadySent = slaNotificationRepository
                    .existsByAppealIdAndNotificationType(appeal.id, TYPE_BREACH)
                if (!alreadySent) {
                    val deadline = appeal.createdAt.plusHours(policy.resolutionHours.toLong())
                    val overdueHours = ageHours - policy.resolutionHours
                    sendNotification(
                        recipient = recipient,
                        type = TYPE_BREACH,
                        appealId = appeal.id,
                        subject = appeal.subject,
                        priority = appeal.priority,
                        status = appeal.status,
                        deadline = deadline,
                        ageHours = ageHours,
                        overdueHours = overdueHours,
                        timeLeftHours = null,
                        policy = policy,
                    )
                    breachesSent++
                }
                continue // breach важнее warning — не дублируем
            }

            // ── Предупреждение о приближении SLA (resolution) ──────────────
            val warningThresholdHours =
                policy.resolutionHours * reportProperties.slaWarningThresholdPercent / 100.0
            if (ageHours >= warningThresholdHours) {
                val alreadySent = slaNotificationRepository
                    .existsByAppealIdAndNotificationType(appeal.id, TYPE_WARNING)
                if (!alreadySent) {
                    val deadline = appeal.createdAt.plusHours(policy.resolutionHours.toLong())
                    val timeLeftHours = policy.resolutionHours - ageHours
                    sendNotification(
                        recipient = recipient,
                        type = TYPE_WARNING,
                        appealId = appeal.id,
                        subject = appeal.subject,
                        priority = appeal.priority,
                        status = appeal.status,
                        deadline = deadline,
                        ageHours = ageHours,
                        overdueHours = null,
                        timeLeftHours = timeLeftHours,
                        policy = policy,
                    )
                    warningsSent++
                }
            }
        }

        if (warningsSent > 0 || breachesSent > 0) {
            log.info("SLA monitoring: {} warnings, {} breach notifications sent", warningsSent, breachesSent)
        }
    }

    private fun sendNotification(
        recipient: String,
        type: String,
        appealId: java.util.UUID,
        subject: String,
        priority: AppealPriority,
        status: AppealStatus,
        deadline: LocalDateTime,
        ageHours: Long,
        overdueHours: Long?,
        timeLeftHours: Long?,
        policy: SlaPolicy,
    ) {
        val isBreach = type == TYPE_BREACH
        val emailSubject = if (isBreach) {
            "[SLA НАРУШЕНИЕ] Обращение «${subject.take(60)}» — срок истёк"
        } else {
            "[SLA] Обращение «${subject.take(60)}» — срок истекает скоро"
        }

        val htmlBody = buildNotificationHtml(
            isBreach = isBreach,
            appealSubject = subject,
            priority = priority,
            status = status,
            deadline = deadline,
            ageHours = ageHours,
            overdueHours = overdueHours,
            timeLeftHours = timeLeftHours,
            resolutionHours = policy.resolutionHours,
        )

        try {
            emailServiceClient.sendEmail(
                to = listOf(recipient),
                subject = emailSubject,
                htmlBody = htmlBody,
            )

            slaNotificationRepository.save(
                SlaNotification(
                    appealId = appealId,
                    notificationType = type,
                    recipientEmail = recipient,
                    appealSubject = subject.take(512),
                    appealPriority = priority.name,
                    slaDeadline = deadline,
                    ageHours = ageHours.toInt().coerceAtMost(Int.MAX_VALUE),
                ),
            )
        } catch (ex: Exception) {
            log.error("Failed to send SLA {} notification for appeal {}: {}", type, appealId, ex.message)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML-шаблон уведомления
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildNotificationHtml(
        isBreach: Boolean,
        appealSubject: String,
        priority: AppealPriority,
        status: AppealStatus,
        deadline: LocalDateTime,
        ageHours: Long,
        overdueHours: Long?,
        timeLeftHours: Long?,
        resolutionHours: Int,
    ): String {
        val accentColor = if (isBreach) "#c62828" else "#e65100"
        val bgBanner = if (isBreach) "#ffebee" else "#fff3e0"
        val badgeText = if (isBreach) "НАРУШЕНИЕ SLA" else "ПРЕДУПРЕЖДЕНИЕ SLA"
        val timeInfo = when {
            isBreach && overdueHours != null ->
                "<span style='color:#c62828;font-weight:700;'>Просрочено на $overdueHours ч</span>"
            timeLeftHours != null ->
                "Осталось: <span style='color:#e65100;font-weight:700;'>$timeLeftHours ч</span>"
            else -> "—"
        }
        val priorityColor = when (priority) {
            AppealPriority.LOW -> "#43a047"
            AppealPriority.MEDIUM -> "#1e88e5"
            AppealPriority.HIGH -> "#fb8c00"
            AppealPriority.CRITICAL -> "#e53935"
        }
        val priorityLabel = when (priority) {
            AppealPriority.LOW -> "Низкий"
            AppealPriority.MEDIUM -> "Средний"
            AppealPriority.HIGH -> "Высокий"
            AppealPriority.CRITICAL -> "Критический"
        }
        val statusLabel = when (status) {
            AppealStatus.PENDING_PROCESSING -> "Ожидает обработки"
            AppealStatus.IN_PROGRESS -> "В работе"
            AppealStatus.WAITING_CLIENT_RESPONSE -> "Ожидает ответа клиента"
            AppealStatus.CLOSED -> "Закрыто"
            AppealStatus.SPAM -> "Спам"
        }

        return """
<!DOCTYPE html>
<html lang="ru">
<head><meta charset="UTF-8"></head>
<body style="margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background:#f0f2f5;color:#222;">
<table width="100%" cellpadding="0" cellspacing="0">
<tr><td align="center" style="padding:32px 16px;">

  <table width="560" cellpadding="0" cellspacing="0"
         style="background:#fff;border-radius:10px;box-shadow:0 2px 12px rgba(0,0,0,.12);overflow:hidden;">

    <!-- Цветная шапка -->
    <tr>
      <td style="background:$accentColor;padding:24px 32px;">
        <span style="display:inline-block;background:rgba(255,255,255,.2);color:#fff;
                     font-size:11px;font-weight:700;letter-spacing:1px;
                     padding:4px 10px;border-radius:3px;">$badgeText</span>
        <h2 style="margin:12px 0 0;color:#fff;font-size:18px;font-weight:700;line-height:1.4;">
          Обращение требует ${if (isBreach) "немедленного" else "вашего"} внимания
        </h2>
      </td>
    </tr>

    <!-- Тело -->
    <tr>
      <td style="padding:28px 32px;">

        <!-- Банер с временем -->
        <div style="background:$bgBanner;border-left:4px solid $accentColor;
                    border-radius:4px;padding:14px 18px;margin-bottom:24px;">
          <div style="font-size:13px;color:#555;">Срок решения SLA: <strong>${deadline.format(dateTimeFmt)}</strong></div>
          <div style="font-size:13px;color:#555;margin-top:4px;">Возраст обращения: <strong>${ageHours} ч</strong> из ${resolutionHours} ч</div>
          <div style="font-size:14px;margin-top:8px;">$timeInfo</div>
        </div>

        <!-- Детали обращения -->
        <table width="100%" cellpadding="0" cellspacing="0"
               style="border-collapse:collapse;font-size:13px;">
          <tr>
            <td style="padding:10px 0;border-bottom:1px solid #f0f0f0;color:#78909c;width:140px;">Тема обращения</td>
            <td style="padding:10px 0;border-bottom:1px solid #f0f0f0;font-weight:600;">$appealSubject</td>
          </tr>
          <tr>
            <td style="padding:10px 0;border-bottom:1px solid #f0f0f0;color:#78909c;">Приоритет</td>
            <td style="padding:10px 0;border-bottom:1px solid #f0f0f0;">
              <span style="background:$priorityColor;color:#fff;padding:3px 10px;
                           border-radius:3px;font-size:12px;font-weight:600;">$priorityLabel</span>
            </td>
          </tr>
          <tr>
            <td style="padding:10px 0;border-bottom:1px solid #f0f0f0;color:#78909c;">Текущий статус</td>
            <td style="padding:10px 0;border-bottom:1px solid #f0f0f0;">$statusLabel</td>
          </tr>
          <tr>
            <td style="padding:10px 0;color:#78909c;">Дедлайн решения</td>
            <td style="padding:10px 0;font-weight:600;color:$accentColor;">${deadline.format(dateTimeFmt)}</td>
          </tr>
        </table>

        <p style="margin:24px 0 0;font-size:13px;color:#546e7a;line-height:1.6;">
          ${if (isBreach)
              "Срок SLA по данному обращению истёк. Необходимо срочно принять меры для его решения."
          else
              "Срок SLA по данному обращению истекает. Пожалуйста, проконтролируйте ход работы и при необходимости эскалируйте."}
        </p>

      </td>
    </tr>

    <!-- Футер -->
    <tr>
      <td style="background:#f8f9fa;padding:16px 32px;border-top:1px solid #e0e0e0;">
        <p style="margin:0;font-size:11px;color:#90a4ae;text-align:center;">
          Автоматическое уведомление системы поддержки. Не отвечайте на это письмо.
        </p>
      </td>
    </tr>

  </table>
</td></tr>
</table>
</body>
</html>
        """.trimIndent()
    }
}
