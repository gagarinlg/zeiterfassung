package com.zeiterfassung.repository

import com.zeiterfassung.model.entity.SystemSettingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SystemSettingRepository : JpaRepository<SystemSettingEntity, UUID> {
    fun findByKey(key: String): Optional<SystemSettingEntity>
}
