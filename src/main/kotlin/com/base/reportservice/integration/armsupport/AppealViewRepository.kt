package com.base.reportservice.integration.armsupport

import com.base.reportservice.domain.AppealStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface AppealViewRepository : JpaRepository<AppealView, UUID> {

    fun findAllByStatusNotIn(statuses: List<AppealStatus>): List<AppealView>

    fun countByStatus(status: AppealStatus): Long

    /** Обращения, созданные за период (включительно). */
    fun findAllByCreatedAtBetween(from: LocalDateTime, to: LocalDateTime): List<AppealView>

    /** Обращения, закрытые начиная с указанного момента. */
    fun findAllByClosedAtGreaterThanEqual(from: LocalDateTime): List<AppealView>

    /** Активные обращения, созданные раньше указанного порога (для SLA-расчёта). */
    @Query(
        """
        SELECT a FROM AppealView a
        WHERE a.status NOT IN ('CLOSED', 'SPAM')
          AND a.createdAt <= :threshold
        """,
    )
    fun findActiveOlderThan(@Param("threshold") threshold: LocalDateTime): List<AppealView>
}
