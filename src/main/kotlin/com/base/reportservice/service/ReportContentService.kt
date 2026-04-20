package com.base.reportservice.service

import com.base.reportservice.domain.AppealPriority
import com.base.reportservice.domain.AppealStatus
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class ReportContentService {

    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun buildHtml(stats: ReportStats): String {
        val totalBreaches = stats.sla.responseBreached + stats.sla.resolutionBreached
        return """
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Отчет по обращениям</title>
</head>
<body style="margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background:#f0f2f5;color:#222;">

<table width="100%" cellpadding="0" cellspacing="0">
<tr><td align="center" style="padding:32px 16px;">

  <table width="680" cellpadding="0" cellspacing="0"
         style="background:#fff;border-radius:10px;box-shadow:0 2px 12px rgba(0,0,0,.12);overflow:hidden;">

    <!-- ───── HEADER ───── -->
    <tr>
      <td style="background:linear-gradient(135deg,#1565c0,#1e88e5);padding:32px 40px;">
        <h1 style="margin:0;color:#fff;font-size:22px;font-weight:700;letter-spacing:.3px;">
          Ежедневный отчёт по обращениям
        </h1>
        <p style="margin:8px 0 0;color:#bbdefb;font-size:13px;">
          Период: <strong>${stats.periodStart.format(dateFmt)} — ${stats.periodEnd.format(dateFmt)}</strong>
        </p>
        <p style="margin:4px 0 0;color:#bbdefb;font-size:12px;">
          Сформирован: ${stats.generatedAt.format(dateTimeFmt)}
        </p>
      </td>
    </tr>

    <!-- ───── BODY ───── -->
    <tr>
      <td style="padding:32px 40px;">

        <!-- Карточки-сводка -->
        <table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:32px;">
          <tr>
            ${summaryCard("Всего обращений", stats.totalAppeals, "#1565c0")}
            ${summaryCard("Создано за период", stats.createdThisPeriod, "#2e7d32")}
            ${summaryCard("Закрыто за период", stats.closedThisPeriod, "#00796b")}
            ${summaryCard("Нарушений SLA", totalBreaches, if (totalBreaches > 0) "#c62828" else "#546e7a")}
          </tr>
        </table>

        <!-- Статусы -->
        ${sectionTitle("Обращения по статусу")}
        <table width="100%" cellpadding="0" cellspacing="0"
               style="border-collapse:collapse;margin-bottom:28px;font-size:13px;">
          ${tableHead("Статус", "Количество", "Доля")}
          ${stats.byStatus.entries.joinToString("") { (s, cnt) -> statusRow(s, cnt, stats.totalAppeals) }}
        </table>

        <!-- Приоритеты -->
        ${sectionTitle("Распределение по приоритетам")}
        <table width="100%" cellpadding="0" cellspacing="0"
               style="border-collapse:collapse;margin-bottom:28px;font-size:13px;">
          ${tableHead("Приоритет", "Количество")}
          ${stats.byPriority.entries.joinToString("") { (p, cnt) -> priorityRow(p, cnt) }}
        </table>

        <!-- Каналы -->
        ${sectionTitle("Каналы обращений")}
        <table width="100%" cellpadding="0" cellspacing="0"
               style="border-collapse:collapse;margin-bottom:28px;font-size:13px;">
          ${tableHead("Канал", "Количество")}
          ${stats.byChannel.entries.filter { it.value > 0 }.joinToString("") { (ch, cnt) ->
            simpleRow(channelLabel(ch.name), cnt.toString(), "#546e7a")
          }}
        </table>

        <!-- SLA -->
        ${slaSection(stats)}

        <!-- Группы -->
        ${if (stats.byGroup.isNotEmpty()) groupSection(stats) else ""}

      </td>
    </tr>

    <!-- ───── FOOTER ───── -->
    <tr>
      <td style="background:#f8f9fa;padding:20px 40px;border-top:1px solid #e0e0e0;">
        <p style="margin:0;font-size:11px;color:#90a4ae;text-align:center;">
          Письмо сформировано автоматически системой поддержки. Не отвечайте на него.
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

    // ──────────────────────────────────────────────────────────────
    // Строительные блоки шаблона
    // ──────────────────────────────────────────────────────────────

    private fun summaryCard(label: String, value: Int, color: String) = """
        <td width="25%" style="padding:0 5px;">
          <div style="background:#f5f7fa;border-left:4px solid $color;
                      border-radius:5px;padding:14px 12px;text-align:center;">
            <div style="font-size:28px;font-weight:700;color:$color;">$value</div>
            <div style="font-size:11px;color:#607d8b;margin-top:5px;line-height:1.3;">$label</div>
          </div>
        </td>
    """.trimIndent()

    private fun sectionTitle(title: String) = """
        <h3 style="margin:0 0 10px;font-size:14px;font-weight:700;color:#1565c0;
                   border-bottom:2px solid #e3f2fd;padding-bottom:6px;">$title</h3>
    """.trimIndent()

    private fun tableHead(vararg cols: String): String {
        val cells = cols.mapIndexed { i, c ->
            val align = if (i == 0) "left" else "right"
            """<th style="padding:9px 12px;background:#e3f2fd;color:#1565c0;
                          font-size:12px;font-weight:700;text-align:$align;
                          border-bottom:2px solid #bbdefb;">$c</th>"""
        }.joinToString("")
        return "<tr>$cells</tr>"
    }

    private fun statusRow(status: AppealStatus, count: Int, total: Int): String {
        val pct = if (total > 0) "%.1f%%".format(count * 100.0 / total) else "—"
        val (label, color) = statusInfo(status)
        return """
            <tr>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;">
                <span style="display:inline-block;width:9px;height:9px;border-radius:50%;
                             background:$color;margin-right:7px;vertical-align:middle;"></span>$label
              </td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;text-align:right;
                         font-weight:600;">$count</td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;text-align:right;
                         color:#78909c;">$pct</td>
            </tr>
        """.trimIndent()
    }

    private fun priorityRow(priority: AppealPriority, count: Int): String {
        val color = priorityColor(priority)
        return """
            <tr>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;">
                <span style="display:inline-block;background:$color;color:#fff;
                             padding:2px 8px;border-radius:3px;font-size:11px;
                             font-weight:600;">${priorityLabel(priority)}</span>
              </td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;
                         text-align:right;font-weight:600;">$count</td>
            </tr>
        """.trimIndent()
    }

    private fun simpleRow(label: String, value: String, dotColor: String) = """
        <tr>
          <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;">
            <span style="display:inline-block;width:9px;height:9px;border-radius:50%;
                         background:$dotColor;margin-right:7px;vertical-align:middle;"></span>$label
          </td>
          <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;
                     text-align:right;font-weight:600;">$value</td>
        </tr>
    """.trimIndent()

    private fun slaSection(stats: ReportStats): String {
        val hasBreach = stats.sla.responseBreached > 0 || stats.sla.resolutionBreached > 0
        val accentColor = if (hasBreach) "#c62828" else "#2e7d32"
        val bgColor = if (hasBreach) "#ffebee" else "#e8f5e9"

        val cards = """
            <table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:16px;">
              <tr>
                <td width="33%" style="padding:0 5px 0 0;">
                  <div style="background:#f5f7fa;border-radius:5px;padding:12px;text-align:center;">
                    <div style="font-size:22px;font-weight:700;color:#1565c0;">${stats.sla.totalActive}</div>
                    <div style="font-size:11px;color:#607d8b;margin-top:4px;">Активных</div>
                  </div>
                </td>
                <td width="33%" style="padding:0 3px;">
                  <div style="background:$bgColor;border-radius:5px;padding:12px;text-align:center;">
                    <div style="font-size:22px;font-weight:700;color:$accentColor;">${stats.sla.responseBreached}</div>
                    <div style="font-size:11px;color:#607d8b;margin-top:4px;">Просрочен ответ</div>
                  </div>
                </td>
                <td width="34%" style="padding:0 0 0 5px;">
                  <div style="background:$bgColor;border-radius:5px;padding:12px;text-align:center;">
                    <div style="font-size:22px;font-weight:700;color:$accentColor;">${stats.sla.resolutionBreached}</div>
                    <div style="font-size:11px;color:#607d8b;margin-top:4px;">Просрочено решение</div>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val breachTable = if (stats.sla.breachedItems.isNotEmpty()) {
            """
            <p style="margin:16px 0 8px;font-size:13px;font-weight:600;color:#c62828;">
              Обращения с нарушением SLA (топ-10):
            </p>
            <table width="100%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;font-size:12px;margin-bottom:24px;">
              <tr style="background:#ffebee;">
                <th style="padding:8px 10px;text-align:left;color:#c62828;border-bottom:2px solid #ef9a9a;">Тема</th>
                <th style="padding:8px 10px;text-align:center;color:#c62828;border-bottom:2px solid #ef9a9a;">Приоритет</th>
                <th style="padding:8px 10px;text-align:center;color:#c62828;border-bottom:2px solid #ef9a9a;">Статус</th>
                <th style="padding:8px 10px;text-align:right;color:#c62828;border-bottom:2px solid #ef9a9a;">Возраст</th>
                <th style="padding:8px 10px;text-align:center;color:#c62828;border-bottom:2px solid #ef9a9a;">Нарушение</th>
              </tr>
              ${stats.sla.breachedItems.joinToString("") { item ->
                val (statusLabel, _) = statusInfo(item.status)
                val breachType = when {
                    item.responseBreached && item.resolutionBreached -> "Ответ + Решение"
                    item.responseBreached -> "Первый ответ"
                    else -> "Решение"
                }
                val shortSubject = item.subject.take(45).let { if (item.subject.length > 45) "$it…" else it }
                """
                <tr>
                  <td style="padding:7px 10px;border-bottom:1px solid #f0f0f0;">$shortSubject</td>
                  <td style="padding:7px 10px;border-bottom:1px solid #f0f0f0;text-align:center;">
                    <span style="background:${priorityColor(item.priority)};color:#fff;
                                 padding:2px 6px;border-radius:3px;font-size:11px;">${priorityLabel(item.priority)}</span>
                  </td>
                  <td style="padding:7px 10px;border-bottom:1px solid #f0f0f0;text-align:center;">$statusLabel</td>
                  <td style="padding:7px 10px;border-bottom:1px solid #f0f0f0;text-align:right;
                             font-weight:700;color:#c62828;">${item.ageHours}ч</td>
                  <td style="padding:7px 10px;border-bottom:1px solid #f0f0f0;text-align:center;
                             color:#c62828;font-weight:600;">$breachType</td>
                </tr>
                """.trimIndent()
              }}
            </table>
            """.trimIndent()
        } else {
            """<p style="font-size:13px;color:#2e7d32;margin:0 0 24px;">
              Нарушений SLA не зафиксировано.
            </p>""".trimIndent()
        }

        return """
            ${sectionTitle("SLA — уровень обслуживания")}
            $cards
            $breachTable
        """.trimIndent()
    }

    private fun groupSection(stats: ReportStats) = """
        ${sectionTitle("Нагрузка по группам назначения")}
        <table width="100%" cellpadding="0" cellspacing="0"
               style="border-collapse:collapse;font-size:13px;margin-bottom:28px;">
          ${tableHead("Группа", "Всего", "Ожидает", "В работе", "Ожидает клиента")}
          ${stats.byGroup.joinToString("") { g ->
            """
            <tr>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;">${g.groupName}</td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;text-align:right;font-weight:600;">${g.totalAppeals}</td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;text-align:right;color:#ef6c00;">${g.pendingAppeals}</td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;text-align:right;color:#1565c0;">${g.inProgressAppeals}</td>
              <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;text-align:right;color:#6a1b9a;">${g.waitingAppeals}</td>
            </tr>
            """.trimIndent()
          }}
        </table>
    """.trimIndent()

    // ──────────────────────────────────────────────────────────────
    // Справочники
    // ──────────────────────────────────────────────────────────────

    private fun statusInfo(status: AppealStatus): Pair<String, String> = when (status) {
        AppealStatus.PENDING_PROCESSING -> "Ожидает обработки" to "#ef6c00"
        AppealStatus.IN_PROGRESS -> "В работе" to "#1565c0"
        AppealStatus.WAITING_CLIENT_RESPONSE -> "Ожидает ответа клиента" to "#6a1b9a"
        AppealStatus.CLOSED -> "Закрыто" to "#2e7d32"
        AppealStatus.SPAM -> "Спам" to "#78909c"
    }

    private fun priorityLabel(priority: AppealPriority) = when (priority) {
        AppealPriority.LOW -> "Низкий"
        AppealPriority.MEDIUM -> "Средний"
        AppealPriority.HIGH -> "Высокий"
        AppealPriority.CRITICAL -> "Критический"
    }

    private fun priorityColor(priority: AppealPriority) = when (priority) {
        AppealPriority.LOW -> "#43a047"
        AppealPriority.MEDIUM -> "#1e88e5"
        AppealPriority.HIGH -> "#fb8c00"
        AppealPriority.CRITICAL -> "#e53935"
    }

    private fun channelLabel(channel: String) = when (channel) {
        "EMAIL" -> "Email"
        "LETTER" -> "Письмо"
        "CALL" -> "Звонок"
        "CHAT" -> "Чат"
        else -> channel
    }
}
