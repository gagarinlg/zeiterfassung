package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.EmployeeConfigRequest
import com.zeiterfassung.model.dto.EmployeeConfigResponse
import com.zeiterfassung.model.entity.EmployeeConfigEntity
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class EmployeeConfigService(
    private val employeeConfigRepository: EmployeeConfigRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {
    fun getConfig(userId: UUID): EmployeeConfigResponse {
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found: $userId") }
        val config =
            employeeConfigRepository.findByUserId(userId)
                ?: return createDefaultConfig(userId)
        return config.toResponse()
    }

    @Transactional
    fun updateConfig(
        adminUserId: UUID,
        userId: UUID,
        request: EmployeeConfigRequest,
    ): EmployeeConfigResponse {
        val user =
            userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }

        val existingConfig = employeeConfigRepository.findByUserId(userId)
        val isNew = existingConfig == null
        val config = existingConfig ?: EmployeeConfigEntity(user = user)
        val oldResponse = if (!isNew) config.toResponse() else null

        request.weeklyWorkHours?.let { config.weeklyWorkHours = it }
        request.dailyWorkHours?.let { config.dailyWorkHours = it }
        request.workDays?.let { config.workDays = objectMapper.writeValueAsString(it) }
        request.vacationDaysPerYear?.let { config.vacationDaysPerYear = it }
        request.vacationCarryOverMax?.let { config.vacationCarryOverMax = it }
        request.contractStartDate?.let { config.contractStartDate = it }
        request.contractEndDate?.let { config.contractEndDate = it }
        request.isHomeOfficeEligible?.let { config.isHomeOfficeEligible = it }

        val saved = employeeConfigRepository.save(config)
        auditService.logDataChange(adminUserId, "EMPLOYEE_CONFIG_UPDATED", "EmployeeConfig", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    private fun createDefaultConfig(userId: UUID): EmployeeConfigResponse {
        val user =
            userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }
        val config = EmployeeConfigEntity(user = user)
        val saved = employeeConfigRepository.save(config)
        return saved.toResponse()
    }

    private fun EmployeeConfigEntity.toResponse(): EmployeeConfigResponse {
        val workDaysList: List<Int> =
            try {
                objectMapper.readValue(workDays)
            } catch (e: Exception) {
                listOf(1, 2, 3, 4, 5)
            }
        return EmployeeConfigResponse(
            id = id,
            userId = user.id,
            weeklyWorkHours = weeklyWorkHours,
            dailyWorkHours = dailyWorkHours,
            workDays = workDaysList,
            vacationDaysPerYear = vacationDaysPerYear,
            vacationCarryOverMax = vacationCarryOverMax,
            contractStartDate = contractStartDate,
            contractEndDate = contractEndDate,
            isHomeOfficeEligible = isHomeOfficeEligible,
        )
    }
}
