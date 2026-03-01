package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.DuplicateResourceException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.AssignRolesRequest
import com.zeiterfassung.model.dto.ChangePasswordRequest
import com.zeiterfassung.model.dto.CreateUserRequest
import com.zeiterfassung.model.dto.PageResponse
import com.zeiterfassung.model.dto.ResetPasswordRequest
import com.zeiterfassung.model.dto.UpdateRfidRequest
import com.zeiterfassung.model.dto.UpdateUserRequest
import com.zeiterfassung.model.dto.UserResponse
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.RoleRepository
import com.zeiterfassung.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
) {
    @Transactional
    fun createUser(
        request: CreateUserRequest,
        actorId: UUID,
    ): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("Email already exists: ${request.email}")
        }

        val roles = roleRepository.findByNameIn(request.roles)

        val managerId = request.managerId?.let { UUID.fromString(it) }
        val manager =
            managerId?.let {
                userRepository.findById(it).orElseThrow { ResourceNotFoundException("Manager not found: $it") }
            }

        val user =
            UserEntity(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                firstName = request.firstName,
                lastName = request.lastName,
                employeeNumber = request.employeeNumber,
                phone = request.phone,
                manager = manager,
            )
        user.roles.addAll(roles)

        val saved = userRepository.save(user)
        auditService.logDataChange(actorId, "USER_CREATED", "User", saved.id, null, saved.toUserResponse())
        return saved.toUserResponse()
    }

    fun getUsers(pageable: Pageable): PageResponse<UserResponse> {
        val page = userRepository.findByIsDeletedFalse(pageable)
        return PageResponse(
            content = page.content.map { it.toUserResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            pageNumber = page.number,
            pageSize = page.size,
        )
    }

    fun getUserById(id: UUID): UserResponse {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        if (user.isDeleted) throw ResourceNotFoundException("User not found: $id")
        return user.toUserResponse()
    }

    @Transactional
    fun updateUser(
        id: UUID,
        request: UpdateUserRequest,
        actorId: UUID,
    ): UserResponse {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        if (user.isDeleted) throw ResourceNotFoundException("User not found: $id")

        val oldResponse = user.toUserResponse()

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.phone?.let { user.phone = it }
        request.isActive?.let { user.isActive = it }
        request.dateFormat?.let { user.dateFormat = it }
        request.timeFormat?.let { user.timeFormat = it }
        request.managerId?.let { managerIdStr ->
            val manager =
                userRepository
                    .findById(UUID.fromString(managerIdStr))
                    .orElseThrow { ResourceNotFoundException("Manager not found: $managerIdStr") }
            user.manager = manager
        }

        val saved = userRepository.save(user)
        auditService.logDataChange(actorId, "USER_UPDATED", "User", saved.id, oldResponse, saved.toUserResponse())
        return saved.toUserResponse()
    }

    @Transactional
    fun deleteUser(
        id: UUID,
        actorId: UUID,
    ) {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        val oldResponse = user.toUserResponse()
        user.isDeleted = true
        user.isActive = false
        userRepository.save(user)
        auditService.logDataChange(actorId, "USER_DELETED", "User", id, oldResponse, null)
    }

    @Transactional
    fun assignRoles(
        id: UUID,
        request: AssignRolesRequest,
        actorId: UUID,
    ): UserResponse {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        val oldRoles = user.roles.map { it.name }

        val newRoles = roleRepository.findByNameIn(request.roles)
        user.roles.clear()
        user.roles.addAll(newRoles)
        val saved = userRepository.save(user)

        auditService.logPermissionChange(actorId, "User", id, oldRoles, request.roles)
        return saved.toUserResponse()
    }

    @Transactional
    fun updateRfid(
        id: UUID,
        request: UpdateRfidRequest,
        actorId: UUID,
    ): UserResponse {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        user.rfidTagId = request.rfidTagId
        val saved = userRepository.save(user)
        auditService.logDataChange(actorId, "RFID_UPDATED", "User", id, null, null)
        return saved.toUserResponse()
    }

    @Transactional
    fun changePassword(
        id: UUID,
        request: ChangePasswordRequest,
        actorId: UUID,
    ) {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw UnauthorizedException("Current password is incorrect")
        }
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
    }

    @Transactional
    fun resetPassword(
        id: UUID,
        request: ResetPasswordRequest,
        actorId: UUID,
    ) {
        val user =
            userRepository
                .findById(id)
                .orElseThrow { ResourceNotFoundException("User not found: $id") }
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
        auditService.logDataChange(actorId, "PASSWORD_RESET", "User", id, null, null)
    }

    fun getTeamMembers(id: UUID): List<UserResponse> = userRepository.findByManagerId(id).map { it.toUserResponse() }

    private fun UserEntity.toUserResponse(): UserResponse {
        val roles = this.roles.map { it.name }
        val permissions = this.roles.flatMap { it.permissions.map { p -> p.name } }.distinct()
        return UserResponse(
            id = this.id.toString(),
            email = this.email,
            firstName = this.firstName,
            lastName = this.lastName,
            employeeNumber = this.employeeNumber,
            phone = this.phone,
            photoUrl = this.photoUrl,
            managerId = this.manager?.id?.toString(),
            isActive = this.isActive,
            roles = roles,
            permissions = permissions,
            dateFormat = this.dateFormat,
            timeFormat = this.timeFormat,
        )
    }
}
