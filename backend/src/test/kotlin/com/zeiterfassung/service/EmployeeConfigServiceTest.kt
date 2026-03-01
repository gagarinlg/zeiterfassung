package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.EmployeeConfigRequest
import com.zeiterfassung.model.entity.EmployeeConfigEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EmployeeConfigServiceTest {
    @Mock
    private lateinit var employeeConfigRepository: EmployeeConfigRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var auditService: AuditService

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var service: EmployeeConfigService

    private lateinit var user: UserEntity
    private val userId = UUID.randomUUID()
    private val adminUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            EmployeeConfigService(
                employeeConfigRepository,
                userRepository,
                auditService,
                objectMapper,
            )
        user =
            UserEntity(
                id = userId,
                email = "test@test.com",
                passwordHash = "hash",
                firstName = "Test",
                lastName = "User",
            )
    }

    @Test
    fun `getConfig returns default config when no config exists`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(
            employeeConfigRepository.save(any(EmployeeConfigEntity::class.java) ?: EmployeeConfigEntity(user = user)),
        ).thenAnswer { it.arguments[0] as EmployeeConfigEntity }

        val result = service.getConfig(userId)
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.weeklyWorkHours).isEqualByComparingTo(BigDecimal("40.00"))
        assertThat(result.dailyWorkHours).isEqualByComparingTo(BigDecimal("8.00"))
        assertThat(result.workDays).containsExactly(1, 2, 3, 4, 5)
        assertThat(result.vacationDaysPerYear).isEqualTo(30)
    }

    @Test
    fun `getConfig returns existing config`() {
        val config =
            EmployeeConfigEntity(
                user = user,
                weeklyWorkHours = BigDecimal("30.00"),
                dailyWorkHours = BigDecimal("6.00"),
                workDays = "[1,2,3]",
                vacationDaysPerYear = 25,
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)

        val result = service.getConfig(userId)
        assertThat(result.weeklyWorkHours).isEqualByComparingTo(BigDecimal("30.00"))
        assertThat(result.dailyWorkHours).isEqualByComparingTo(BigDecimal("6.00"))
        assertThat(result.workDays).containsExactly(1, 2, 3)
        assertThat(result.vacationDaysPerYear).isEqualTo(25)
    }

    @Test
    fun `getConfig throws when user not found`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getConfig(userId)
        }
    }

    @Test
    fun `updateConfig creates new config for user`() {
        val request =
            EmployeeConfigRequest(
                weeklyWorkHours = BigDecimal("35.00"),
                dailyWorkHours = BigDecimal("7.00"),
                workDays = listOf(1, 2, 3, 4),
                vacationDaysPerYear = 28,
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(
            employeeConfigRepository.save(any(EmployeeConfigEntity::class.java) ?: EmployeeConfigEntity(user = user)),
        ).thenAnswer { it.arguments[0] as EmployeeConfigEntity }

        val result = service.updateConfig(adminUserId, userId, request)
        assertThat(result.weeklyWorkHours).isEqualByComparingTo(BigDecimal("35.00"))
        assertThat(result.dailyWorkHours).isEqualByComparingTo(BigDecimal("7.00"))
        assertThat(result.workDays).containsExactly(1, 2, 3, 4)
        assertThat(result.vacationDaysPerYear).isEqualTo(28)
    }

    @Test
    fun `updateConfig updates existing config`() {
        val existingConfig =
            EmployeeConfigEntity(
                user = user,
                weeklyWorkHours = BigDecimal("40.00"),
                dailyWorkHours = BigDecimal("8.00"),
                vacationDaysPerYear = 30,
            )
        val request =
            EmployeeConfigRequest(
                vacationDaysPerYear = 25,
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(existingConfig)
        `when`(
            employeeConfigRepository.save(any(EmployeeConfigEntity::class.java) ?: existingConfig),
        ).thenAnswer { it.arguments[0] as EmployeeConfigEntity }

        val result = service.updateConfig(adminUserId, userId, request)
        assertThat(result.vacationDaysPerYear).isEqualTo(25)
        // Unchanged fields should remain
        assertThat(result.weeklyWorkHours).isEqualByComparingTo(BigDecimal("40.00"))
    }

    @Test
    fun `updateConfig throws when user not found`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.updateConfig(adminUserId, userId, EmployeeConfigRequest())
        }
    }

    @Test
    fun `updateConfig handles partial updates with only some fields set`() {
        val existingConfig =
            EmployeeConfigEntity(
                user = user,
                weeklyWorkHours = BigDecimal("40.00"),
                dailyWorkHours = BigDecimal("8.00"),
                vacationDaysPerYear = 30,
                isHomeOfficeEligible = false,
            )
        val request =
            EmployeeConfigRequest(
                isHomeOfficeEligible = true,
                contractStartDate = LocalDate.of(2024, 1, 1),
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(existingConfig)
        `when`(
            employeeConfigRepository.save(any(EmployeeConfigEntity::class.java) ?: existingConfig),
        ).thenAnswer { it.arguments[0] as EmployeeConfigEntity }

        val result = service.updateConfig(adminUserId, userId, request)
        assertThat(result.isHomeOfficeEligible).isTrue()
        assertThat(result.contractStartDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(result.weeklyWorkHours).isEqualByComparingTo(BigDecimal("40.00"))
        assertThat(result.vacationDaysPerYear).isEqualTo(30)
    }

    @Test
    fun `toResponse handles invalid workDays JSON gracefully`() {
        val config =
            EmployeeConfigEntity(
                user = user,
                workDays = "not-valid-json",
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)

        val result = service.getConfig(userId)
        // Should fall back to default work days
        assertThat(result.workDays).containsExactly(1, 2, 3, 4, 5)
    }
}
