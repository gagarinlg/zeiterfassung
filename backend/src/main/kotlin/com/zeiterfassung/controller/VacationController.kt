package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.ApproveVacationRequest
import com.zeiterfassung.model.dto.CreateVacationRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.PublicHolidayResponse
import com.zeiterfassung.model.dto.RejectVacationRequest
import com.zeiterfassung.model.dto.UpdateVacationRequest
import com.zeiterfassung.model.dto.VacationBalanceResponse
import com.zeiterfassung.model.dto.VacationCalendarResponse
import com.zeiterfassung.model.dto.VacationRequestResponse
import com.zeiterfassung.model.enums.VacationStatus
import com.zeiterfassung.service.VacationService
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
class VacationController(
    private val vacationService: VacationService,
) {
    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    fun createRequest(
        @Valid @RequestBody dto: CreateVacationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<VacationRequestResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(vacationService.createRequest(UUID.fromString(userId), dto))

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('vacation.request.own')")
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
    fun getRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<VacationRequestResponse> = ResponseEntity.ok(vacationService.getRequest(id, UUID.fromString(userId)))

    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    fun updateRequest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateVacationRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<VacationRequestResponse> = ResponseEntity.ok(vacationService.updateRequest(id, UUID.fromString(userId), dto))

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('vacation.request.own')")
    fun cancelRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        vacationService.cancelRequest(id, UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('vacation.approve')")
    fun approveRequest(
        @PathVariable id: UUID,
        @RequestBody(required = false) dto: ApproveVacationRequest?,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<VacationRequestResponse> =
        ResponseEntity.ok(vacationService.approveRequest(id, UUID.fromString(approverId), dto ?: ApproveVacationRequest()))

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('vacation.approve')")
    fun rejectRequest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RejectVacationRequest,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<VacationRequestResponse> = ResponseEntity.ok(vacationService.rejectRequest(id, UUID.fromString(approverId), dto))

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('vacation.approve')")
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
    fun getMyBalance(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<VacationBalanceResponse> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(vacationService.getBalance(UUID.fromString(userId), targetYear))
    }

    @GetMapping("/balance/{userId}")
    @PreAuthorize("hasAuthority('vacation.view.team')")
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
    fun getPublicHolidays(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) stateCode: String?,
    ): ResponseEntity<List<PublicHolidayResponse>> {
        val targetYear = year ?: LocalDate.now().year
        return ResponseEntity.ok(vacationService.getPublicHolidays(targetYear, stateCode))
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('vacation.view.team')")
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
    fun setBalance(
        @PathVariable userId: UUID,
        @RequestParam(required = false) year: Int?,
        @Valid @RequestBody dto: com.zeiterfassung.model.dto.SetVacationBalanceRequest,
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
