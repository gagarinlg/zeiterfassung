package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.DuplicateResourceException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateUserRequest
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @Mock private lateinit var auditService: AuditService

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var userService: UserService
    private val actorId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, roleRepository, passwordEncoder, auditService)
    }

    @Test
    fun `createUser should throw DuplicateResourceException when email exists`() {
        `when`(userRepository.existsByEmail("test@test.com")).thenReturn(true)
        assertThrows<DuplicateResourceException> {
            userService.createUser(
                CreateUserRequest("test@test.com", "password123", "First", "Last"),
                actorId,
            )
        }
    }

    @Test
    fun `createUser should create user successfully`() {
        val email = "new@test.com"
        `when`(userRepository.existsByEmail(email)).thenReturn(false)
        `when`(roleRepository.findByNameIn(listOf("EMPLOYEE"))).thenReturn(emptyList())
        val savedUser =
            UserEntity(
                email = email,
                passwordHash = "hash",
                firstName = "First",
                lastName = "Last",
            )
        `when`(userRepository.save(any(UserEntity::class.java) ?: savedUser)).thenReturn(savedUser)

        val result =
            userService.createUser(
                CreateUserRequest(email, "password123", "First", "Last"),
                actorId,
            )
        assertThat(result.email).isEqualTo(email)
    }

    @Test
    fun `getUserById should throw ResourceNotFoundException for non-existent user`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> {
            userService.getUserById(id)
        }
    }

    @Test
    fun `getUsers should return paginated results`() {
        val users =
            listOf(
                UserEntity(email = "a@test.com", passwordHash = "h", firstName = "A", lastName = "B"),
            )
        val page = PageImpl(users)
        `when`(userRepository.findByIsDeletedFalse(any(Pageable::class.java) ?: PageRequest.of(0, 20))).thenReturn(page)

        val result = userService.getUsers(PageRequest.of(0, 20))
        assertThat(result.content).hasSize(1)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `deleteUser should soft-delete user`() {
        val user =
            UserEntity(
                email = "del@test.com",
                passwordHash = "h",
                firstName = "Del",
                lastName = "User",
            )
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any(UserEntity::class.java) ?: user)).thenReturn(user)

        userService.deleteUser(user.id, actorId)

        assertThat(user.isDeleted).isTrue()
        assertThat(user.isActive).isFalse()
    }
}
