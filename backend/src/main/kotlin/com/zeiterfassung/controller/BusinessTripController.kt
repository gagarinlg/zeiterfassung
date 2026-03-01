package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.ApproveBusinessTripRequest
import com.zeiterfassung.model.dto.BusinessTripResponse
import com.zeiterfassung.model.dto.CreateBusinessTripRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.RejectBusinessTripRequest
import com.zeiterfassung.model.dto.UpdateBusinessTripRequest
import com.zeiterfassung.service.BusinessTripService
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
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/business-trips")
@Tag(name = "Business Trips")
@SecurityRequirement(name = "bearerAuth")
class BusinessTripController(
    private val businessTripService: BusinessTripService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Create business trip request", description = "Creates a new business trip request for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Business trip request created")
    @ApiResponse(responseCode = "400", description = "Invalid dates")
    @ApiResponse(responseCode = "409", description = "Overlapping business trip exists")
    fun createTrip(
        @Valid @RequestBody dto: CreateBusinessTripRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<BusinessTripResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(businessTripService.createTrip(UUID.fromString(userId), dto))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Update business trip", description = "Updates a pending business trip request.")
    @ApiResponse(responseCode = "200", description = "Business trip updated")
    @ApiResponse(responseCode = "400", description = "Cannot update non-pending trip")
    fun updateTrip(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateBusinessTripRequest,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<BusinessTripResponse> = ResponseEntity.ok(businessTripService.updateTrip(id, UUID.fromString(userId), dto))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Cancel business trip", description = "Cancels a business trip request.")
    @ApiResponse(responseCode = "204", description = "Business trip cancelled")
    fun cancelTrip(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<Void> {
        businessTripService.cancelTrip(id, UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Get my business trips", description = "Returns a paginated list of the authenticated user's business trips.")
    @ApiResponse(responseCode = "200", description = "Business trips returned")
    fun getMyTrips(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<BusinessTripResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = businessTripService.getUserTrips(UUID.fromString(userId), pageable)
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
    @Operation(summary = "Get business trip", description = "Returns a specific business trip by ID.")
    @ApiResponse(responseCode = "200", description = "Business trip returned")
    @ApiResponse(responseCode = "404", description = "Business trip not found")
    fun getTrip(
        @PathVariable id: UUID,
    ): ResponseEntity<BusinessTripResponse> = ResponseEntity.ok(businessTripService.getTrip(id))

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('vacation.approve')")
    @Operation(summary = "Approve business trip", description = "Approves a pending business trip request. Requires approval permission.")
    @ApiResponse(responseCode = "200", description = "Business trip approved")
    fun approveTrip(
        @PathVariable id: UUID,
        @RequestBody(required = false) dto: ApproveBusinessTripRequest?,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<BusinessTripResponse> =
        ResponseEntity.ok(businessTripService.approveTrip(id, UUID.fromString(approverId), dto ?: ApproveBusinessTripRequest()))

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('vacation.approve')")
    @Operation(summary = "Reject business trip", description = "Rejects a pending business trip request with a mandatory reason.")
    @ApiResponse(responseCode = "200", description = "Business trip rejected")
    fun rejectTrip(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RejectBusinessTripRequest,
        @AuthenticationPrincipal approverId: String,
    ): ResponseEntity<BusinessTripResponse> = ResponseEntity.ok(businessTripService.rejectTrip(id, UUID.fromString(approverId), dto))

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('time.edit.own')")
    @Operation(summary = "Complete business trip", description = "Marks an approved business trip as completed.")
    @ApiResponse(responseCode = "200", description = "Business trip completed")
    fun completeTrip(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) actualCost: BigDecimal?,
    ): ResponseEntity<BusinessTripResponse> = ResponseEntity.ok(businessTripService.completeTrip(id, UUID.fromString(userId), actualCost))

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('vacation.approve')")
    @Operation(
        summary = "Get pending business trips",
        description = "Returns pending business trip requests for approval.",
    )
    @ApiResponse(responseCode = "200", description = "Pending trips returned")
    fun getPendingTrips(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<BusinessTripResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending())
        val result = businessTripService.getPendingTrips(UUID.fromString(userId), pageable)
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
