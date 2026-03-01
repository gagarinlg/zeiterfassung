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
@RequestMapping("/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Create user", description = "Creates a new user account. Requires admin permission.")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> {
        val response = userService.createUser(request, UUID.fromString(actorId))
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "List users", description = "Returns a paginated list of all users.")
    @ApiResponse(responseCode = "200", description = "Users returned")
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
    @Operation(summary = "Get user by ID", description = "Returns a user by their unique ID.")
    @ApiResponse(responseCode = "200", description = "User returned")
    @ApiResponse(responseCode = "404", description = "User not found")
    fun getUserById(
        @PathVariable id: UUID,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.getUserById(id))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Update user", description = "Updates an existing user's profile information.")
    @ApiResponse(responseCode = "200", description = "User updated")
    @ApiResponse(responseCode = "404", description = "User not found")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateUser(id, request, UUID.fromString(actorId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Delete user", description = "Soft-deletes a user account.")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    fun deleteUser(
        @PathVariable id: UUID,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        userService.deleteUser(id, UUID.fromString(actorId))
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Assign roles", description = "Assigns roles to a user.")
    @ApiResponse(responseCode = "200", description = "Roles assigned")
    fun assignRoles(
        @PathVariable id: UUID,
        @RequestBody request: AssignRolesRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.assignRoles(id, request, UUID.fromString(actorId)))

    @PutMapping("/{id}/rfid")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @Operation(summary = "Update RFID tag", description = "Updates the RFID tag ID for a user.")
    @ApiResponse(responseCode = "200", description = "RFID tag updated")
    fun updateRfid(
        @PathVariable id: UUID,
        @RequestBody request: UpdateRfidRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateRfid(id, request, UUID.fromString(actorId)))

    @PutMapping("/{id}/password")
    @PreAuthorize("#id.toString() == authentication.principal or hasAuthority('admin.users.manage')")
    @Operation(summary = "Change password", description = "Changes a user's password. Users can change their own; admins can change any.")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Current password incorrect")
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
    @Operation(summary = "Reset user password", description = "Admin resets a user's password to a new value.")
    @ApiResponse(responseCode = "204", description = "Password reset")
    fun resetPassword(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ResetPasswordRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        userService.resetPassword(id, request, UUID.fromString(actorId))
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile", description = "Updates the authenticated user's own profile (name, phone, preferences).")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    fun updateOwnProfile(
        @Valid @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.updateOwnProfile(UUID.fromString(actorId), request))

    @GetMapping("/{id}/all-subordinates")
    @PreAuthorize("hasAuthority('admin.users.manage') or hasAuthority('time.view.team')")
    @Operation(summary = "Get all subordinates", description = "Returns the full hierarchy of subordinates for a manager.")
    @ApiResponse(responseCode = "200", description = "Subordinates returned")
    fun getAllSubordinates(
        @PathVariable id: UUID,
    ): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getAllSubordinates(id))

    @GetMapping("/{id}/team")
    @PreAuthorize("hasAuthority('admin.users.manage') or hasAuthority('time.view.team')")
    @Operation(summary = "Get team members", description = "Returns direct team members for a manager.")
    @ApiResponse(responseCode = "200", description = "Team members returned")
    fun getTeamMembers(
        @PathVariable id: UUID,
    ): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getTeamMembers(id))
}
