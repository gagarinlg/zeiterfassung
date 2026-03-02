package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.CreateWorkHourChangeRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.RejectWorkHourChangeRequest
import com.zeiterfassung.model.dto.WorkHourChangeResponse
import com.zeiterfassung.service.WorkHourChangeService
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/work-hour-changes")
@Tag(name = "Work Hour Changes")
@SecurityRequirement(name = "bearerAuth")
class WorkHourChangeController(
    private val workHourChangeService: WorkHourChangeService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(
        summary = "Create work hour change request",
        description = "Creates a new work hour change request for the authenticated user.",
    )
    @ApiResponse(responseCode = "201", description = "Work hour change request created")
    @ApiResponse(responseCode = "409", description = "A pending request already exists")
    fun createRequest(
        @Valid @RequestBody dto: CreateWorkHourChangeRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<WorkHourChangeResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(workHourChangeService.createRequest(UUID.fromString(userId), dto))

    @GetMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(
        summary = "Get my work hour change requests",
        description = "Returns a paginated list of the authenticated user's work hour change requests.",
    )
    @ApiResponse(responseCode = "200", description = "Work hour change requests returned")
    fun getMyRequests(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<WorkHourChangeResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = workHourChangeService.getMyRequests(UUID.fromString(userId), pageable)
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

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(
        summary = "Get pending work hour change requests",
        description = "Returns pending work hour change requests for approval.",
    )
    @ApiResponse(responseCode = "200", description = "Pending requests returned")
    fun getPendingRequests(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<WorkHourChangeResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending())
        val result = workHourChangeService.getPendingRequests(pageable)
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

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(
        summary = "Approve work hour change request",
        description = "Approves a pending work hour change request and updates the employee's work hours.",
    )
    @ApiResponse(responseCode = "200", description = "Work hour change request approved")
    @ApiResponse(responseCode = "400", description = "Request is not in pending status")
    fun approveRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<WorkHourChangeResponse> =
        ResponseEntity.ok(workHourChangeService.approveRequest(id, UUID.fromString(approverId)))

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(
        summary = "Reject work hour change request",
        description = "Rejects a pending work hour change request with a mandatory reason.",
    )
    @ApiResponse(responseCode = "200", description = "Work hour change request rejected")
    @ApiResponse(responseCode = "400", description = "Request is not in pending status")
    fun rejectRequest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RejectWorkHourChangeRequest,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<WorkHourChangeResponse> =
        ResponseEntity.ok(workHourChangeService.rejectRequest(id, UUID.fromString(approverId), dto))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(
        summary = "Cancel work hour change request",
        description = "Cancels a pending work hour change request owned by the authenticated user.",
    )
    @ApiResponse(responseCode = "200", description = "Work hour change request cancelled")
    @ApiResponse(responseCode = "400", description = "Request is not in pending status")
    @ApiResponse(responseCode = "403", description = "Not the owner of the request")
    fun cancelRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<WorkHourChangeResponse> =
        ResponseEntity.ok(workHourChangeService.cancelRequest(id, UUID.fromString(userId)))
}
