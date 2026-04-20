package com.base.reportservice.integration.armsupport

import com.base.reportservice.domain.AppealChannel
import com.base.reportservice.domain.AppealPriority
import com.base.reportservice.domain.AppealStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDateTime
import java.util.UUID

/**
 * Read-only JPA view onto arm_support.appeals.
 * Report-service shares the same PostgreSQL instance and reads this schema directly.
 */
@Entity
@Immutable
@Table(name = "appeals", schema = "arm_support")
class AppealView(
    @Id
    val id: UUID,
    val subject: String,
    @Enumerated(EnumType.STRING)
    val channel: AppealChannel,
    @Enumerated(EnumType.STRING)
    val status: AppealStatus,
    @Enumerated(EnumType.STRING)
    val priority: AppealPriority,
    @Column(name = "organization_id")
    val organizationId: UUID?,
    @Column(name = "assigned_operator_id")
    val assignedOperatorId: UUID?,
    @Column(name = "assignment_group_id")
    val assignmentGroupId: UUID?,
    @Column(name = "skill_group_id")
    val skillGroupId: UUID?,
    @Column(name = "created_by_id")
    val createdById: UUID,
    @Column(name = "contact_email")
    val contactEmail: String?,
    val closedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
