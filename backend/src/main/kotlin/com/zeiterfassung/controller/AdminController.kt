package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AuditLogResponse
import com.zeiterfassung.model.dto.LdapConfigResponse
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.SystemSettingResponse
import com.zeiterfassung.model.dto.TestMailRequest
import com.zeiterfassung.model.dto.UpdateLdapConfigRequest
import com.zeiterfassung.model.dto.UpdateSystemSettingRequest
import com.zeiterfassung.service.AdminService
import com.zeiterfassung.service.EmailService
import com.zeiterfassung.service.LdapService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('admin.users.manage')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
class AdminController(
    private val adminService: AdminService,
    private val ldapService: LdapService,
    private val emailService: EmailService,
) {
    @GetMapping("/audit-log")
    @Operation(summary = "Get audit log", description = "Returns a paginated audit log of system actions.")
    @ApiResponse(responseCode = "200", description = "Audit log entries returned")
    fun getAuditLog(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<PageResponse<AuditLogResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return ResponseEntity.ok(adminService.getAuditLog(pageable))
    }

    @GetMapping("/audit-log/user/{userId}")
    @Operation(summary = "Get audit log by user", description = "Returns audit log entries filtered by a specific user.")
    @ApiResponse(responseCode = "200", description = "Filtered audit log entries returned")
    fun getAuditLogByUser(
        @PathVariable userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<PageResponse<AuditLogResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return ResponseEntity.ok(adminService.getAuditLogByUser(userId, pageable))
    }

    @GetMapping("/settings")
    @Operation(summary = "Get system settings", description = "Returns all system settings as key-value pairs.")
    @ApiResponse(responseCode = "200", description = "System settings returned")
    fun getSystemSettings(): ResponseEntity<List<SystemSettingResponse>> = ResponseEntity.ok(adminService.getSystemSettings())

    @PutMapping("/settings/{key}")
    @Operation(summary = "Update system setting", description = "Updates a single system setting by key.")
    @ApiResponse(responseCode = "200", description = "Setting updated successfully")
    fun updateSystemSetting(
        @PathVariable key: String,
        @RequestBody request: UpdateSystemSettingRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<SystemSettingResponse> = ResponseEntity.ok(adminService.updateSystemSetting(key, request, UUID.fromString(actorId)))

    @GetMapping("/ldap")
    @Operation(summary = "Get LDAP configuration", description = "Returns the current LDAP/Active Directory configuration.")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @ApiResponse(responseCode = "200", description = "LDAP configuration returned")
    fun getLdapConfig(): ResponseEntity<LdapConfigResponse> = ResponseEntity.ok(ldapService.getLdapConfig())

    @PutMapping("/ldap")
    @Operation(summary = "Update LDAP configuration", description = "Updates the LDAP/Active Directory configuration.")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @ApiResponse(responseCode = "200", description = "LDAP configuration updated")
    fun updateLdapConfig(
        @RequestBody request: UpdateLdapConfigRequest,
    ): ResponseEntity<LdapConfigResponse> = ResponseEntity.ok(ldapService.updateLdapConfig(request))

    @PostMapping("/mail/test")
    @Operation(summary = "Send test email", description = "Sends a test email to verify mail configuration.")
    @ApiResponse(responseCode = "200", description = "Test email sent successfully")
    @ApiResponse(responseCode = "400", description = "Mail is not configured")
    @ApiResponse(responseCode = "500", description = "Failed to send email")
    fun sendTestMail(
        @RequestBody request: TestMailRequest,
    ): ResponseEntity<Map<String, String>> =
        try {
            emailService.sendTestMail(request.recipientEmail)
            ResponseEntity.ok(mapOf("status" to "ok", "message" to "Test email sent successfully"))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("status" to "error", "message" to (e.message ?: "Mail is not configured")))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("status" to "error", "message" to (e.message ?: "Unknown error")))
        }
}
