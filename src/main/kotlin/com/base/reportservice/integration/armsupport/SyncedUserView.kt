package com.base.reportservice.integration.armsupport

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "synced_users", schema = "arm_support")
class SyncedUserView(
    @Id
    val id: UUID,
    val email: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val status: String,
    val syncedAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { username }
}
