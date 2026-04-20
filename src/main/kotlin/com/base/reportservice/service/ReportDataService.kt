package com.base.reportservice.service

import com.base.reportservice.domain.AppealChannel
import com.base.reportservice.domain.AppealPriority
import com.base.reportservice.domain.AppealStatus
import com.base.reportservice.integration.armsupport.AppealViewRepository
import com.base.reportservice.integration.armsupport.AssignmentGroupViewRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

// ──────────────────────────── DTOs ────────────────────────────

data class ReportStats(
    val generatedAt: LocalDateTime,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val totalAppeals: Int,
    val byStatus: Map<AppealStatus, Int>,
    val byPriority: Map<AppealPriority, Int>,
    val byChannel: Map<AppealChannel, Int>,
    val sla: SlaBreachSummary,
    val byGroup: List<GroupStats>,
    val createdThisPeriod: Int,
    val closedThisPeriod: Int,
)

data class SlaBreachSummary(
    val totalActive: Int,
    val responseBreached: Int,
    val resolutionBreached: Int,
    /** Топ-10 обращений с нарушениями, отсортированных по возрасту убыванием. */
    val breachedItems: List<AppealSlaStatus>,
)

data class GroupStats(
    val groupId: UUID,
    val groupName: String,
    val mailboxEmail: String?,
    val totalAppeals: Int,
    val pendingAppeals: Int,
    val inProgressAppeals: Int,
    val waitingAppeals: Int,
)

// ──────────────────────────── Service ────────────────────────────

@Service
class ReportDataService(
    private val appealViewRepository: AppealViewRepository,
    private val assignmentGroupViewRepository: AssignmentGroupViewRepository,
    private val slaCalculationService: SlaCalculationService,
) {
    fun buildWeeklyStats(): ReportStats {
        val now = LocalDateTime.now()
        val periodStart = now.minusDays(7)

        val allAppeals = appealViewRepository.findAll()
        val activeAppeals = allAppeals.filter { it.status !in listOf(AppealStatus.CLOSED, AppealStatus.SPAM) }

        val createdThisPeriod = allAppeals.count { it.createdAt >= periodStart }
        val closedThisPeriod = allAppeals.count { it.closedAt != null && it.closedAt!! >= periodStart }

        // SLA
        val slaStatuses = slaCalculationService.calculate(activeAppeals)
        val responseBreached = slaStatuses.filter { it.responseBreached }
        val resolutionBreached = slaStatuses.filter { it.resolutionBreached }
        val allBreached = (responseBreached + resolutionBreached)
            .distinctBy { it.appealId }
            .sortedByDescending { it.ageHours }
            .take(10)

        // Per-group stats
        val groups = assignmentGroupViewRepository.findAll()
        val byGroup = groups
            .map { group ->
                val ga = allAppeals.filter { it.assignmentGroupId == group.id }
                GroupStats(
                    groupId = group.id,
                    groupName = group.name,
                    mailboxEmail = group.mailboxEmail,
                    totalAppeals = ga.size,
                    pendingAppeals = ga.count { it.status == AppealStatus.PENDING_PROCESSING },
                    inProgressAppeals = ga.count { it.status == AppealStatus.IN_PROGRESS },
                    waitingAppeals = ga.count { it.status == AppealStatus.WAITING_CLIENT_RESPONSE },
                )
            }
            .filter { it.totalAppeals > 0 }
            .sortedByDescending { it.totalAppeals }

        return ReportStats(
            generatedAt = now,
            periodStart = periodStart,
            periodEnd = now,
            totalAppeals = allAppeals.size,
            byStatus = AppealStatus.entries.associateWith { s -> allAppeals.count { it.status == s } },
            byPriority = AppealPriority.entries.associateWith { p -> allAppeals.count { it.priority == p } },
            byChannel = AppealChannel.entries.associateWith { c -> allAppeals.count { it.channel == c } },
            sla = SlaBreachSummary(
                totalActive = activeAppeals.size,
                responseBreached = responseBreached.size,
                resolutionBreached = resolutionBreached.size,
                breachedItems = allBreached,
            ),
            byGroup = byGroup,
            createdThisPeriod = createdThisPeriod,
            closedThisPeriod = closedThisPeriod,
        )
    }
}
