package com.base.reportservice.service

import com.base.reportservice.domain.AppealPriority
import com.base.reportservice.domain.AppealStatus
import com.base.reportservice.domain.SlaPolicy
import com.base.reportservice.integration.armsupport.AppealView
import com.base.reportservice.repository.SlaPolicyRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class AppealSlaStatus(
    val appealId: UUID,
    val subject: String,
    val priority: AppealPriority,
    val status: AppealStatus,
    val ageHours: Long,
    /** Первый ответ — просрочен, если обращение застряло в PENDING_PROCESSING дольше порога. */
    val responseBreached: Boolean,
    /** Решение — просрочено, если активное обращение существует дольше порога resolution_hours. */
    val resolutionBreached: Boolean,
    val responseDeadline: LocalDateTime?,
    val resolutionDeadline: LocalDateTime?,
)

@Service
class SlaCalculationService(
    private val slaPolicyRepository: SlaPolicyRepository,
) {
    private val terminalStatuses = setOf(AppealStatus.CLOSED, AppealStatus.SPAM)

    /**
     * Вычисляет SLA-статус для списка обращений.
     * Политики загружаются один раз — не вызывай в цикле поштучно.
     */
    fun calculate(appeals: List<AppealView>): List<AppealSlaStatus> {
        val policies: Map<AppealPriority, SlaPolicy> =
            slaPolicyRepository.findAll().associateBy { it.priority }
        val now = LocalDateTime.now()
        return appeals.map { calculateOne(it, policies, now) }
    }

    private fun calculateOne(
        appeal: AppealView,
        policies: Map<AppealPriority, SlaPolicy>,
        now: LocalDateTime,
    ): AppealSlaStatus {
        val ageHours = ChronoUnit.HOURS.between(appeal.createdAt, now)
        val isTerminal = appeal.status in terminalStatuses
        val policy = policies[appeal.priority]

        val responseBreached = !isTerminal &&
            appeal.status == AppealStatus.PENDING_PROCESSING &&
            policy != null && ageHours >= policy.responseHours

        val resolutionBreached = !isTerminal &&
            policy != null && ageHours >= policy.resolutionHours

        return AppealSlaStatus(
            appealId = appeal.id,
            subject = appeal.subject,
            priority = appeal.priority,
            status = appeal.status,
            ageHours = ageHours,
            responseBreached = responseBreached,
            resolutionBreached = resolutionBreached,
            responseDeadline = policy?.let { appeal.createdAt.plusHours(it.responseHours.toLong()) },
            resolutionDeadline = policy?.let { appeal.createdAt.plusHours(it.resolutionHours.toLong()) },
        )
    }
}
