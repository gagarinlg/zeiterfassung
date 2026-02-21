package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.EmployeeConfigRequest
import com.zeiterfassung.model.dto.EmployeeConfigResponse
import com.zeiterfassung.service.EmployeeConfigService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/employee-config")
class EmployeeConfigController(
    private val employeeConfigService: EmployeeConfigService,
) {
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage') or #userId.toString() == authentication.principal")
    fun getConfig(
        @PathVariable userId: UUID,
    ): ResponseEntity<EmployeeConfigResponse> = ResponseEntity.ok(employeeConfigService.getConfig(userId))

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun updateConfig(
        @PathVariable userId: UUID,
        @RequestBody request: EmployeeConfigRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<EmployeeConfigResponse> =
        ResponseEntity.ok(employeeConfigService.updateConfig(UUID.fromString(actorId), userId, request))
}
