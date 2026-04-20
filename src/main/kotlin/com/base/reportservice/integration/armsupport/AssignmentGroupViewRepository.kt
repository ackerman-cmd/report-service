package com.base.reportservice.integration.armsupport

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AssignmentGroupViewRepository : JpaRepository<AssignmentGroupView, UUID>
