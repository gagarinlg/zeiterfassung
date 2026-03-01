package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.ApproveBusinessTripRequest
import com.zeiterfassung.model.dto.CreateBusinessTripRequest
import com.zeiterfassung.model.dto.RejectBusinessTripRequest
import com.zeiterfassung.model.dto.UpdateBusinessTripRequest
import com.zeiterfassung.model.entity.BusinessTripEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.enums.BusinessTripStatus
import com.zeiterfassung.repository.BusinessTripRepository
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
class BusinessTripServiceTest {
    @Mock private lateinit var businessTripRepository: BusinessTripRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var auditService: AuditService

    @Mock private lateinit var notificationService: NotificationService

    private lateinit var service: BusinessTripService

    private lateinit var user: UserEntity
    private lateinit var manager: UserEntity
    private val userId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            BusinessTripService(
                businessTripRepository,
                userRepository,
                auditService,
                notificationService,
            )
        user = UserEntity(id = userId, email = "user@test.com", passwordHash = "hash", firstName = "John", lastName = "Doe")
        manager = UserEntity(id = managerId, email = "manager@test.com", passwordHash = "hash", firstName = "Jane", lastName = "Smith")
        manager.subordinates.add(user)
    }

    // ---- createTrip ----

    @Test
    fun `createTrip success`() {
        val dto =
            CreateBusinessTripRequest(
                startDate = LocalDate.now().plusDays(5),
                endDate = LocalDate.now().plusDays(7),
                destination = "Berlin",
                purpose = "Conference",
            )

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(businessTripRepository.findOverlapping(userId, dto.startDate, dto.endDate, null)).thenReturn(emptyList())
        `when`(businessTripRepository.save(any())).thenAnswer { it.arguments[0] as BusinessTripEntity }

        val result = service.createTrip(userId, dto)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.status).isEqualTo(BusinessTripStatus.REQUESTED)
        assertThat(result.destination).isEqualTo("Berlin")
        assertThat(result.purpose).isEqualTo("Conference")
    }

    @Test
    fun `createTrip rejects invalid dates (end before start)`() {
        val dto =
            CreateBusinessTripRequest(
                startDate = LocalDate.now().plusDays(10),
                endDate = LocalDate.now().plusDays(5),
                destination = "Berlin",
                purpose = "Conference",
            )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<BadRequestException> { service.createTrip(userId, dto) }
    }

    @Test
    fun `createTrip rejects overlapping trips`() {
        val dto =
            CreateBusinessTripRequest(
                startDate = LocalDate.now().plusDays(5),
                endDate = LocalDate.now().plusDays(7),
                destination = "Berlin",
                purpose = "Conference",
            )
        val existing = tripEntity(userId = userId)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(businessTripRepository.findOverlapping(userId, dto.startDate, dto.endDate, null)).thenReturn(listOf(existing))

        assertThrows<ConflictException> { service.createTrip(userId, dto) }
    }

    // ---- updateTrip ----

    @Test
    fun `updateTrip success`() {
        val entity = tripEntity(userId = userId)
        val dto = UpdateBusinessTripRequest(destination = "Munich")

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(businessTripRepository.findOverlapping(userId, entity.startDate, entity.endDate, entity.id)).thenReturn(emptyList())
        `when`(businessTripRepository.save(any())).thenAnswer { it.arguments[0] as BusinessTripEntity }

        val result = service.updateTrip(entity.id, userId, dto)

        assertThat(result.destination).isEqualTo("Munich")
    }

    @Test
    fun `updateTrip throws when not owner`() {
        val entity = tripEntity(userId = UUID.randomUUID())
        val dto = UpdateBusinessTripRequest(destination = "Munich")

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<ForbiddenException> { service.updateTrip(entity.id, userId, dto) }
    }

    @Test
    fun `updateTrip throws when not pending`() {
        val entity = tripEntity(userId = userId, status = BusinessTripStatus.APPROVED)
        val dto = UpdateBusinessTripRequest(destination = "Munich")

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<BadRequestException> { service.updateTrip(entity.id, userId, dto) }
    }

    // ---- cancelTrip ----

    @Test
    fun `cancelTrip success`() {
        val entity = tripEntity(userId = userId)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(businessTripRepository.save(any())).thenAnswer { it.arguments[0] as BusinessTripEntity }

        service.cancelTrip(entity.id, userId)

        assertThat(entity.status).isEqualTo(BusinessTripStatus.CANCELLED)
    }

    @Test
    fun `cancelTrip throws when not owner`() {
        val entity = tripEntity(userId = UUID.randomUUID())

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<ForbiddenException> { service.cancelTrip(entity.id, userId) }
    }

    // ---- approveTrip ----

    @Test
    fun `approveTrip success`() {
        val entity = tripEntity(userId = userId)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(businessTripRepository.save(any())).thenAnswer { it.arguments[0] as BusinessTripEntity }

        val result = service.approveTrip(entity.id, managerId, ApproveBusinessTripRequest())

        assertThat(result.status).isEqualTo(BusinessTripStatus.APPROVED)
        assertThat(result.approvedById).isEqualTo(managerId)
    }

    @Test
    fun `approveTrip throws when self-approving`() {
        val entity = tripEntity(userId = userId)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<ForbiddenException> { service.approveTrip(entity.id, userId, ApproveBusinessTripRequest()) }
    }

    @Test
    fun `approveTrip throws when not pending`() {
        val entity = tripEntity(userId = userId, status = BusinessTripStatus.APPROVED)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))

        assertThrows<BadRequestException> { service.approveTrip(entity.id, managerId, ApproveBusinessTripRequest()) }
    }

    // ---- rejectTrip ----

    @Test
    fun `rejectTrip success`() {
        val entity = tripEntity(userId = userId)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(businessTripRepository.save(any())).thenAnswer { it.arguments[0] as BusinessTripEntity }

        val result = service.rejectTrip(entity.id, managerId, RejectBusinessTripRequest(rejectionReason = "Budget exceeded"))

        assertThat(result.status).isEqualTo(BusinessTripStatus.REJECTED)
        assertThat(result.rejectionReason).isEqualTo("Budget exceeded")
    }

    @Test
    fun `rejectTrip throws when self-rejecting`() {
        val entity = tripEntity(userId = userId)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<ForbiddenException> {
            service.rejectTrip(entity.id, userId, RejectBusinessTripRequest(rejectionReason = "reason"))
        }
    }

    // ---- completeTrip ----

    @Test
    fun `completeTrip success`() {
        val entity = tripEntity(userId = userId, status = BusinessTripStatus.APPROVED)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))
        `when`(businessTripRepository.save(any())).thenAnswer { it.arguments[0] as BusinessTripEntity }

        val result = service.completeTrip(entity.id, userId, null)

        assertThat(result.status).isEqualTo(BusinessTripStatus.COMPLETED)
    }

    @Test
    fun `completeTrip throws when not approved`() {
        val entity = tripEntity(userId = userId, status = BusinessTripStatus.REQUESTED)

        `when`(businessTripRepository.findById(entity.id)).thenReturn(Optional.of(entity))

        assertThrows<BadRequestException> { service.completeTrip(entity.id, userId, null) }
    }

    // ---- getTrip ----

    @Test
    fun `getTrip throws when not found`() {
        val id = UUID.randomUUID()
        `when`(businessTripRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getTrip(id) }
    }

    // ---- helpers ----

    private fun tripEntity(
        userId: UUID,
        status: BusinessTripStatus = BusinessTripStatus.REQUESTED,
    ): BusinessTripEntity {
        val tripUser =
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
        return BusinessTripEntity(
            user = tripUser,
            startDate = LocalDate.now().plusDays(5),
            endDate = LocalDate.now().plusDays(7),
            destination = "Berlin",
            purpose = "Conference",
            status = status,
        )
    }
}
