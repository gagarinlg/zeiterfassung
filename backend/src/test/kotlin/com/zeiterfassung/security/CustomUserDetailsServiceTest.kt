package com.zeiterfassung.security

import com.zeiterfassung.model.entity.PermissionEntity
import com.zeiterfassung.model.entity.RoleEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CustomUserDetailsServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var service: CustomUserDetailsService

    @BeforeEach
    fun setUp() {
        service = CustomUserDetailsService(userRepository)
    }

    @Test
    fun `loadUserByUsername should return UserDetails for valid user`() {
        val user =
            UserEntity(
                email = "test@example.com",
                passwordHash = "hashedPassword",
                firstName = "Max",
                lastName = "Mustermann",
            )
        val permission = PermissionEntity(name = "time.edit.own")
        val role = RoleEntity(name = "EMPLOYEE", permissions = mutableSetOf(permission))
        user.roles.add(role)

        `when`(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("test@example.com")

        assertThat(userDetails.username).isEqualTo(user.id.toString())
        assertThat(userDetails.password).isEqualTo("hashedPassword")
        assertThat(userDetails.isEnabled).isTrue()
        assertThat(userDetails.isAccountNonLocked).isTrue()
        val authorityNames = userDetails.authorities.map { it.authority }
        assertThat(authorityNames).contains("ROLE_EMPLOYEE")
        assertThat(authorityNames).contains("time.edit.own")
    }

    @Test
    fun `loadUserByUsername should throw UsernameNotFoundException when user not found`() {
        `when`(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty())

        val ex =
            assertThrows<UsernameNotFoundException> {
                service.loadUserByUsername("nonexistent@example.com")
            }
        assertThat(ex.message).contains("nonexistent@example.com")
    }

    @Test
    fun `loadUserByUsername should return disabled UserDetails for inactive user`() {
        val user =
            UserEntity(
                email = "inactive@example.com",
                passwordHash = "hash",
                firstName = "Inactive",
                lastName = "User",
                isActive = false,
            )
        `when`(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("inactive@example.com")

        assertThat(userDetails.isEnabled).isFalse()
    }

    @Test
    fun `loadUserByUsername should return disabled UserDetails for deleted user`() {
        val user =
            UserEntity(
                email = "deleted@example.com",
                passwordHash = "hash",
                firstName = "Deleted",
                lastName = "User",
                isDeleted = true,
            )
        `when`(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("deleted@example.com")

        assertThat(userDetails.isEnabled).isFalse()
    }

    @Test
    fun `loadUserByUsername should return locked UserDetails when lockedUntil is in the future`() {
        val user =
            UserEntity(
                email = "locked@example.com",
                passwordHash = "hash",
                firstName = "Locked",
                lastName = "User",
            )
        user.lockedUntil = Instant.now().plusSeconds(3600)
        `when`(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("locked@example.com")

        assertThat(userDetails.isAccountNonLocked).isFalse()
    }

    @Test
    fun `loadUserByUsername should return unlocked UserDetails when lockedUntil is in the past`() {
        val user =
            UserEntity(
                email = "unlocked@example.com",
                passwordHash = "hash",
                firstName = "Unlocked",
                lastName = "User",
            )
        user.lockedUntil = Instant.now().minusSeconds(3600)
        `when`(userRepository.findByEmail("unlocked@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("unlocked@example.com")

        assertThat(userDetails.isAccountNonLocked).isTrue()
    }

    @Test
    fun `loadUserByUsername should include multiple roles and permissions`() {
        val user =
            UserEntity(
                email = "admin@example.com",
                passwordHash = "hash",
                firstName = "Admin",
                lastName = "User",
            )
        val perm1 = PermissionEntity(name = "time.edit.own")
        val perm2 = PermissionEntity(name = "admin.users.manage")
        val perm3 = PermissionEntity(name = "vacation.approve")
        val roleEmployee = RoleEntity(name = "EMPLOYEE", permissions = mutableSetOf(perm1))
        val roleAdmin = RoleEntity(name = "ADMIN", permissions = mutableSetOf(perm2, perm3))
        user.roles.addAll(listOf(roleEmployee, roleAdmin))

        `when`(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("admin@example.com")

        val authorityNames = userDetails.authorities.map { it.authority }
        assertThat(authorityNames).contains("ROLE_EMPLOYEE", "ROLE_ADMIN")
        assertThat(authorityNames).contains("time.edit.own", "admin.users.manage", "vacation.approve")
    }

    @Test
    fun `loadUserByUsername should handle user with no roles`() {
        val user =
            UserEntity(
                email = "noroles@example.com",
                passwordHash = "hash",
                firstName = "No",
                lastName = "Roles",
            )
        `when`(userRepository.findByEmail("noroles@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("noroles@example.com")

        assertThat(userDetails.authorities).isEmpty()
    }

    @Test
    fun `loadUserByUsername should return unlocked UserDetails when lockedUntil is null`() {
        val user =
            UserEntity(
                email = "normal@example.com",
                passwordHash = "hash",
                firstName = "Normal",
                lastName = "User",
            )
        `when`(userRepository.findByEmail("normal@example.com")).thenReturn(Optional.of(user))

        val userDetails = service.loadUserByUsername("normal@example.com")

        assertThat(userDetails.isAccountNonLocked).isTrue()
    }
}
