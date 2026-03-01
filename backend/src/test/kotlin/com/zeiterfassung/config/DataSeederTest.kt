package com.zeiterfassung.config

import com.zeiterfassung.model.entity.RoleEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.RoleRepository
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DataSeederTest {
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var roleRepository: RoleRepository

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var dataSeeder: DataSeeder

    @BeforeEach
    fun setUp() {
        dataSeeder = DataSeeder(
            userRepository = userRepository,
            roleRepository = roleRepository,
            passwordEncoder = passwordEncoder,
            adminPassword = "TestAdmin@123!",
        )
    }

    @Test
    fun `seed skips when super admin role does not exist`() {
        `when`(userRepository.existsByEmail("admin@zeiterfassung.local")).thenReturn(false)
        `when`(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.empty())

        dataSeeder.run(DefaultApplicationArguments())

        verify(userRepository, never()).save(any())
    }

    @Test
    fun `seed skips when admin already exists`() {
        `when`(userRepository.existsByEmail("admin@zeiterfassung.local")).thenReturn(true)

        dataSeeder.run(DefaultApplicationArguments())

        verify(userRepository, never()).save(any())
        verify(roleRepository, never()).findByName(any() ?: "SUPER_ADMIN")
    }

    @Test
    fun `seed creates admin when role exists and admin does not exist`() {
        val superAdminRole = RoleEntity(name = "SUPER_ADMIN")
        `when`(userRepository.existsByEmail("admin@zeiterfassung.local")).thenReturn(false)
        `when`(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole))
        `when`(userRepository.save(any(UserEntity::class.java) ?: UserEntity(
            email = "", passwordHash = "", firstName = "", lastName = "",
        ))).thenAnswer { it.arguments[0] }

        dataSeeder.run(DefaultApplicationArguments())

        val captor = ArgumentCaptor.forClass(UserEntity::class.java)
        verify(userRepository).save(captor.capture())
        val savedUser = captor.value

        assertThat(savedUser.email).isEqualTo("admin@zeiterfassung.local")
        assertThat(savedUser.firstName).isEqualTo("Super")
        assertThat(savedUser.lastName).isEqualTo("Admin")
        assertThat(savedUser.employeeNumber).isEqualTo("ADMIN-001")
        assertThat(savedUser.roles).contains(superAdminRole)
        assertThat(passwordEncoder.matches("TestAdmin@123!", savedUser.passwordHash)).isTrue()
    }
}
