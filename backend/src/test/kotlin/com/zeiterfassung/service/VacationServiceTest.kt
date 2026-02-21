package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.CreateVacationRequest
import com.zeiterfassung.model.dto.RejectVacationRequest
import com.zeiterfassung.model.entity.EmployeeConfigEntity
import com.zeiterfassung.model.entity.PublicHolidayEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.model.entity.VacationBalanceEntity
import com.zeiterfassung.model.entity.VacationRequestEntity
import com.zeiterfassung.model.enums.VacationStatus
import com.zeiterfassung.repository.EmployeeConfigRepository
import com.zeiterfassung.repository.PublicHolidayRepository
import com.zeiterfassung.repository.UserRepository
import com.zeiterfassung.repository.VacationBalanceRepository
import com.zeiterfassung.repository.VacationRequestRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class VacationServiceTest {
    @Mock private lateinit var vacationRequestRepository: VacationRequestRepository
    @Mock private lateinit var vacationBalanceRepository: VacationBalanceRepository
    @Mock private lateinit var publicHolidayRepository: PublicHolidayRepository
    @Mock private lateinit var employeeConfigRepository: EmployeeConfigRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var auditService: AuditService

    private val objectMapper = ObjectMapper()
    private lateinit var service: VacationService

    private lateinit var user: UserEntity
    private lateinit var manager: UserEntity
    private val userId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            VacationService(
                vacationRequestRepository,
                vacationBalanceRepository,
                publicHolidayRepository,
                employeeConfigRepository,
                userRepository,
                auditService,
                objectMapper,
            )
        user = UserEntity(id = userId, email = "user@test.com", passwordHash = "hash", firstName = "John", lastName = "Doe")
        manager = UserEntity(id = managerId, email = "manager@test.com", passwordHash = "hash", firstName = "Jane", lastName = "Smith")
        manager.subordinates.add(user)
    }

    // ---- createRequest ----

    @Test
    fun `createRequest success`() {
        val dto = CreateVacationRequest(
            startDate = LocalDate.now().plusDays(5),
            endDate = LocalDate.now().plusDays(9),
        )
        val balance = balanceWith(totalDays = 30, usedDays = 0, carriedOverDays = 0)
        val config = configWith(vacationDaysPerYear = 30)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(vacationRequestRepository.findOverlapping(any(), any(), any(), any())).thenReturn(emptyList())
        `when`(publicHolidayRepository.findApplicableForYear(any())).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)
        `when`(vacationBalanceRepository.findByUserIdAndYear(any(), any())).thenReturn(balance)
        `when`(vacationRequestRepository.save(any())).thenAnswer { it.arguments[0] as VacationRequestEntity }

        val result = service.createRequest(userId, dto)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.status).isEqualTo(VacationStatus.PENDING)
        assertThat(result.totalDays).isGreaterThan(BigDecimal.ZERO)
    }

    @Test
    fun `createRequest rejects past start date`() {
        val dto = CreateVacationRequest(
            startDate = LocalDate.now().minusDays(1),
            endDate = LocalDate.now().plusDays(3),
        )
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<BadRequestException> { service.createRequest(userId, dto) }
    }

    @Test
    fun `createRequest rejects overlapping request`() {
        val dto = CreateVacationRequest(
            startDate = LocalDate.now().plusDays(5),
            endDate = LocalDate.now().plusDays(9),
        )
        val existingRequest = requestEntity(userId = userId, status = VacationStatus.PENDING)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(vacationRequestRepository.findOverlapping(any(), any(), any(), any())).thenReturn(listOf(existingRequest))

        assertThrows<ConflictException> { service.createRequest(userId, dto) }
    }

    @Test
    fun `createRequest rejects insufficient balance`() {
        val dto = CreateVacationRequest(
            startDate = LocalDate.now().plusDays(5),
            endDate = LocalDate.now().plusDays(10),
        )
        val balance = balanceWith(totalDays = 1, usedDays = 1, carriedOverDays = 0)
        val config = configWith(vacationDaysPerYear = 1)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(vacationRequestRepository.findOverlapping(any(), any(), any(), any())).thenReturn(emptyList())
        `when`(publicHolidayRepository.findApplicableForYear(any())).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)
        `when`(vacationBalanceRepository.findByUserIdAndYear(any(), any())).thenReturn(balance)

        assertThrows<BadRequestException> { service.createRequest(userId, dto) }
    }

    // ---- cancelRequest ----

    @Test
    fun `cancelRequest pending request`() {
        val request = requestEntity(userId = userId, status = VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(vacationRequestRepository.save(any())).thenReturn(request)

        service.cancelRequest(request.id, userId)

        assertThat(request.status).isEqualTo(VacationStatus.CANCELLED)
    }

    @Test
    fun `cancelRequest approved request restores balance`() {
        val request = requestEntity(userId = userId, status = VacationStatus.APPROVED, totalDays = BigDecimal("5"))
        val balance = balanceWith(totalDays = 30, usedDays = 5, carriedOverDays = 0)

        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(vacationRequestRepository.save(any())).thenReturn(request)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, request.startDate.year)).thenReturn(balance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }

        service.cancelRequest(request.id, userId)

        assertThat(balance.usedDays).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `cancelRequest throws when already cancelled`() {
        val request = requestEntity(userId = userId, status = VacationStatus.CANCELLED)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<BadRequestException> { service.cancelRequest(request.id, userId) }
    }

    @Test
    fun `cancelRequest throws when not owner`() {
        val request = requestEntity(userId = UUID.randomUUID(), status = VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<ForbiddenException> { service.cancelRequest(request.id, userId) }
    }

    // ---- approveRequest ----

    @Test
    fun `approveRequest success`() {
        val request = requestEntity(userId = userId, status = VacationStatus.PENDING, totalDays = BigDecimal("3"))
        val balance = balanceWith(totalDays = 30, usedDays = 0, carriedOverDays = 0)

        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, request.startDate.year)).thenReturn(balance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }
        `when`(vacationRequestRepository.save(any())).thenAnswer { it.arguments[0] as VacationRequestEntity }

        service.approveRequest(request.id, managerId, com.zeiterfassung.model.dto.ApproveVacationRequest())

        assertThat(request.status).isEqualTo(VacationStatus.APPROVED)
        assertThat(balance.usedDays).isEqualByComparingTo(BigDecimal("3"))
    }

    @Test
    fun `approveRequest throws when self-approval attempted`() {
        val request = requestEntity(userId = userId, status = VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<ForbiddenException> {
            service.approveRequest(request.id, userId, com.zeiterfassung.model.dto.ApproveVacationRequest())
        }
    }

    @Test
    fun `approveRequest throws when not pending`() {
        val request = requestEntity(userId = userId, status = VacationStatus.APPROVED)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))

        assertThrows<BadRequestException> {
            service.approveRequest(request.id, managerId, com.zeiterfassung.model.dto.ApproveVacationRequest())
        }
    }

    // ---- rejectRequest ----

    @Test
    fun `rejectRequest success`() {
        val request = requestEntity(userId = userId, status = VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(vacationRequestRepository.save(any())).thenAnswer { it.arguments[0] as VacationRequestEntity }

        service.rejectRequest(request.id, managerId, RejectVacationRequest(rejectionReason = "Not enough coverage"))

        assertThat(request.status).isEqualTo(VacationStatus.REJECTED)
        assertThat(request.rejectionReason).isEqualTo("Not enough coverage")
    }

    @Test
    fun `rejectRequest throws when self-rejection attempted`() {
        val request = requestEntity(userId = userId, status = VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<ForbiddenException> {
            service.rejectRequest(request.id, userId, RejectVacationRequest(rejectionReason = "reason"))
        }
    }

    // ---- calculateWorkingDays ----

    @Test
    fun `calculateWorkingDays counts weekdays only`() {
        // Monday to Friday = 5 days
        val monday = LocalDate.of(2025, 1, 6)
        val friday = LocalDate.of(2025, 1, 10)
        val result = service.calculateWorkingDays(monday, friday, false, false, listOf(1, 2, 3, 4, 5), emptyList())
        assertThat(result).isEqualByComparingTo(BigDecimal("5"))
    }

    @Test
    fun `calculateWorkingDays excludes weekends`() {
        // Friday to Monday = only 2 working days (Fri + Mon)
        val friday = LocalDate.of(2025, 1, 10)
        val monday = LocalDate.of(2025, 1, 13)
        val result = service.calculateWorkingDays(friday, monday, false, false, listOf(1, 2, 3, 4, 5), emptyList())
        assertThat(result).isEqualByComparingTo(BigDecimal("2"))
    }

    @Test
    fun `calculateWorkingDays excludes public holidays`() {
        val monday = LocalDate.of(2025, 1, 6)
        val friday = LocalDate.of(2025, 1, 10)
        val holidays = listOf(LocalDate.of(2025, 1, 8)) // Wednesday is a holiday
        val result = service.calculateWorkingDays(monday, friday, false, false, listOf(1, 2, 3, 4, 5), holidays)
        assertThat(result).isEqualByComparingTo(BigDecimal("4"))
    }

    @Test
    fun `calculateWorkingDays handles half day start`() {
        val monday = LocalDate.of(2025, 1, 6)
        val wednesday = LocalDate.of(2025, 1, 8)
        val result = service.calculateWorkingDays(monday, wednesday, true, false, listOf(1, 2, 3, 4, 5), emptyList())
        assertThat(result).isEqualByComparingTo(BigDecimal("2.5"))
    }

    @Test
    fun `calculateWorkingDays handles single half day`() {
        val monday = LocalDate.of(2025, 1, 6)
        val result = service.calculateWorkingDays(monday, monday, true, false, listOf(1, 2, 3, 4, 5), emptyList())
        assertThat(result).isEqualByComparingTo(BigDecimal("0.5"))
    }

    @Test
    fun `calculateWorkingDays with custom work days`() {
        // Mon-Thu work week (4 days)
        val monday = LocalDate.of(2025, 1, 6)
        val friday = LocalDate.of(2025, 1, 10)
        val result = service.calculateWorkingDays(monday, friday, false, false, listOf(1, 2, 3, 4), emptyList())
        assertThat(result).isEqualByComparingTo(BigDecimal("4"))
    }

    // ---- balance initialization ----

    @Test
    fun `initializeYearBalance creates new balance with carry over`() {
        val prevBalance = balanceWith(totalDays = 30, usedDays = 20, carriedOverDays = 0) // 10 remaining
        val config = configWith(vacationDaysPerYear = 30, vacationCarryOverMax = 5)

        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(null)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2024)).thenReturn(prevBalance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }

        val result = service.initializeYearBalance(userId, 2025)

        assertThat(result.totalDays).isEqualByComparingTo(BigDecimal("30"))
        // 10 remaining from prev year, capped at 5
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal("5"))
    }

    @Test
    fun `initializeYearBalance returns existing balance if found`() {
        val existing = balanceWith(totalDays = 25, usedDays = 0, carriedOverDays = 0)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(existing)

        val result = service.initializeYearBalance(userId, 2025)

        assertThat(result).isSameAs(existing)
    }

    // ---- getPublicHolidays ----

    @Test
    fun `getPublicHolidays returns adjusted dates for recurring holidays`() {
        val holiday = PublicHolidayEntity(
            date = LocalDate.of(2024, 1, 1),
            name = "Neujahr",
            stateCode = null,
            isRecurring = true,
        )
        `when`(publicHolidayRepository.findApplicableForYear(2025)).thenReturn(listOf(holiday))

        val result = service.getPublicHolidays(2025, null)

        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2025, 1, 1))
    }

    @Test
    fun `getPublicHolidays filters by state code`() {
        val federal = PublicHolidayEntity(date = LocalDate.of(2024, 1, 1), name = "Neujahr", stateCode = null, isRecurring = true)
        val stateOnly = PublicHolidayEntity(date = LocalDate.of(2024, 11, 1), name = "Allerheiligen", stateCode = "BY", isRecurring = true)
        val otherState = PublicHolidayEntity(date = LocalDate.of(2024, 8, 15), name = "Maria Himmelfahrt", stateCode = "BY", isRecurring = true)

        `when`(publicHolidayRepository.findApplicableForYear(2025)).thenReturn(listOf(federal, stateOnly, otherState))

        val resultBY = service.getPublicHolidays(2025, "BY")
        assertThat(resultBY).hasSize(3) // federal + both BY

        val resultHE = service.getPublicHolidays(2025, "HE")
        assertThat(resultHE).hasSize(1) // federal only
    }

    // ---- helpers ----

    private fun balanceWith(
        totalDays: Int,
        usedDays: Int,
        carriedOverDays: Int,
    ) = VacationBalanceEntity(
        user = user,
        year = LocalDate.now().year,
        totalDays = BigDecimal(totalDays),
        usedDays = BigDecimal(usedDays),
        carriedOverDays = BigDecimal(carriedOverDays),
    )

    private fun configWith(
        vacationDaysPerYear: Int = 30,
        vacationCarryOverMax: Int = 10,
    ) = EmployeeConfigEntity(
        user = user,
        vacationDaysPerYear = vacationDaysPerYear,
        vacationCarryOverMax = vacationCarryOverMax,
    )

    private fun requestEntity(
        userId: UUID,
        status: VacationStatus,
        totalDays: BigDecimal = BigDecimal("5"),
    ): VacationRequestEntity {
        val requestUser = if (userId == this.userId) user else UserEntity(
            id = userId,
            email = "other@test.com",
            passwordHash = "hash",
            firstName = "Other",
            lastName = "User",
        )
        return VacationRequestEntity(
            user = requestUser,
            startDate = LocalDate.now().plusDays(10),
            endDate = LocalDate.now().plusDays(14),
            totalDays = totalDays,
            status = status,
        )
    }
}
