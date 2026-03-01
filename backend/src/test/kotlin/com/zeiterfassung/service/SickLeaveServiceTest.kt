package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateSickLeaveRequest
import com.zeiterfassung.model.dto.UpdateSickLeaveRequest
import com.zeiterfassung.model.entity.SickLeaveEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.enums.SickLeaveStatus
import com.zeiterfassung.repository.SickLeaveRepository
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
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SickLeaveServiceTest {
    @Mock private lateinit var sickLeaveRepository: SickLeaveRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var auditService: AuditService

    @Mock private lateinit var notificationService: NotificationService

    private lateinit var service: SickLeaveService

    private lateinit var user: UserEntity
    private lateinit var manager: UserEntity
    private val userId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            SickLeaveService(
                sickLeaveRepository,
                userRepository,
                auditService,
                notificationService,
            )
        user = UserEntity(id = userId, email = "user@test.com", passwordHash = "hash", firstName = "John", lastName = "Doe")
        manager = UserEntity(id = managerId, email = "manager@test.com", passwordHash = "hash", firstName = "Jane", lastName = "Smith")
        user.manager = manager
        manager.subordinates.add(user)
    }

    // ---- reportSickLeave ----

    @Test
    fun `reportSickLeave success`() {
        val dto =
            CreateSickLeaveRequest(
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(3),
                notes = "Flu",
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(sickLeaveRepository.findOverlapping(userId, dto.startDate, dto.endDate)).thenReturn(emptyList())
        `when`(sickLeaveRepository.save(any())).thenAnswer { it.arguments[0] as SickLeaveEntity }

        val result = service.reportSickLeave(userId, dto)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.status).isEqualTo(SickLeaveStatus.REPORTED)
        assertThat(result.startDate).isEqualTo(dto.startDate)
        assertThat(result.endDate).isEqualTo(dto.endDate)
    }

    @Test
    fun `reportSickLeave rejects invalid dates (end before start)`() {
        val dto =
            CreateSickLeaveRequest(
                startDate = LocalDate.now().plusDays(5),
                endDate = LocalDate.now(),
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<BadRequestException> { service.reportSickLeave(userId, dto) }
    }

    @Test
    fun `reportSickLeave rejects overlapping sick leave`() {
        val dto =
            CreateSickLeaveRequest(
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(3),
            )
        val existing = sickLeaveEntity(userId = userId)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(sickLeaveRepository.findOverlapping(userId, dto.startDate, dto.endDate)).thenReturn(listOf(existing))

        assertThrows<ConflictException> { service.reportSickLeave(userId, dto) }
    }

    // ---- reportSickLeaveByManager ----

    @Test
    fun `reportSickLeaveByManager success`() {
        val dto =
            CreateSickLeaveRequest(
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(2),
                notes = "Employee called in sick",
            )

        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(sickLeaveRepository.findOverlapping(userId, dto.startDate, dto.endDate)).thenReturn(emptyList())
        `when`(sickLeaveRepository.save(any())).thenAnswer { it.arguments[0] as SickLeaveEntity }

        val result = service.reportSickLeaveByManager(managerId, userId, dto)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.reportedById).isEqualTo(managerId)
        assertThat(result.status).isEqualTo(SickLeaveStatus.REPORTED)
    }

    // ---- updateSickLeave ----

    @Test
    fun `updateSickLeave success`() {
        val entity = sickLeaveEntity(userId = userId)
        val dto = UpdateSickLeaveRequest(endDate = LocalDate.now().plusDays(7), notes = "Extended")

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(sickLeaveRepository.save(any())).thenAnswer { it.arguments[0] as SickLeaveEntity }

        val result = service.updateSickLeave(entity.id, userId, dto)

        assertThat(result.endDate).isEqualTo(dto.endDate)
        assertThat(result.notes).isEqualTo("Extended")
    }

    @Test
    fun `updateSickLeave throws when not owner`() {
        val entity = sickLeaveEntity(userId = UUID.randomUUID())
        val dto = UpdateSickLeaveRequest(notes = "test")

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<ForbiddenException> { service.updateSickLeave(entity.id, userId, dto) }
    }

    @Test
    fun `updateSickLeave throws when cancelled`() {
        val entity = sickLeaveEntity(userId = userId, status = SickLeaveStatus.CANCELLED)
        val dto = UpdateSickLeaveRequest(notes = "test")

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<BadRequestException> { service.updateSickLeave(entity.id, userId, dto) }
    }

    // ---- cancelSickLeave ----

    @Test
    fun `cancelSickLeave success`() {
        val entity = sickLeaveEntity(userId = userId)

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(sickLeaveRepository.save(any())).thenAnswer { it.arguments[0] as SickLeaveEntity }

        service.cancelSickLeave(entity.id, userId)

        assertThat(entity.status).isEqualTo(SickLeaveStatus.CANCELLED)
    }

    @Test
    fun `cancelSickLeave throws when not owner`() {
        val entity = sickLeaveEntity(userId = UUID.randomUUID())

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<ForbiddenException> { service.cancelSickLeave(entity.id, userId) }
    }

    @Test
    fun `cancelSickLeave throws when already cancelled`() {
        val entity = sickLeaveEntity(userId = userId, status = SickLeaveStatus.CANCELLED)

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<BadRequestException> { service.cancelSickLeave(entity.id, userId) }
    }

    // ---- submitCertificate ----

    @Test
    fun `submitCertificate success`() {
        val entity = sickLeaveEntity(userId = userId)

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(sickLeaveRepository.save(any())).thenAnswer { it.arguments[0] as SickLeaveEntity }

        val result = service.submitCertificate(entity.id, userId)

        assertThat(result.hasCertificate).isTrue()
        assertThat(result.status).isEqualTo(SickLeaveStatus.CERTIFICATE_RECEIVED)
    }

    @Test
    fun `submitCertificate throws when not owner`() {
        val entity = sickLeaveEntity(userId = UUID.randomUUID())

        `when`(sickLeaveRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<ForbiddenException> { service.submitCertificate(entity.id, userId) }
    }

    // ---- getSickLeave ----

    @Test
    fun `getSickLeave throws when not found`() {
        val id = UUID.randomUUID()
        `when`(sickLeaveRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getSickLeave(id) }
    }

    // ---- helpers ----

    private fun sickLeaveEntity(
        userId: UUID,
        status: SickLeaveStatus = SickLeaveStatus.REPORTED,
    ): SickLeaveEntity {
        val entityUser =
            if (userId == this.userId) {
                user
            } else {
                UserEntity(
                    id = userId,
                    email = "other@test.com",
                    passwordHash = "hash",
                    firstName = "Other",
                    lastName = "User",
                )
            }
        return SickLeaveEntity(
            user = entityUser,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(3),
            status = status,
        )
    }
}
