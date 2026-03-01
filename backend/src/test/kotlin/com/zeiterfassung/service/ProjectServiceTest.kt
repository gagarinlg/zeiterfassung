package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateProjectRequest
import com.zeiterfassung.model.dto.CreateTimeAllocationRequest
import com.zeiterfassung.model.dto.UpdateProjectRequest
import com.zeiterfassung.model.dto.UpdateTimeAllocationRequest
import com.zeiterfassung.model.entity.ProjectEntity
import com.zeiterfassung.model.entity.TimeAllocationEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.ProjectRepository
import com.zeiterfassung.repository.TimeAllocationRepository
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectServiceTest {
    @Mock private lateinit var projectRepository: ProjectRepository

    @Mock private lateinit var timeAllocationRepository: TimeAllocationRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var auditService: AuditService

    private lateinit var service: ProjectService

    private lateinit var user: UserEntity
    private val userId = UUID.randomUUID()
    private val projectId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            ProjectService(
                projectRepository,
                timeAllocationRepository,
                userRepository,
                auditService,
            )
        user = UserEntity(id = userId, email = "user@test.com", passwordHash = "hash", firstName = "John", lastName = "Doe")
    }

    // ---- createProject ----

    @Test
    fun `createProject success`() {
        val dto = CreateProjectRequest(name = "Project Alpha", code = "PA-001")

        `when`(projectRepository.existsByCode(dto.code)).thenReturn(false)
        `when`(projectRepository.existsByName(dto.name)).thenReturn(false)
        `when`(projectRepository.save(any())).thenAnswer { it.arguments[0] as ProjectEntity }

        val result = service.createProject(dto, userId)

        assertThat(result.name).isEqualTo("Project Alpha")
        assertThat(result.code).isEqualTo("PA-001")
        assertThat(result.isActive).isTrue()
    }

    @Test
    fun `createProject throws duplicate code`() {
        val dto = CreateProjectRequest(name = "Project Alpha", code = "PA-001")

        `when`(projectRepository.existsByCode(dto.code)).thenReturn(true)

        assertThrows<ConflictException> { service.createProject(dto, userId) }
    }

    @Test
    fun `createProject throws duplicate name`() {
        val dto = CreateProjectRequest(name = "Project Alpha", code = "PA-001")

        `when`(projectRepository.existsByCode(dto.code)).thenReturn(false)
        `when`(projectRepository.existsByName(dto.name)).thenReturn(true)

        assertThrows<ConflictException> { service.createProject(dto, userId) }
    }

    // ---- updateProject ----

    @Test
    fun `updateProject success`() {
        val entity = projectEntity()
        val dto = UpdateProjectRequest(name = "Updated Name")

        `when`(projectRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(projectRepository.findByName("Updated Name")).thenReturn(null)
        `when`(projectRepository.save(any())).thenAnswer { it.arguments[0] as ProjectEntity }

        val result = service.updateProject(entity.id, dto, userId)

        assertThat(result.name).isEqualTo("Updated Name")
    }

    @Test
    fun `updateProject throws when not found`() {
        val id = UUID.randomUUID()
        val dto = UpdateProjectRequest(name = "test")

        `when`(projectRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.updateProject(id, dto, userId) }
    }

    // ---- getProject ----

    @Test
    fun `getProject throws when not found`() {
        val id = UUID.randomUUID()
        `when`(projectRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getProject(id) }
    }

    // ---- createTimeAllocation ----

    @Test
    fun `createTimeAllocation success`() {
        val project = projectEntity()
        val dto =
            CreateTimeAllocationRequest(
                projectId = project.id,
                date = LocalDate.now(),
                minutes = 120,
                notes = "Development",
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(projectRepository.findById(project.id)).thenReturn(Optional.of(project))
        `when`(timeAllocationRepository.save(any())).thenAnswer { it.arguments[0] as TimeAllocationEntity }

        val result = service.createTimeAllocation(userId, dto)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.projectId).isEqualTo(project.id)
        assertThat(result.minutes).isEqualTo(120)
    }

    @Test
    fun `createTimeAllocation throws when project not found`() {
        val dto =
            CreateTimeAllocationRequest(
                projectId = UUID.randomUUID(),
                date = LocalDate.now(),
                minutes = 60,
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(projectRepository.findById(dto.projectId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.createTimeAllocation(userId, dto) }
    }

    @Test
    fun `createTimeAllocation throws when project inactive`() {
        val project = projectEntity(isActive = false)
        val dto =
            CreateTimeAllocationRequest(
                projectId = project.id,
                date = LocalDate.now(),
                minutes = 60,
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(projectRepository.findById(project.id)).thenReturn(Optional.of(project))

        assertThrows<BadRequestException> { service.createTimeAllocation(userId, dto) }
    }

    @Test
    fun `createTimeAllocation throws when minutes non-positive`() {
        val project = projectEntity()
        val dto =
            CreateTimeAllocationRequest(
                projectId = project.id,
                date = LocalDate.now(),
                minutes = 0,
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(projectRepository.findById(project.id)).thenReturn(Optional.of(project))

        assertThrows<BadRequestException> { service.createTimeAllocation(userId, dto) }
    }

    // ---- updateTimeAllocation ----

    @Test
    fun `updateTimeAllocation success`() {
        val project = projectEntity()
        val allocation = allocationEntity(userId = userId, project = project)
        val dto = UpdateTimeAllocationRequest(minutes = 180)

        `when`(timeAllocationRepository.findById(allocation.id)).thenReturn(Optional.of(allocation))
        `when`(timeAllocationRepository.save(any())).thenAnswer { it.arguments[0] as TimeAllocationEntity }

        val result = service.updateTimeAllocation(allocation.id, userId, dto)

        assertThat(result.minutes).isEqualTo(180)
    }

    @Test
    fun `updateTimeAllocation throws when not owner`() {
        val project = projectEntity()
        val allocation = allocationEntity(userId = UUID.randomUUID(), project = project)
        val dto = UpdateTimeAllocationRequest(minutes = 180)

        `when`(timeAllocationRepository.findById(allocation.id)).thenReturn(Optional.of(allocation))

        assertThrows<ForbiddenException> { service.updateTimeAllocation(allocation.id, userId, dto) }
    }

    // ---- deleteTimeAllocation ----

    @Test
    fun `deleteTimeAllocation success`() {
        val project = projectEntity()
        val allocation = allocationEntity(userId = userId, project = project)

        `when`(timeAllocationRepository.findById(allocation.id)).thenReturn(Optional.of(allocation))

        service.deleteTimeAllocation(allocation.id, userId)
        // no exception means success
    }

    @Test
    fun `deleteTimeAllocation throws when not owner`() {
        val project = projectEntity()
        val allocation = allocationEntity(userId = UUID.randomUUID(), project = project)

        `when`(timeAllocationRepository.findById(allocation.id)).thenReturn(Optional.of(allocation))

        assertThrows<ForbiddenException> { service.deleteTimeAllocation(allocation.id, userId) }
    }

    // ---- helpers ----

    private fun projectEntity(isActive: Boolean = true) =
        ProjectEntity(
            id = projectId,
            name = "Project Alpha",
            code = "PA-001",
            description = "Test project",
            isActive = isActive,
        )

    private fun allocationEntity(
        userId: UUID,
        project: ProjectEntity,
    ): TimeAllocationEntity {
        val allocUser =
            if (userId == this.userId) {
                user
            } else {
                UserEntity(
                    id = userId,
                    email = "other@test.com",
                    passwordHash = "hash",
                    firstName = "Other",
                    lastName = "User",
                )
            }
        return TimeAllocationEntity(
            user = allocUser,
            project = project,
            date = LocalDate.now(),
            minutes = 120,
        )
    }
}
