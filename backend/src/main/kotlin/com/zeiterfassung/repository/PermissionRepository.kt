package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.PermissionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface PermissionRepository : JpaRepository<PermissionEntity, UUID> {
    fun findByName(name: String): Optional<PermissionEntity>

    fun findByNameIn(names: Collection<String>): List<PermissionEntity>
}
