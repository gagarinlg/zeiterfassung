package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.ApproveVacationRequest
import com.zeiterfassung.model.dto.CreateVacationRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.PublicHolidayResponse
import com.zeiterfassung.model.dto.RejectVacationRequest
import com.zeiterfassung.model.dto.SetVacationBalanceRequest
import com.zeiterfassung.model.dto.UpdateVacationRequest
import com.zeiterfassung.model.dto.VacationBalanceResponse
import com.zeiterfassung.model.dto.VacationCalendarResponse
import com.zeiterfassung.model.dto.VacationRequestResponse
import com.zeiterfassung.model.enums.VacationStatus
import com.zeiterfassung.service.VacationService
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
@RequestMapping("/vacation")
@Tag(name = "Vacation")
@SecurityRequirement(name = "bearerAuth")
class VacationController(
    private val vacationService: VacationService,
) {
    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    @Operation(summary = "Create vacation request", description = "Creates a new vacation request for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Vacation request created")
    @ApiResponse(responseCode = "400", description = "Invalid dates or insufficient balance")
    @ApiResponse(responseCode = "409", description = "Overlapping vacation request exists")
    fun createRequest(
        @Valid @RequestBody dto: CreateVacationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<VacationRequestResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(vacationService.createRequest(UUID.fromString(userId), dto))

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    @Operation(
        summary = "Get my vacation requests",
        description = "Returns a paginated list of the authenticated user's vacation requests.",
    )
    @ApiResponse(responseCode = "200", description = "Vacation requests returned")
    fun getMyRequests(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) status: VacationStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<VacationRequestResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = vacationService.getUserRequests(UUID.fromString(userId), year, status, pageable)
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

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    @Operation(summary = "Get vacation request", description = "Returns a specific vacation request by ID.")
    @ApiResponse(responseCode = "200", description = "Vacation request returned")
    @ApiResponse(responseCode = "404", description = "Vacation request not found")
    fun getRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<VacationRequestResponse> = ResponseEntity.ok(vacationService.getRequest(id, UUID.fromString(userId)))

    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    @Operation(summary = "Update vacation request", description = "Updates a pending vacation request.")
    @ApiResponse(responseCode = "200", description = "Vacation request updated")
    @ApiResponse(responseCode = "400", description = "Cannot update non-pending request")
    fun updateRequest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateVacationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<VacationRequestResponse> = ResponseEntity.ok(vacationService.updateRequest(id, UUID.fromString(userId), dto))

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    @Operation(summary = "Cancel vacation request", description = "Cancels a pending or approved vacation request.")
    @ApiResponse(responseCode = "204", description = "Vacation request cancelled")
    fun cancelRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        vacationService.cancelRequest(id, UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('vacation.approve')")
    @Operation(summary = "Approve vacation request", description = "Approves a pending vacation request. Requires approval permission.")
    @ApiResponse(responseCode = "200", description = "Vacation request approved")
    fun approveRequest(
        @PathVariable id: UUID,
        @RequestBody(required = false) dto: ApproveVacationRequest?,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<VacationRequestResponse> =
        ResponseEntity.ok(vacationService.approveRequest(id, UUID.fromString(approverId), dto ?: ApproveVacationRequest()))

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('vacation.approve')")
    @Operation(summary = "Reject vacation request", description = "Rejects a pending vacation request with a mandatory reason.")
    @ApiResponse(responseCode = "200", description = "Vacation request rejected")
    fun rejectRequest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RejectVacationRequest,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<VacationRequestResponse> = ResponseEntity.ok(vacationService.rejectRequest(id, UUID.fromString(approverId), dto))

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('vacation.approve')")
    @Operation(
        summary = "Get pending requests",
        description = "Returns pending vacation requests for approval. Optionally returns all pending requests.",
    )
    @ApiResponse(responseCode = "200", description = "Pending requests returned")
    fun getPendingRequests(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") allRequests: Boolean,
    ): ResponseEntity<PageResponse<VacationRequestResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending())
        val result =
            if (allRequests) {
                vacationService.getAllPendingRequests(pageable)
            } else {
                vacationService.getPendingRequests(UUID.fromString(userId), pageable)
            }
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

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    @Operation(summary = "Get my vacation balance", description = "Returns the vacation balance for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Vacation balance returned")
    fun getMyBalance(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<VacationBalanceResponse> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(vacationService.getBalance(UUID.fromString(userId), targetYear))
    }

    @GetMapping("/balance/{userId}")
    @PreAuthorize("hasAuthority('vacation.view.team')")
    @Operation(
        summary = "Get user vacation balance",
        description = "Returns the vacation balance for a specific user. Requires team view permission.",
    )
    @ApiResponse(responseCode = "200", description = "Vacation balance returned")
    fun getUserBalance(
        @PathVariable userId: UUID,
        @RequestParam(required = false) year: Int?,
        @AuthenticationPrincipal requestingUserId: String,
    ): ResponseEntity<VacationBalanceResponse> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(vacationService.getBalanceForManager(UUID.fromString(requestingUserId), userId, targetYear))
    }

    @GetMapping("/holidays")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get public holidays",
        description = "Returns public holidays for the specified year, optionally filtered by German state.",
    )
    @ApiResponse(responseCode = "200", description = "Public holidays returned")
    fun getPublicHolidays(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) stateCode: String?,
    ): ResponseEntity<List<PublicHolidayResponse>> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(vacationService.getPublicHolidays(targetYear, stateCode))
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('vacation.view.team')")
    @Operation(
        summary = "Get team vacation calendar",
        description = "Returns a vacation calendar showing team member absences for the specified month.",
    )
    @ApiResponse(responseCode = "200", description = "Vacation calendar returned")
    fun getTeamCalendar(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
    ): ResponseEntity<VacationCalendarResponse> {
        val today = LocalDate.now()
        val targetYear = year ?: today.year
        val targetMonth = month ?: today.monthValue
        return ResponseEntity.ok(vacationService.getTeamCalendar(UUID.fromString(userId), targetYear, targetMonth))
    }

    @PutMapping("/balance/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Set vacation balance", description = "Manually sets the vacation balance for a user. Requires admin permission.")
    @ApiResponse(responseCode = "200", description = "Vacation balance updated")
    fun setBalance(
        @PathVariable userId: UUID,
        @RequestParam(required = false) year: Int?,
        @Valid @RequestBody dto: SetVacationBalanceRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<VacationBalanceResponse> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(
            vacationService.setBalance(
                userId,
                targetYear,
                dto.totalDays,
                dto.usedDays,
                dto.carriedOverDays,
                UUID.fromString(actorId),
            ),
        )
    }

    @PostMapping("/balance/{userId}/carry-over")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Trigger vacation carry-over", description = "Triggers the vacation balance carry-over calculation for a user.")
    @ApiResponse(responseCode = "200", description = "Carry-over calculated")
    fun triggerCarryOver(
        @PathVariable userId: UUID,
        @RequestParam(required = false) year: Int?,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<VacationBalanceResponse> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(
            vacationService.triggerCarryOver(userId, targetYear, UUID.fromString(actorId)),
        )
    }
}
