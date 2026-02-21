package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.EmployeeConfigEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeConfigRepository : JpaRepository<EmployeeConfigEntity, UUID> {
    fun findByUser_Id(userId: UUID): EmployeeConfigEntity?
}
