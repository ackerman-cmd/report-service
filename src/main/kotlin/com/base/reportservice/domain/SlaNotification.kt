package com.base.reportservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "sla_notifications", schema = "report_service")
class SlaNotification(
    @Id
    val id: UUID = UUID.randomUUID(),
    /** ID обращения из arm_support.appeals. */
    val appealId: UUID,
    /** SLA_WARNING — предупреждение, SLA_BREACH — нарушение. */
    val notificationType: String,
    val recipientEmail: String,
    val sentAt: LocalDateTime = LocalDateTime.now(),
    @Column(length = 512)
    val appealSubject: String?,
    val appealPriority: String?,
    val slaDeadline: LocalDateTime?,
    val ageHours: Int?,
)
