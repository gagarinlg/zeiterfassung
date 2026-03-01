package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AuditLogResponse
import com.zeiterfassung.model.dto.LdapConfigResponse
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.SystemSettingResponse
import com.zeiterfassung.model.dto.UpdateLdapConfigRequest
import com.zeiterfassung.model.dto.UpdateSystemSettingRequest
import com.zeiterfassung.service.AdminService
import com.zeiterfassung.service.LdapService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('admin.users.manage')")
class AdminController(
    private val adminService: AdminService,
    private val ldapService: LdapService,
) {
    @GetMapping("/audit-log")
    fun getAuditLog(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<PageResponse<AuditLogResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return ResponseEntity.ok(adminService.getAuditLog(pageable))
    }

    @GetMapping("/audit-log/user/{userId}")
    fun getAuditLogByUser(
        @PathVariable userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<PageResponse<AuditLogResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return ResponseEntity.ok(adminService.getAuditLogByUser(userId, pageable))
    }

    @GetMapping("/settings")
    fun getSystemSettings(): ResponseEntity<List<SystemSettingResponse>> = ResponseEntity.ok(adminService.getSystemSettings())

    @PutMapping("/settings/{key}")
    fun updateSystemSetting(
        @PathVariable key: String,
        @RequestBody request: UpdateSystemSettingRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<SystemSettingResponse> = ResponseEntity.ok(adminService.updateSystemSetting(key, request, UUID.fromString(actorId)))

    @GetMapping("/ldap")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun getLdapConfig(): ResponseEntity<LdapConfigResponse> = ResponseEntity.ok(ldapService.getLdapConfig())

    @PutMapping("/ldap")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun updateLdapConfig(
        @RequestBody request: UpdateLdapConfigRequest,
    ): ResponseEntity<LdapConfigResponse> = ResponseEntity.ok(ldapService.updateLdapConfig(request))
}
