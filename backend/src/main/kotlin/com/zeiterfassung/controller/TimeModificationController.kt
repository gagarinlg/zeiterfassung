package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.CreateTimeModificationRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.RejectTimeModificationRequest
import com.zeiterfassung.model.dto.TimeModificationResponse
import com.zeiterfassung.service.TimeModificationService
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
@RequestMapping("/time-modifications")
@Tag(name = "Time Modifications")
@SecurityRequirement(name = "bearerAuth")
class TimeModificationController(
    private val timeModificationService: TimeModificationService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(
        summary = "Create time modification request",
        description = "Creates a new time modification request for the authenticated user.",
    )
    @ApiResponse(responseCode = "201", description = "Time modification request created")
    @ApiResponse(responseCode = "409", description = "A pending request already exists for this time entry")
    fun createRequest(
        @Valid @RequestBody dto: CreateTimeModificationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<TimeModificationResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(timeModificationService.createRequest(UUID.fromString(userId), dto))

    @GetMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(
        summary = "Get my time modification requests",
        description = "Returns a paginated list of the authenticated user's time modification requests.",
    )
    @ApiResponse(responseCode = "200", description = "Time modification requests returned")
    fun getMyRequests(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<TimeModificationResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = timeModificationService.getMyRequests(UUID.fromString(userId), pageable)
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
        summary = "Get pending time modification requests",
        description = "Returns pending time modification requests for approval.",
    )
    @ApiResponse(responseCode = "200", description = "Pending requests returned")
    fun getPendingRequests(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<TimeModificationResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending())
        val result = timeModificationService.getPendingRequests(pageable)
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
        summary = "Approve time modification request",
        description = "Approves a pending time modification request and updates the time entry.",
    )
    @ApiResponse(responseCode = "200", description = "Time modification request approved")
    @ApiResponse(responseCode = "400", description = "Request is not in pending status")
    fun approveRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<TimeModificationResponse> = ResponseEntity.ok(timeModificationService.approveRequest(id, UUID.fromString(approverId)))

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(
        summary = "Reject time modification request",
        description = "Rejects a pending time modification request with a mandatory reason.",
    )
    @ApiResponse(responseCode = "200", description = "Time modification request rejected")
    @ApiResponse(responseCode = "400", description = "Request is not in pending status")
    fun rejectRequest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RejectTimeModificationRequest,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<TimeModificationResponse> =
        ResponseEntity.ok(
            timeModificationService.rejectRequest(id, UUID.fromString(approverId), dto),
        )

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(
        summary = "Cancel time modification request",
        description = "Cancels a pending time modification request owned by the authenticated user.",
    )
    @ApiResponse(responseCode = "200", description = "Time modification request cancelled")
    @ApiResponse(responseCode = "400", description = "Request is not in pending status")
    @ApiResponse(responseCode = "403", description = "Not the owner of the request")
    fun cancelRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<TimeModificationResponse> = ResponseEntity.ok(timeModificationService.cancelRequest(id, UUID.fromString(userId)))
}
