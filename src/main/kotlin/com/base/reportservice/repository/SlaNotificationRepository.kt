package com.base.reportservice.repository

import com.base.reportservice.domain.SlaNotification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SlaNotificationRepository : JpaRepository<SlaNotification, UUID> {

    fun existsByAppealIdAndNotificationType(appealId: UUID, notificationType: String): Boolean
}
