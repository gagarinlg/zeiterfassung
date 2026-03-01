package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AssignRolesRequest
import com.zeiterfassung.model.dto.ChangePasswordRequest
import com.zeiterfassung.model.dto.CreateUserRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.ResetPasswordRequest
import com.zeiterfassung.model.dto.UpdateRfidRequest
import com.zeiterfassung.model.dto.UpdateUserRequest
import com.zeiterfassung.model.dto.UserResponse
import com.zeiterfassung.service.UserService
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
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> {
        val response = userService.createUser(request, UUID.fromString(actorId))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun getUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "lastName") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
    ): ResponseEntity<PageResponse<UserResponse>> {
        val sort = if (sortDir == "desc") Sort.by(sortBy).descending() else Sort.by(sortBy).ascending()
        val pageable = PageRequest.of(page, size, sort)
        return ResponseEntity.ok(userService.getUsers(pageable))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin.users.manage') or #id.toString() == authentication.principal")
    fun getUserById(
        @PathVariable id: UUID,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.getUserById(id))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateUser(id, request, UUID.fromString(actorId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun deleteUser(
        @PathVariable id: UUID,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        userService.deleteUser(id, UUID.fromString(actorId))
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun assignRoles(
        @PathVariable id: UUID,
        @RequestBody request: AssignRolesRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.assignRoles(id, request, UUID.fromString(actorId)))

    @PutMapping("/{id}/rfid")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun updateRfid(
        @PathVariable id: UUID,
        @RequestBody request: UpdateRfidRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateRfid(id, request, UUID.fromString(actorId)))

    @PutMapping("/{id}/password")
    @PreAuthorize("#id.toString() == authentication.principal or hasAuthority('admin.users.manage')")
    fun changePassword(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangePasswordRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        userService.changePassword(id, request, UUID.fromString(actorId))
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    fun resetPassword(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ResetPasswordRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        userService.resetPassword(id, request, UUID.fromString(actorId))
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/me")
    fun updateOwnProfile(
        @Valid @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateOwnProfile(UUID.fromString(actorId), request))

    @GetMapping("/{id}/all-subordinates")
    @PreAuthorize("hasAuthority('admin.users.manage') or hasAuthority('time.view.team')")
    fun getAllSubordinates(
        @PathVariable id: UUID,
    ): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getAllSubordinates(id))

    @GetMapping("/{id}/team")
    @PreAuthorize("hasAuthority('admin.users.manage') or hasAuthority('time.view.team')")
    fun getTeamMembers(
        @PathVariable id: UUID,
    ): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getTeamMembers(id))
}
