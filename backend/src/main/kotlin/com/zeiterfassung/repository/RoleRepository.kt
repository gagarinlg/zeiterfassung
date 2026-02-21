package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface RoleRepository : JpaRepository<RoleEntity, UUID> {
    fun findByName(name: String): Optional<RoleEntity>

    fun findByNameIn(names: Collection<String>): List<RoleEntity>
}
