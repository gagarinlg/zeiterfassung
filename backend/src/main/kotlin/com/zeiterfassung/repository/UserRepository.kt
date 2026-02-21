package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): Optional<UserEntity>

    fun findByRfidTagId(rfidTagId: String): Optional<UserEntity>

    fun findByEmployeeNumber(employeeNumber: String): Optional<UserEntity>

    fun findByIsDeletedFalse(pageable: Pageable): Page<UserEntity>

    fun findByManagerId(managerId: UUID): List<UserEntity>

    fun existsByEmail(email: String): Boolean

    fun existsByEmailAndIdNot(
        email: String,
        id: UUID,
    ): Boolean
}
