package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.EmployeeConfigRequest
import com.zeiterfassung.model.dto.EmployeeConfigResponse
import com.zeiterfassung.service.EmployeeConfigService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Employee Config")
@SecurityRequirement(name = "bearerAuth")
class EmployeeConfigController(
    private val employeeConfigService: EmployeeConfigService,
) {
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage') or #userId.toString() == authentication.principal")
    @Operation(summary = "Get employee configuration", description = "Returns the work configuration for the specified employee.")
    @ApiResponse(responseCode = "200", description = "Employee configuration returned")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    fun getConfig(
        @PathVariable userId: UUID,
    ): ResponseEntity<EmployeeConfigResponse> = ResponseEntity.ok(employeeConfigService.getConfig(userId))

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(
        summary = "Update employee configuration",
        description = "Updates work hours, vacation days, and other configuration for an employee.",
    )
    @ApiResponse(responseCode = "200", description = "Employee configuration updated")
    fun updateConfig(
        @PathVariable userId: UUID,
        @RequestBody request: EmployeeConfigRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<EmployeeConfigResponse> =
        ResponseEntity.ok(employeeConfigService.updateConfig(UUID.fromString(actorId), userId, request))
}
