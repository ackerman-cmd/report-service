package com.base.reportservice.integration.armsupport

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "assignment_groups", schema = "arm_support")
class AssignmentGroupView(
    @Id
    val id: UUID,
    val name: String,
    val description: String?,
    val mailboxEmail: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
