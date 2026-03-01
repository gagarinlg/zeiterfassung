package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.ProjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun findByIsActive(
        isActive: Boolean,
        pageable: Pageable,
    ): Page<ProjectEntity>

    fun findByCode(code: String): ProjectEntity?

    fun findByName(name: String): ProjectEntity?

    fun existsByCode(code: String): Boolean

    fun existsByName(name: String): Boolean
}
