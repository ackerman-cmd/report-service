package com.base.reportservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "report_history", schema = "report_service")
class ReportHistory(
    @Id
    val id: UUID = UUID.randomUUID(),
    val reportType: String,
    @Column(columnDefinition = "TEXT")
    val recipientEmails: String,
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    var sentAt: LocalDateTime? = null,
    var status: String = "PENDING",
    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,
    var periodStart: LocalDateTime? = null,
    var periodEnd: LocalDateTime? = null,
    var totalAppeals: Int? = null,
    var slaBreachesCount: Int? = null,
    var s3Key: String? = null,
    var s3Url: String? = null,
    var fileSizeBytes: Long? = null,
)
