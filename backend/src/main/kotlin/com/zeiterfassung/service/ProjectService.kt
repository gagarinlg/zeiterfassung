package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateProjectRequest
import com.zeiterfassung.model.dto.CreateTimeAllocationRequest
import com.zeiterfassung.model.dto.ProjectResponse
import com.zeiterfassung.model.dto.TimeAllocationResponse
import com.zeiterfassung.model.dto.UpdateProjectRequest
import com.zeiterfassung.model.dto.UpdateTimeAllocationRequest
import com.zeiterfassung.model.entity.ProjectEntity
import com.zeiterfassung.model.entity.TimeAllocationEntity
import com.zeiterfassung.repository.ProjectRepository
import com.zeiterfassung.repository.TimeAllocationRepository
import com.zeiterfassung.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val timeAllocationRepository: TimeAllocationRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
) {
    // ---- Project CRUD ----

    @Transactional
    fun createProject(
        dto: CreateProjectRequest,
        actorId: UUID,
    ): ProjectResponse {
        if (projectRepository.existsByCode(dto.code)) {
            throw ConflictException("project.error.code_exists")
        }
        if (projectRepository.existsByName(dto.name)) {
            throw ConflictException("project.error.name_exists")
        }

        val entity =
            ProjectEntity(
                name = dto.name,
                code = dto.code,
                description = dto.description,
                costCenter = dto.costCenter,
            )
        val saved = projectRepository.save(entity)
        auditService.logDataChange(actorId, "PROJECT_CREATED", "Project", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun updateProject(
        projectId: UUID,
        dto: UpdateProjectRequest,
        actorId: UUID,
    ): ProjectResponse {
        val entity = findProjectOrThrow(projectId)
        val oldResponse = entity.toResponse()

        dto.name?.let { newName ->
            val existing = projectRepository.findByName(newName)
            if (existing != null && existing.id != projectId) {
                throw ConflictException("project.error.name_exists")
            }
            entity.name = newName
        }
        dto.code?.let { newCode ->
            val existing = projectRepository.findByCode(newCode)
            if (existing != null && existing.id != projectId) {
                throw ConflictException("project.error.code_exists")
            }
            entity.code = newCode
        }
        dto.description?.let { entity.description = it }
        dto.costCenter?.let { entity.costCenter = it }
        dto.isActive?.let { entity.isActive = it }

        val saved = projectRepository.save(entity)
        auditService.logDataChange(actorId, "PROJECT_UPDATED", "Project", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    fun getProject(projectId: UUID): ProjectResponse = findProjectOrThrow(projectId).toResponse()

    fun getProjects(
        activeOnly: Boolean,
        pageable: Pageable,
    ): Page<ProjectResponse> =
        if (activeOnly) {
            projectRepository.findByIsActive(true, pageable).map { it.toResponse() }
        } else {
            projectRepository.findAll(pageable).map { it.toResponse() }
        }

    // ---- Time Allocation CRUD ----

    @Transactional
    fun createTimeAllocation(
        userId: UUID,
        dto: CreateTimeAllocationRequest,
    ): TimeAllocationResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found: $userId") }
        val project = findProjectOrThrow(dto.projectId)
        if (!project.isActive) {
            throw BadRequestException("project.error.inactive")
        }
        if (dto.minutes <= 0) {
            throw BadRequestException("time_allocation.error.invalid_minutes")
        }

        val entity =
            TimeAllocationEntity(
                user = user,
                project = project,
                date = dto.date,
                minutes = dto.minutes,
                notes = dto.notes,
            )
        val saved = timeAllocationRepository.save(entity)
        auditService.logDataChange(userId, "TIME_ALLOCATION_CREATED", "TimeAllocation", saved.id, null, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun updateTimeAllocation(
        allocationId: UUID,
        userId: UUID,
        dto: UpdateTimeAllocationRequest,
    ): TimeAllocationResponse {
        val entity = findAllocationOrThrow(allocationId)
        if (entity.user.id != userId) {
            throw ForbiddenException("time_allocation.error.not_owner")
        }

        val oldResponse = entity.toResponse()

        dto.projectId?.let { newProjectId ->
            val project = findProjectOrThrow(newProjectId)
            if (!project.isActive) {
                throw BadRequestException("project.error.inactive")
            }
            entity.project = project
        }
        dto.date?.let { entity.date = it }
        dto.minutes?.let { newMinutes ->
            if (newMinutes <= 0) {
                throw BadRequestException("time_allocation.error.invalid_minutes")
            }
            entity.minutes = newMinutes
        }
        dto.notes?.let { entity.notes = it }

        val saved = timeAllocationRepository.save(entity)
        auditService.logDataChange(userId, "TIME_ALLOCATION_UPDATED", "TimeAllocation", saved.id, oldResponse, saved.toResponse())
        return saved.toResponse()
    }

    @Transactional
    fun deleteTimeAllocation(
        allocationId: UUID,
        userId: UUID,
    ) {
        val entity = findAllocationOrThrow(allocationId)
        if (entity.user.id != userId) {
            throw ForbiddenException("time_allocation.error.not_owner")
        }
        val oldResponse = entity.toResponse()
        timeAllocationRepository.delete(entity)
        auditService.logDataChange(userId, "TIME_ALLOCATION_DELETED", "TimeAllocation", allocationId, oldResponse, null)
    }

    fun getUserAllocations(
        userId: UUID,
        pageable: Pageable,
    ): Page<TimeAllocationResponse> = timeAllocationRepository.findByUserId(userId, pageable).map { it.toResponse() }

    fun getUserAllocationsForDate(
        userId: UUID,
        date: LocalDate,
    ): List<TimeAllocationResponse> =
        timeAllocationRepository
            .findByUserIdAndDate(userId, date)
            .map { it.toResponse() }

    fun getUserAllocationsForDateRange(
        userId: UUID,
        start: LocalDate,
        end: LocalDate,
    ): List<TimeAllocationResponse> =
        timeAllocationRepository
            .findByUserIdAndDateBetween(userId, start, end)
            .map { it.toResponse() }

    fun getProjectAllocations(
        projectId: UUID,
        pageable: Pageable,
    ): Page<TimeAllocationResponse> = timeAllocationRepository.findByProjectId(projectId, pageable).map { it.toResponse() }

    fun getAllocation(allocationId: UUID): TimeAllocationResponse = findAllocationOrThrow(allocationId).toResponse()

    private fun findProjectOrThrow(id: UUID): ProjectEntity =
        projectRepository
            .findById(id)
            .orElseThrow { ResourceNotFoundException("Project not found: $id") }

    private fun findAllocationOrThrow(id: UUID): TimeAllocationEntity =
        timeAllocationRepository
            .findById(id)
            .orElseThrow { ResourceNotFoundException("Time allocation not found: $id") }

    private fun ProjectEntity.toResponse() =
        ProjectResponse(
            id = id,
            name = name,
            code = code,
            description = description,
            costCenter = costCenter,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun TimeAllocationEntity.toResponse() =
        TimeAllocationResponse(
            id = id,
            userId = user.id,
            userName = "${user.firstName} ${user.lastName}",
            projectId = project.id,
            projectName = project.name,
            projectCode = project.code,
            date = date,
            minutes = minutes,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
