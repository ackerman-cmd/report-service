package com.base.reportservice.repository

import com.base.reportservice.domain.ReportHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReportHistoryRepository : JpaRepository<ReportHistory, UUID>
