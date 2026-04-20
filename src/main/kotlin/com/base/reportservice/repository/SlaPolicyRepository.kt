package com.base.reportservice.repository

import com.base.reportservice.domain.AppealPriority
import com.base.reportservice.domain.SlaPolicy
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SlaPolicyRepository : JpaRepository<SlaPolicy, UUID> {
    fun findByPriority(priority: AppealPriority): SlaPolicy?
}
