package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.entity.AuditLogEntity
import com.zeiterfassung.model.entity.BusinessTripEntity
import com.zeiterfassung.model.entity.SickLeaveEntity
import com.zeiterfassung.model.entity.TimeEntryEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.entity.VacationRequestEntity
import com.zeiterfassung.model.enums.BusinessTripStatus
import com.zeiterfassung.model.enums.SickLeaveStatus
import com.zeiterfassung.model.enums.TimeEntryType
import com.zeiterfassung.model.enums.VacationStatus
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.BusinessTripRepository
import com.zeiterfassung.repository.RefreshTokenRepository
import com.zeiterfassung.repository.SickLeaveRepository
import com.zeiterfassung.repository.TimeEntryRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.VacationRequestRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GdprServiceTest {
    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var timeEntryRepository: TimeEntryRepository

    @Mock private lateinit var vacationRequestRepository: VacationRequestRepository

    @Mock private lateinit var sickLeaveRepository: SickLeaveRepository

    @Mock private lateinit var businessTripRepository: BusinessTripRepository

    @Mock private lateinit var auditLogRepository: AuditLogRepository

    @Mock private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock private lateinit var auditService: AuditService

    private lateinit var service: GdprService

    private lateinit var user: UserEntity
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            GdprService(
                userRepository,
                timeEntryRepository,
                vacationRequestRepository,
                sickLeaveRepository,
                businessTripRepository,
                auditLogRepository,
                refreshTokenRepository,
                auditService,
            )
        user =
            UserEntity(
                id = userId,
                email = "user@test.com",
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                phone = "+49123456",
                employeeNumber = "EMP-001",
            )
    }

    // ---- exportUserData ----

    @Test
    fun `exportUserData returns complete data`() {
        val timeEntry = TimeEntryEntity(user = user, entryType = TimeEntryType.CLOCK_IN, timestamp = Instant.now())
        val vacation =
            VacationRequestEntity(
                user = user,
                startDate = LocalDate.now().plusDays(10),
                endDate = LocalDate.now().plusDays(14),
                totalDays = BigDecimal("5"),
                status = VacationStatus.PENDING,
            )
        val sickLeave =
            SickLeaveEntity(
                user = user,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(3),
                status = SickLeaveStatus.REPORTED,
            )
        val businessTrip =
            BusinessTripEntity(
                user = user,
                startDate = LocalDate.now().plusDays(20),
                endDate = LocalDate.now().plusDays(22),
                destination = "Berlin",
                purpose = "Conference",
                status = BusinessTripStatus.REQUESTED,
            )
        val auditEntry =
            AuditLogEntity(
                user = user,
                action = "LOGIN",
                entityType = "User",
                entityId = userId,
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdOrderByTimestampAsc(userId)).thenReturn(listOf(timeEntry))
        `when`(vacationRequestRepository.findByUserId(userId, Pageable.unpaged())).thenReturn(PageImpl(listOf(vacation)))
        `when`(sickLeaveRepository.findByUserId(userId, Pageable.unpaged())).thenReturn(PageImpl(listOf(sickLeave)))
        `when`(businessTripRepository.findByUserId(userId, Pageable.unpaged())).thenReturn(PageImpl(listOf(businessTrip)))
        `when`(auditLogRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(listOf(auditEntry))

        val result = service.exportUserData(userId)

        assertThat(result.personalInfo.email).isEqualTo("user@test.com")
        assertThat(result.personalInfo.firstName).isEqualTo("John")
        assertThat(result.timeEntries).hasSize(1)
        assertThat(result.vacationRequests).hasSize(1)
        assertThat(result.sickLeaves).hasSize(1)
        assertThat(result.businessTrips).hasSize(1)
        assertThat(result.auditLog).hasSize(1)
    }

    @Test
    fun `exportUserData throws when user not found`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.exportUserData(id) }
    }

    @Test
    fun `exportUserData returns empty lists when no data`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(timeEntryRepository.findByUserIdOrderByTimestampAsc(userId)).thenReturn(emptyList())
        `when`(vacationRequestRepository.findByUserId(userId, Pageable.unpaged())).thenReturn(PageImpl(emptyList()))
        `when`(sickLeaveRepository.findByUserId(userId, Pageable.unpaged())).thenReturn(PageImpl(emptyList()))
        `when`(businessTripRepository.findByUserId(userId, Pageable.unpaged())).thenReturn(PageImpl(emptyList()))
        `when`(auditLogRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(emptyList())

        val result = service.exportUserData(userId)

        assertThat(result.personalInfo.email).isEqualTo("user@test.com")
        assertThat(result.timeEntries).isEmpty()
        assertThat(result.vacationRequests).isEmpty()
        assertThat(result.sickLeaves).isEmpty()
        assertThat(result.businessTrips).isEmpty()
        assertThat(result.auditLog).isEmpty()
    }

    // ---- requestDeletion ----

    @Test
    fun `requestDeletion anonymizes user data`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any())).thenAnswer { it.arguments[0] as UserEntity }

        val result = service.requestDeletion(userId, userId)

        assertThat(result.status).isEqualTo("COMPLETED")
        assertThat(user.firstName).isEqualTo("Deleted")
        assertThat(user.lastName).isEqualTo("User")
        assertThat(user.email).contains("anonymized.invalid")
        assertThat(user.phone).isNull()
        assertThat(user.rfidTagId).isNull()
        assertThat(user.employeeNumber).isNull()
    }

    @Test
    fun `requestDeletion throws when user not found`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.requestDeletion(id, id) }
    }

    @Test
    fun `requestDeletion sets isDeleted and isActive flags`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any())).thenAnswer { it.arguments[0] as UserEntity }

        service.requestDeletion(userId, userId)

        assertThat(user.isDeleted).isTrue()
        assertThat(user.isActive).isFalse()
    }

    @Test
    fun `requestDeletion revokes refresh tokens`() {
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any())).thenAnswer { it.arguments[0] as UserEntity }
        `when`(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(2)

        val result = service.requestDeletion(userId, userId)

        assertThat(result.status).isEqualTo("COMPLETED")
    }
}
