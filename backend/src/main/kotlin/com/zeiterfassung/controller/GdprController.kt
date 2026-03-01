package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.GdprDataExportResponse
import com.zeiterfassung.model.dto.GdprDeletionResponse
import com.zeiterfassung.service.GdprService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/gdpr")
@Tag(name = "GDPR")
@SecurityRequirement(name = "bearerAuth")
class GdprController(
    private val gdprService: GdprService,
) {
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export own data", description = "Exports all personal data for the authenticated user (GDPR Art. 15).")
    @ApiResponse(responseCode = "200", description = "Data export returned")
    fun exportOwnData(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<GdprDataExportResponse> = ResponseEntity.ok(gdprService.exportUserData(UUID.fromString(userId)))

    @PostMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Request account deletion",
        description = "Anonymizes personal data and deactivates the authenticated user's account (GDPR Art. 17).",
    )
    @ApiResponse(responseCode = "200", description = "Account deleted and anonymized")
    fun requestOwnDeletion(
        @AuthenticationPrincipal userId: String,
    ): ResponseEntity<GdprDeletionResponse> {
        val id = UUID.fromString(userId)
        return ResponseEntity.ok(gdprService.requestDeletion(id, id))
    }

    @GetMapping("/export/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Export user data (admin)", description = "Exports all personal data for a specific user (admin only).")
    @ApiResponse(responseCode = "200", description = "Data export returned")
    @ApiResponse(responseCode = "404", description = "User not found")
    fun exportUserData(
        @PathVariable userId: UUID,
    ): ResponseEntity<GdprDataExportResponse> = ResponseEntity.ok(gdprService.exportUserData(userId))

    @PostMapping("/delete/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(
        summary = "Delete user data (admin)",
        description = "Anonymizes personal data and deactivates a specific user's account (admin only).",
    )
    @ApiResponse(responseCode = "200", description = "Account deleted and anonymized")
    @ApiResponse(responseCode = "404", description = "User not found")
    fun deleteUserData(
        @PathVariable userId: UUID,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<GdprDeletionResponse> = ResponseEntity.ok(
        gdprService.requestDeletion(userId, UUID.fromString(actorId)),
    )
}
