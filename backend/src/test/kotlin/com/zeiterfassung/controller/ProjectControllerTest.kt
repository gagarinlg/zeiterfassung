package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.CreateProjectRequest
import com.zeiterfassung.model.dto.CreateTimeAllocationRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.ProjectResponse
import com.zeiterfassung.model.dto.TimeAllocationResponse
import com.zeiterfassung.model.dto.UpdateProjectRequest
import com.zeiterfassung.model.dto.UpdateTimeAllocationRequest
import com.zeiterfassung.service.ProjectService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectControllerTest {
    @Mock
    private lateinit var projectService: ProjectService

    private lateinit var controller: ProjectController

    private val userId = UUID.randomUUID()
    private val actorId = userId.toString()

    private fun sampleProject(id: UUID = UUID.randomUUID()): ProjectResponse =
        ProjectResponse(
            id = id,
            name = "Test Project",
            code = "PROJ001",
            description = "A test project",
            costCenter = "CC100",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private fun sampleAllocation(id: UUID = UUID.randomUUID()): TimeAllocationResponse =
        TimeAllocationResponse(
            id = id,
            userId = userId,
            userName = "Max Mustermann",
            projectId = UUID.randomUUID(),
            projectName = "Test Project",
            projectCode = "PROJ001",
            date = LocalDate.of(2026, 6, 1),
            minutes = 120,
            notes = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = ProjectController(projectService)
    }

    @Test
    fun `createProject should return 201 with project`() {
        val dto = CreateProjectRequest("New Project", "NP001", "Description", "CC200")
        val expected = sampleProject()
        `when`(projectService.createProject(dto, userId)).thenReturn(expected)

        val response = controller.createProject(dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!.name).isEqualTo("Test Project")
    }

    @Test
    fun `updateProject should return updated project`() {
        val projectId = UUID.randomUUID()
        val dto = UpdateProjectRequest(name = "Updated")
        val expected = sampleProject(projectId)
        `when`(projectService.updateProject(projectId, dto, userId)).thenReturn(expected)

        val response = controller.updateProject(projectId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `getProject should return project by id`() {
        val projectId = UUID.randomUUID()
        val expected = sampleProject(projectId)
        `when`(projectService.getProject(projectId)).thenReturn(expected)

        val response = controller.getProject(projectId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo(projectId)
    }

    @Test
    fun `getProjects should return paginated projects`() {
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        val page = PageImpl(listOf(sampleProject()), pageable, 1)
        `when`(projectService.getProjects(true, pageable)).thenReturn(page)

        val response = controller.getProjects(true, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }

    @Test
    fun `createAllocation should return 201 with allocation`() {
        val projectId = UUID.randomUUID()
        val dto = CreateTimeAllocationRequest(projectId, LocalDate.of(2026, 6, 1), 120, null)
        val expected = sampleAllocation()
        `when`(projectService.createTimeAllocation(userId, dto)).thenReturn(expected)

        val response = controller.createAllocation(dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `updateAllocation should return updated allocation`() {
        val allocationId = UUID.randomUUID()
        val dto = UpdateTimeAllocationRequest(minutes = 180)
        val expected = sampleAllocation(allocationId)
        `when`(projectService.updateTimeAllocation(allocationId, userId, dto)).thenReturn(expected)

        val response = controller.updateAllocation(allocationId, dto, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `deleteAllocation should return no content`() {
        val allocationId = UUID.randomUUID()

        val response = controller.deleteAllocation(allocationId, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(projectService).deleteTimeAllocation(allocationId, userId)
    }

    @Test
    fun `getMyAllocations should return paginated allocations`() {
        val pageable = PageRequest.of(0, 20, Sort.by("date").descending())
        val page = PageImpl(listOf(sampleAllocation()), pageable, 1)
        `when`(projectService.getUserAllocations(userId, pageable)).thenReturn(page)

        val response = controller.getMyAllocations(actorId, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }

    @Test
    fun `getAllocationsByDate should return allocations for date`() {
        val date = LocalDate.of(2026, 6, 1)
        val allocations = listOf(sampleAllocation())
        `when`(projectService.getUserAllocationsForDate(userId, date)).thenReturn(allocations)

        val response = controller.getAllocationsByDate(actorId, date)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `getAllocationsByRange should return allocations for range`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 30)
        val allocations = listOf(sampleAllocation())
        `when`(projectService.getUserAllocationsForDateRange(userId, start, end)).thenReturn(allocations)

        val response = controller.getAllocationsByRange(actorId, start, end)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `getProjectAllocations should return paginated project allocations`() {
        val projectId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 20, Sort.by("date").descending())
        val page = PageImpl(listOf(sampleAllocation()), pageable, 1)
        `when`(projectService.getProjectAllocations(projectId, pageable)).thenReturn(page)

        val response = controller.getProjectAllocations(projectId, 0, 20)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.content).hasSize(1)
    }
}
