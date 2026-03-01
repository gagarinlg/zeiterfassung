package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.CreateSickLeaveRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.SickLeaveResponse
import com.zeiterfassung.model.dto.UpdateSickLeaveRequest
import com.zeiterfassung.service.SickLeaveService
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
import java.util.UUID

@RestController
@RequestMapping("/sick-leave")
@Tag(name = "Sick Leave")
@SecurityRequirement(name = "bearerAuth")
class SickLeaveController(
    private val sickLeaveService: SickLeaveService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Report sick leave", description = "Reports a new sick leave for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Sick leave reported")
    @ApiResponse(responseCode = "400", description = "Invalid dates")
    @ApiResponse(responseCode = "409", description = "Overlapping sick leave exists")
    fun reportSickLeave(
        @Valid @RequestBody dto: CreateSickLeaveRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<SickLeaveResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(sickLeaveService.reportSickLeave(UUID.fromString(userId), dto))

    @PostMapping("/{userId}")
    @PreAuthorize("hasAuthority('time.edit.team')")
    @Operation(summary = "Report sick leave for employee", description = "Manager reports sick leave on behalf of an employee.")
    @ApiResponse(responseCode = "201", description = "Sick leave reported")
    @ApiResponse(responseCode = "400", description = "Invalid dates")
    @ApiResponse(responseCode = "409", description = "Overlapping sick leave exists")
    fun reportSickLeaveForUser(
        @PathVariable userId: UUID,
        @Valid @RequestBody dto: CreateSickLeaveRequest,
        @AuthenticationPrincipal managerId: String,
    ): ResponseEntity<SickLeaveResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(sickLeaveService.reportSickLeaveByManager(UUID.fromString(managerId), userId, dto))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Update sick leave", description = "Updates an existing sick leave entry.")
    @ApiResponse(responseCode = "200", description = "Sick leave updated")
    @ApiResponse(responseCode = "400", description = "Invalid update")
    @ApiResponse(responseCode = "403", description = "Not the owner")
    fun updateSickLeave(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateSickLeaveRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<SickLeaveResponse> =
        ResponseEntity.ok(sickLeaveService.updateSickLeave(id, UUID.fromString(userId), dto))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Cancel sick leave", description = "Cancels a sick leave entry.")
    @ApiResponse(responseCode = "204", description = "Sick leave cancelled")
    fun cancelSickLeave(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        sickLeaveService.cancelSickLeave(id, UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Get my sick leaves", description = "Returns a paginated list of the authenticated user's sick leaves.")
    @ApiResponse(responseCode = "200", description = "Sick leaves returned")
    fun getMySickLeaves(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<SickLeaveResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = sickLeaveService.getUserSickLeaves(UUID.fromString(userId), pageable)
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Get sick leave", description = "Returns a specific sick leave by ID.")
    @ApiResponse(responseCode = "200", description = "Sick leave returned")
    @ApiResponse(responseCode = "404", description = "Sick leave not found")
    fun getSickLeave(
        @PathVariable id: UUID,
    ): ResponseEntity<SickLeaveResponse> = ResponseEntity.ok(sickLeaveService.getSickLeave(id))

    @PostMapping("/{id}/certificate")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Submit certificate", description = "Marks the sick leave certificate as submitted.")
    @ApiResponse(responseCode = "200", description = "Certificate submitted")
    @ApiResponse(responseCode = "403", description = "Not the owner")
    fun submitCertificate(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<SickLeaveResponse> =
        ResponseEntity.ok(sickLeaveService.submitCertificate(id, UUID.fromString(userId)))
}
