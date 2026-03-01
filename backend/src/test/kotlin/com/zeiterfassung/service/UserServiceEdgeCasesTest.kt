package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.AssignRolesRequest
import com.zeiterfassung.model.dto.ChangePasswordRequest
import com.zeiterfassung.model.dto.ResetPasswordRequest
import com.zeiterfassung.model.dto.UpdateRfidRequest
import com.zeiterfassung.model.dto.UpdateUserRequest
import com.zeiterfassung.model.entity.RoleEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.RoleRepository
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceEdgeCasesTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var roleRepository: RoleRepository

    @Mock
    private lateinit var auditService: AuditService

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var userService: UserService
    private val actorId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, roleRepository, passwordEncoder, auditService)
    }

    @Test
    fun `updateUser throws when user not found`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            userService.updateUser(id, UpdateUserRequest(firstName = "New"), actorId)
        }
    }

    @Test
    fun `updateUser throws when user is deleted`() {
        val user = createUser()
        user.isDeleted = true
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))

        assertThrows<ResourceNotFoundException> {
            userService.updateUser(user.id, UpdateUserRequest(firstName = "New"), actorId)
        }
    }

    @Test
    fun `updateUser with manager assignment`() {
        val user = createUser()
        val manager = createUser(email = "manager@test.com")
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.findById(manager.id)).thenReturn(Optional.of(manager))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        val result = userService.updateUser(user.id, UpdateUserRequest(managerId = manager.id.toString()), actorId)
        assertThat(result.managerId).isEqualTo(manager.id.toString())
    }

    @Test
    fun `updateUser with substitute assignment`() {
        val user = createUser()
        val substitute = createUser(email = "substitute@test.com")
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.findById(substitute.id)).thenReturn(Optional.of(substitute))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        val result = userService.updateUser(user.id, UpdateUserRequest(substituteId = substitute.id.toString()), actorId)
        assertThat(result.substituteId).isEqualTo(substitute.id.toString())
    }

    @Test
    fun `updateUser clears substitute when blank substituteId`() {
        val user = createUser()
        val oldSubstitute = createUser(email = "old-sub@test.com")
        user.substitute = oldSubstitute
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        val result = userService.updateUser(user.id, UpdateUserRequest(substituteId = ""), actorId)
        assertThat(result.substituteId).isNull()
    }

    @Test
    fun `assignRoles updates roles correctly`() {
        val user = createUser()
        val adminRole = RoleEntity(name = "ADMIN")
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(roleRepository.findByNameIn(listOf("ADMIN"))).thenReturn(listOf(adminRole))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        val result = userService.assignRoles(user.id, AssignRolesRequest(listOf("ADMIN")), actorId)
        assertThat(result.roles).contains("ADMIN")
    }

    @Test
    fun `updateRfid updates RFID tag`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        userService.updateRfid(user.id, UpdateRfidRequest("NEW-RFID-123"), actorId)
        assertThat(user.rfidTagId).isEqualTo("NEW-RFID-123")
    }

    @Test
    fun `changePassword throws when passwords dont match`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))

        assertThrows<UnauthorizedException> {
            userService.changePassword(
                user.id,
                ChangePasswordRequest("current", "newpass123", "different123"),
                actorId,
            )
        }
    }

    @Test
    fun `changePassword throws when current password is wrong`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))

        assertThrows<UnauthorizedException> {
            userService.changePassword(
                user.id,
                ChangePasswordRequest("wrong-password", "newpass123", "newpass123"),
                actorId,
            )
        }
    }

    @Test
    fun `changePassword succeeds with correct credentials`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        userService.changePassword(
            user.id,
            ChangePasswordRequest("correct-password", "newpass123", "newpass123"),
            actorId,
        )
        assertThat(passwordEncoder.matches("newpass123", user.passwordHash)).isTrue()
    }

    @Test
    fun `resetPassword throws when passwords dont match`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))

        assertThrows<UnauthorizedException> {
            userService.resetPassword(user.id, ResetPasswordRequest("newpass123", "different123"), actorId)
        }
    }

    @Test
    fun `getAllSubordinates returns recursive subordinates`() {
        val managerId = UUID.randomUUID()
        val sub1 = createUser(email = "sub1@test.com")
        val sub2 = createUser(email = "sub2@test.com")

        `when`(userRepository.findByManagerId(managerId)).thenReturn(listOf(sub1))
        `when`(userRepository.findByManagerId(sub1.id)).thenReturn(listOf(sub2))
        `when`(userRepository.findByManagerId(sub2.id)).thenReturn(emptyList())

        val result = userService.getAllSubordinates(managerId)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.email }).containsExactlyInAnyOrder("sub1@test.com", "sub2@test.com")
    }

    @Test
    fun `getEffectiveSubordinateIds includes substitute subordinates`() {
        val manager = createUser(email = "manager@test.com")
        val directSub = createUser(email = "direct@test.com")
        manager.subordinates.add(directSub)

        val otherManager = createUser(email = "other-manager@test.com")
        val otherSub = createUser(email = "other-sub@test.com")
        otherManager.subordinates.add(otherSub)

        `when`(userRepository.findBySubstituteId(manager.id)).thenReturn(listOf(otherManager))

        val result = userService.getEffectiveSubordinateIds(manager)
        assertThat(result).containsExactlyInAnyOrder(directSub.id, otherSub.id)
    }

    @Test
    fun `isSubordinateOrSubstitute returns true for direct subordinate`() {
        val manager = createUser(email = "manager@test.com")
        val sub = createUser(email = "sub@test.com")
        manager.subordinates.add(sub)

        val result = userService.isSubordinateOrSubstitute(manager, sub.id)
        assertThat(result).isTrue()
    }

    @Test
    fun `isSubordinateOrSubstitute returns true for substitute subordinate`() {
        val substitute = createUser(email = "substitute@test.com")
        val otherManager = createUser(email = "other-manager@test.com")
        val sub = createUser(email = "sub@test.com")
        otherManager.subordinates.add(sub)

        `when`(userRepository.findBySubstituteId(substitute.id)).thenReturn(listOf(otherManager))

        val result = userService.isSubordinateOrSubstitute(substitute, sub.id)
        assertThat(result).isTrue()
    }

    @Test
    fun `isSubordinateOrSubstitute returns false for non-subordinate`() {
        val manager = createUser(email = "manager@test.com")
        val randomUserId = UUID.randomUUID()

        `when`(userRepository.findBySubstituteId(manager.id)).thenReturn(emptyList())

        val result = userService.isSubordinateOrSubstitute(manager, randomUserId)
        assertThat(result).isFalse()
    }

    @Test
    fun `updateOwnProfile updates allowed fields`() {
        val user = createUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenAnswer { it.arguments[0] }

        val result =
            userService.updateOwnProfile(
                user.id,
                UpdateUserRequest(
                    firstName = "NewFirst",
                    lastName = "NewLast",
                    phone = "+49123456789",
                    dateFormat = "dd.MM.yyyy",
                    timeFormat = "HH:mm",
                ),
            )
        assertThat(result.firstName).isEqualTo("NewFirst")
        assertThat(result.lastName).isEqualTo("NewLast")
        assertThat(result.phone).isEqualTo("+49123456789")
        assertThat(result.dateFormat).isEqualTo("dd.MM.yyyy")
        assertThat(result.timeFormat).isEqualTo("HH:mm")
    }

    @Test
    fun `updateOwnProfile throws when user not found`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            userService.updateOwnProfile(id, UpdateUserRequest(firstName = "New"))
        }
    }

    private fun createUser(email: String = "test@test.com"): UserEntity =
        UserEntity(
            id = UUID.randomUUID(),
            email = email,
            passwordHash = passwordEncoder.encode("correct-password"),
            firstName = "Test",
            lastName = "User",
        )
}
