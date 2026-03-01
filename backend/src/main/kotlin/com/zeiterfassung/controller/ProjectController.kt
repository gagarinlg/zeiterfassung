package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.CreateProjectRequest
import com.zeiterfassung.model.dto.CreateTimeAllocationRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.ProjectResponse
import com.zeiterfassung.model.dto.TimeAllocationResponse
import com.zeiterfassung.model.dto.UpdateProjectRequest
import com.zeiterfassung.model.dto.UpdateTimeAllocationRequest
import com.zeiterfassung.service.ProjectService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/projects")
@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
class ProjectController(
    private val projectService: ProjectService,
) {
    // ---- Project CRUD (admin only) ----

    @PostMapping
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Create project", description = "Creates a new project/cost center. Requires admin permission.")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "409", description = "Project code or name already exists")
    fun createProject(
        @Valid @RequestBody dto: CreateProjectRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<ProjectResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(projectService.createProject(dto, UUID.fromString(actorId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Update project", description = "Updates an existing project. Requires admin permission.")
    @ApiResponse(responseCode = "200", description = "Project updated")
    fun updateProject(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateProjectRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<ProjectResponse> =
        ResponseEntity.ok(projectService.updateProject(id, dto, UUID.fromString(actorId)))

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get project", description = "Returns a specific project by ID.")
    @ApiResponse(responseCode = "200", description = "Project returned")
    @ApiResponse(responseCode = "404", description = "Project not found")
    fun getProject(
        @PathVariable id: UUID,
    ): ResponseEntity<ProjectResponse> = ResponseEntity.ok(projectService.getProject(id))

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get projects", description = "Returns a paginated list of projects.")
    @ApiResponse(responseCode = "200", description = "Projects returned")
    fun getProjects(
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ProjectResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val result = projectService.getProjects(activeOnly, pageable)
        return ResponseEntity.ok(
            PageResponse(
                content = result.content,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                pageNumber = result.number,
                pageSize = result.size,
            ),
        )
    }

    // ---- Time Allocation ----

    @PostMapping("/allocations")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Create time allocation", description = "Allocates time to a project for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Time allocation created")
    @ApiResponse(responseCode = "400", description = "Invalid allocation")
    fun createAllocation(
        @Valid @RequestBody dto: CreateTimeAllocationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<TimeAllocationResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(projectService.createTimeAllocation(UUID.fromString(userId), dto))

    @PutMapping("/allocations/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Update time allocation", description = "Updates an existing time allocation.")
    @ApiResponse(responseCode = "200", description = "Time allocation updated")
    fun updateAllocation(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateTimeAllocationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<TimeAllocationResponse> =
        ResponseEntity.ok(projectService.updateTimeAllocation(id, UUID.fromString(userId), dto))

    @DeleteMapping("/allocations/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Delete time allocation", description = "Deletes a time allocation.")
    @ApiResponse(responseCode = "204", description = "Time allocation deleted")
    fun deleteAllocation(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        projectService.deleteTimeAllocation(id, UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/allocations")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Get my time allocations", description = "Returns a paginated list of the authenticated user's time allocations.")
    @ApiResponse(responseCode = "200", description = "Time allocations returned")
    fun getMyAllocations(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<TimeAllocationResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("date").descending())
        val result = projectService.getUserAllocations(UUID.fromString(userId), pageable)
        return ResponseEntity.ok(
            PageResponse(
                content = result.content,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                pageNumber = result.number,
                pageSize = result.size,
            ),
        )
    }

    @GetMapping("/allocations/by-date")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Get allocations by date", description = "Returns time allocations for a specific date.")
    @ApiResponse(responseCode = "200", description = "Time allocations returned")
    fun getAllocationsByDate(
        @AuthenticationPrincipal userId: String,
        @RequestParam date: LocalDate,
    ): ResponseEntity<List<TimeAllocationResponse>> =
        ResponseEntity.ok(projectService.getUserAllocationsForDate(UUID.fromString(userId), date))

    @GetMapping("/allocations/by-range")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Get allocations by date range", description = "Returns time allocations for a date range.")
    @ApiResponse(responseCode = "200", description = "Time allocations returned")
    fun getAllocationsByRange(
        @AuthenticationPrincipal userId: String,
        @RequestParam start: LocalDate,
        @RequestParam end: LocalDate,
    ): ResponseEntity<List<TimeAllocationResponse>> =
        ResponseEntity.ok(projectService.getUserAllocationsForDateRange(UUID.fromString(userId), start, end))

    @GetMapping("/{projectId}/allocations")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(
        summary = "Get project allocations",
        description = "Returns all time allocations for a project. Requires admin permission.",
    )
    @ApiResponse(responseCode = "200", description = "Time allocations returned")
    fun getProjectAllocations(
        @PathVariable projectId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<TimeAllocationResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("date").descending())
        val result = projectService.getProjectAllocations(projectId, pageable)
        return ResponseEntity.ok(
            PageResponse(
                content = result.content,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                pageNumber = result.number,
                pageSize = result.size,
            ),
        )
    }
}
