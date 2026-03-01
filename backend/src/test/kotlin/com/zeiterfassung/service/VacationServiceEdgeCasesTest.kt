package com.zeiterfassung.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.BadRequestException
import com.zeiterfassung.exception.ConflictException
import com.zeiterfassung.exception.ForbiddenException
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.model.dto.UpdateVacationRequest
import com.zeiterfassung.model.entity.EmployeeConfigEntity
import com.zeiterfassung.model.entity.PermissionEntity
import com.zeiterfassung.model.entity.PublicHolidayEntity
import com.zeiterfassung.model.entity.RoleEntity
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
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class VacationServiceEdgeCasesTest {
    @Mock private lateinit var vacationRequestRepository: VacationRequestRepository
    @Mock private lateinit var vacationBalanceRepository: VacationBalanceRepository
    @Mock private lateinit var publicHolidayRepository: PublicHolidayRepository
    @Mock private lateinit var employeeConfigRepository: EmployeeConfigRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var auditService: AuditService
    @Mock private lateinit var notificationService: NotificationService

    private val objectMapper = ObjectMapper()
    private lateinit var service: VacationService

    private lateinit var user: UserEntity
    private lateinit var manager: UserEntity
    private lateinit var admin: UserEntity
    private lateinit var substitute: UserEntity
    private val userId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()
    private val substituteId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = VacationService(
            vacationRequestRepository, vacationBalanceRepository, publicHolidayRepository,
            employeeConfigRepository, userRepository, auditService, objectMapper, notificationService,
        )
        user = UserEntity(id = userId, email = "user@test.com", passwordHash = "h", firstName = "User", lastName = "One")
        manager = UserEntity(id = managerId, email = "mgr@test.com", passwordHash = "h", firstName = "Manager", lastName = "One")
        manager.subordinates.add(user)

        val adminPerm = PermissionEntity(name = "admin.users.manage")
        val adminRole = RoleEntity(name = "SUPER_ADMIN", permissions = mutableSetOf(adminPerm))
        admin = UserEntity(id = adminId, email = "admin@test.com", passwordHash = "h", firstName = "Admin", lastName = "One")
        admin.roles.add(adminRole)

        substitute = UserEntity(id = substituteId, email = "sub@test.com", passwordHash = "h", firstName = "Sub", lastName = "One")
    }

    // ---- getRequest ----

    @Test
    fun `getRequest as owner succeeds`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(userRepository.findBySubstituteId(userId)).thenReturn(emptyList())

        val result = service.getRequest(request.id, userId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `getRequest as manager succeeds`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))

        val result = service.getRequest(request.id, managerId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `getRequest as substitute of manager succeeds`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(substituteId)).thenReturn(Optional.of(substitute))
        `when`(userRepository.findBySubstituteId(substituteId)).thenReturn(listOf(manager))

        val result = service.getRequest(request.id, substituteId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `getRequest as admin succeeds`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(adminId)).thenReturn(Optional.of(admin))

        val result = service.getRequest(request.id, adminId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `getRequest forbidden for unrelated user`() {
        val otherId = UUID.randomUUID()
        val other = UserEntity(id = otherId, email = "other@test.com", passwordHash = "h", firstName = "O", lastName = "U")
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(userRepository.findById(otherId)).thenReturn(Optional.of(other))
        `when`(userRepository.findBySubstituteId(otherId)).thenReturn(emptyList())

        assertThrows<ForbiddenException> { service.getRequest(request.id, otherId) }
    }

    @Test
    fun `getRequest throws when request not found`() {
        val id = UUID.randomUUID()
        `when`(vacationRequestRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getRequest(id, userId) }
    }

    // ---- getUserRequests ----

    @Test
    fun `getUserRequests with year filter`() {
        val pageable = PageRequest.of(0, 10)
        val request = makeRequest(userId, VacationStatus.APPROVED)
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            userId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
        )).thenReturn(listOf(request))

        val result = service.getUserRequests(userId, 2025, null, pageable)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `getUserRequests with year and status filter`() {
        val pageable = PageRequest.of(0, 10)
        val approved = makeRequest(userId, VacationStatus.APPROVED)
        val pending = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            userId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
        )).thenReturn(listOf(approved, pending))

        val result = service.getUserRequests(userId, 2025, VacationStatus.PENDING, pageable)
        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].status).isEqualTo(VacationStatus.PENDING)
    }

    @Test
    fun `getUserRequests with status filter only`() {
        val pageable = PageRequest.of(0, 10)
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findByUserIdAndStatus(userId, VacationStatus.PENDING))
            .thenReturn(listOf(request))

        val result = service.getUserRequests(userId, null, VacationStatus.PENDING, pageable)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `getUserRequests with no filter uses pageable`() {
        val pageable = PageRequest.of(0, 10)
        val request = makeRequest(userId, VacationStatus.APPROVED)
        `when`(vacationRequestRepository.findByUserId(userId, pageable))
            .thenReturn(PageImpl(listOf(request), pageable, 1))

        val result = service.getUserRequests(userId, null, null, pageable)
        assertThat(result.totalElements).isEqualTo(1)
    }

    // ---- getPendingRequests ----

    @Test
    fun `getPendingRequests with subordinates`() {
        val pageable = PageRequest.of(0, 10)
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findBySubstituteId(managerId)).thenReturn(emptyList())
        `when`(vacationRequestRepository.findByStatusAndUserIdIn(
            VacationStatus.PENDING, mutableListOf(userId), pageable,
        )).thenReturn(PageImpl(listOf(request), pageable, 1))

        val result = service.getPendingRequests(managerId, pageable)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `getPendingRequests includes substitute subordinates`() {
        val pageable = PageRequest.of(0, 10)
        val otherEmployeeId = UUID.randomUUID()
        val otherEmployee = UserEntity(id = otherEmployeeId, email = "emp2@t.com", passwordHash = "h", firstName = "E", lastName = "T")
        val otherManager = UserEntity(id = UUID.randomUUID(), email = "mgr2@t.com", passwordHash = "h", firstName = "M", lastName = "T")
        otherManager.subordinates.add(otherEmployee)

        val sub2 = UserEntity(id = substituteId, email = "sub@t.com", passwordHash = "h", firstName = "S", lastName = "T")
        `when`(userRepository.findById(substituteId)).thenReturn(Optional.of(sub2))
        `when`(userRepository.findBySubstituteId(substituteId)).thenReturn(listOf(otherManager))
        `when`(vacationRequestRepository.findByStatusAndUserIdIn(
            any(VacationStatus::class.java) ?: VacationStatus.PENDING,
            any() ?: mutableListOf(),
            any() ?: pageable,
        )).thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.getPendingRequests(substituteId, pageable)
        assertThat(result).isNotNull
    }

    @Test
    fun `getPendingRequests returns empty when no subordinates`() {
        val pageable = PageRequest.of(0, 10)
        val noSubMgr = UserEntity(id = UUID.randomUUID(), email = "ns@t.com", passwordHash = "h", firstName = "N", lastName = "S")
        `when`(userRepository.findById(noSubMgr.id)).thenReturn(Optional.of(noSubMgr))
        `when`(userRepository.findBySubstituteId(noSubMgr.id)).thenReturn(emptyList())

        val result = service.getPendingRequests(noSubMgr.id, pageable)
        assertThat(result.totalElements).isEqualTo(0)
    }

    // ---- getAllPendingRequests ----

    @Test
    fun `getAllPendingRequests delegates to repository`() {
        val pageable = PageRequest.of(0, 10)
        val request = makeRequest(userId, VacationStatus.PENDING)
        `when`(vacationRequestRepository.findByStatus(VacationStatus.PENDING, pageable))
            .thenReturn(PageImpl(listOf(request), pageable, 1))

        val result = service.getAllPendingRequests(pageable)
        assertThat(result.totalElements).isEqualTo(1)
    }

    // ---- getBalanceForManager ----

    @Test
    fun `getBalanceForManager as admin succeeds`() {
        val balance = makeBalance(30, 5, 0)
        `when`(userRepository.findById(adminId)).thenReturn(Optional.of(admin))
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(balance)
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.getBalanceForManager(adminId, userId, 2025)
        assertThat(result.totalDays).isEqualByComparingTo(BigDecimal("30"))
    }

    @Test
    fun `getBalanceForManager as direct manager succeeds`() {
        val balance = makeBalance(30, 10, 0)
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(balance)
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.getBalanceForManager(managerId, userId, 2025)
        assertThat(result.usedDays).isEqualByComparingTo(BigDecimal("10"))
    }

    @Test
    fun `getBalanceForManager as substitute succeeds`() {
        val balance = makeBalance(30, 0, 0)
        `when`(userRepository.findById(substituteId)).thenReturn(Optional.of(substitute))
        `when`(userRepository.findBySubstituteId(substituteId)).thenReturn(listOf(manager))
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(balance)
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.getBalanceForManager(substituteId, userId, 2025)
        assertThat(result).isNotNull
    }

    @Test
    fun `getBalanceForManager forbidden for unrelated user`() {
        val otherId = UUID.randomUUID()
        val other = UserEntity(id = otherId, email = "other2@t.com", passwordHash = "h", firstName = "O", lastName = "U")
        `when`(userRepository.findById(otherId)).thenReturn(Optional.of(other))
        `when`(userRepository.findBySubstituteId(otherId)).thenReturn(emptyList())

        assertThrows<ForbiddenException> { service.getBalanceForManager(otherId, userId, 2025) }
    }

    // ---- getPublicHolidays ----

    @Test
    fun `getPublicHolidays returns all when no state code filter`() {
        val federal = PublicHolidayEntity(date = LocalDate.of(2024, 1, 1), name = "Neujahr", stateCode = null, isRecurring = true)
        val state = PublicHolidayEntity(date = LocalDate.of(2024, 11, 1), name = "Allerheiligen", stateCode = "BY", isRecurring = true)
        `when`(publicHolidayRepository.findApplicableForYear(2025)).thenReturn(listOf(federal, state))

        val result = service.getPublicHolidays(2025, null)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getPublicHolidays non-recurring holiday keeps original date`() {
        val holiday = PublicHolidayEntity(date = LocalDate.of(2025, 6, 15), name = "Special", stateCode = null, isRecurring = false)
        `when`(publicHolidayRepository.findApplicableForYear(2025)).thenReturn(listOf(holiday))

        val result = service.getPublicHolidays(2025, null)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2025, 6, 15))
        assertThat(result[0].isRecurring).isFalse()
    }

    @Test
    fun `getPublicHolidays returns sorted by date`() {
        val h1 = PublicHolidayEntity(date = LocalDate.of(2024, 12, 25), name = "Weihnachten", stateCode = null, isRecurring = true)
        val h2 = PublicHolidayEntity(date = LocalDate.of(2024, 1, 1), name = "Neujahr", stateCode = null, isRecurring = true)
        `when`(publicHolidayRepository.findApplicableForYear(2025)).thenReturn(listOf(h1, h2))

        val result = service.getPublicHolidays(2025, null)
        assertThat(result[0].date).isBefore(result[1].date)
    }

    // ---- getTeamCalendar ----

    @Test
    fun `getTeamCalendar returns team and own requests`() {
        val request = makeRequest(userId, VacationStatus.APPROVED)
        val ownRequest = makeRequest(managerId, VacationStatus.PENDING)
        `when`(userRepository.findById(managerId)).thenReturn(Optional.of(manager))
        `when`(userRepository.findBySubstituteId(managerId)).thenReturn(emptyList())
        `when`(vacationRequestRepository.findApprovedByUserIdsAndDateRange(
            any() ?: mutableListOf(),
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(listOf(request))
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: managerId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(listOf(ownRequest))
        `when`(publicHolidayRepository.findApplicableForYear(anyInt())).thenReturn(emptyList())

        val result = service.getTeamCalendar(managerId, 2025, 6)
        assertThat(result.year).isEqualTo(2025)
        assertThat(result.month).isEqualTo(6)
        assertThat(result.teamRequests).hasSize(1)
        assertThat(result.ownRequests).hasSize(1)
    }

    @Test
    fun `getTeamCalendar with no subordinates returns empty team requests`() {
        val noSubMgr = UserEntity(id = UUID.randomUUID(), email = "ns2@t.com", passwordHash = "h", firstName = "N", lastName = "S")
        `when`(userRepository.findById(noSubMgr.id)).thenReturn(Optional.of(noSubMgr))
        `when`(userRepository.findBySubstituteId(noSubMgr.id)).thenReturn(emptyList())
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: noSubMgr.id,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())
        `when`(publicHolidayRepository.findApplicableForYear(anyInt())).thenReturn(emptyList())

        val result = service.getTeamCalendar(noSubMgr.id, 2025, 1)
        assertThat(result.teamRequests).isEmpty()
    }

    @Test
    fun `getTeamCalendar includes substitute subordinates`() {
        val otherEmpId = UUID.randomUUID()
        val otherEmp = UserEntity(id = otherEmpId, email = "emp3@t.com", passwordHash = "h", firstName = "E", lastName = "T")
        val otherMgr = UserEntity(id = UUID.randomUUID(), email = "mgr3@t.com", passwordHash = "h", firstName = "M", lastName = "T")
        otherMgr.subordinates.add(otherEmp)

        `when`(userRepository.findById(substituteId)).thenReturn(Optional.of(substitute))
        `when`(userRepository.findBySubstituteId(substituteId)).thenReturn(listOf(otherMgr))
        `when`(vacationRequestRepository.findApprovedByUserIdsAndDateRange(
            any() ?: mutableListOf(),
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: substituteId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())
        `when`(publicHolidayRepository.findApplicableForYear(anyInt())).thenReturn(emptyList())

        val result = service.getTeamCalendar(substituteId, 2025, 3)
        assertThat(result).isNotNull
    }

    // ---- updateRequest ----

    @Test
    fun `updateRequest success with date changes`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        val newStart = LocalDate.now().plusDays(20)
        val newEnd = LocalDate.now().plusDays(24)
        val dto = UpdateVacationRequest(startDate = newStart, endDate = newEnd, notes = "Updated")

        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(vacationRequestRepository.findOverlapping(userId, newStart, newEnd, request.id)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(publicHolidayRepository.findApplicableForYear(anyInt())).thenReturn(emptyList())
        val balance = makeBalance(30, 0, 0)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, newStart.year)).thenReturn(balance)
        `when`(vacationRequestRepository.save(any())).thenAnswer { it.arguments[0] as VacationRequestEntity }

        val result = service.updateRequest(request.id, userId, dto)
        assertThat(result.startDate).isEqualTo(newStart)
        assertThat(result.notes).isEqualTo("Updated")
    }

    @Test
    fun `updateRequest throws when not owner`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        val otherId = UUID.randomUUID()
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<ForbiddenException> {
            service.updateRequest(request.id, otherId, UpdateVacationRequest())
        }
    }

    @Test
    fun `updateRequest throws when not pending`() {
        val request = makeRequest(userId, VacationStatus.APPROVED)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<BadRequestException> {
            service.updateRequest(request.id, userId, UpdateVacationRequest())
        }
    }

    @Test
    fun `updateRequest throws on overlap`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        val newStart = LocalDate.now().plusDays(20)
        val newEnd = LocalDate.now().plusDays(24)
        val dto = UpdateVacationRequest(startDate = newStart, endDate = newEnd)

        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(vacationRequestRepository.findOverlapping(userId, newStart, newEnd, request.id))
            .thenReturn(listOf(makeRequest(userId, VacationStatus.APPROVED)))

        assertThrows<ConflictException> { service.updateRequest(request.id, userId, dto) }
    }

    @Test
    fun `updateRequest throws when start after end`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        val dto = UpdateVacationRequest(
            startDate = LocalDate.now().plusDays(30),
            endDate = LocalDate.now().plusDays(20),
        )
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<BadRequestException> { service.updateRequest(request.id, userId, dto) }
    }

    @Test
    fun `updateRequest throws for past date`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        val dto = UpdateVacationRequest(startDate = LocalDate.now().minusDays(1))
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<BadRequestException> { service.updateRequest(request.id, userId, dto) }
    }

    @Test
    fun `updateRequest with insufficient balance throws`() {
        val request = makeRequest(userId, VacationStatus.PENDING)
        val newStart = LocalDate.now().plusDays(5)
        val newEnd = LocalDate.now().plusDays(30)
        val dto = UpdateVacationRequest(startDate = newStart, endDate = newEnd)

        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))
        `when`(vacationRequestRepository.findOverlapping(userId, newStart, newEnd, request.id)).thenReturn(emptyList())
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(publicHolidayRepository.findApplicableForYear(anyInt())).thenReturn(emptyList())
        val balance = makeBalance(1, 1, 0)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, newStart.year)).thenReturn(balance)

        assertThrows<BadRequestException> { service.updateRequest(request.id, userId, dto) }
    }

    // ---- initializeYearBalance ----

    @Test
    fun `initializeYearBalance with no config uses defaults`() {
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(null)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2024)).thenReturn(null)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }

        val result = service.initializeYearBalance(userId, 2025)
        assertThat(result.totalDays).isEqualByComparingTo(BigDecimal("30"))
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `initializeYearBalance with no previous year remaining`() {
        val config = EmployeeConfigEntity(user = user, vacationDaysPerYear = 25, vacationCarryOverMax = 5)
        val prevBalance = VacationBalanceEntity(user = user, year = 2024, totalDays = BigDecimal("25"), usedDays = BigDecimal("25"))

        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(null)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(config)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2024)).thenReturn(prevBalance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }

        val result = service.initializeYearBalance(userId, 2025)
        assertThat(result.totalDays).isEqualByComparingTo(BigDecimal("25"))
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal.ZERO)
    }

    // ---- setBalance ----

    @Test
    fun `setBalance partial update only changes provided fields`() {
        val actorId = UUID.randomUUID()
        val balance = makeBalance(30, 5, 2)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(balance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.setBalance(userId, 2025, null, BigDecimal("10"), null, actorId)
        assertThat(result.totalDays).isEqualByComparingTo(BigDecimal("30"))
        assertThat(result.usedDays).isEqualByComparingTo(BigDecimal("10"))
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal("2"))
    }

    @Test
    fun `setBalance full update changes all fields`() {
        val actorId = UUID.randomUUID()
        val balance = makeBalance(30, 0, 0)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(balance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.setBalance(userId, 2025, BigDecimal("28"), BigDecimal("5"), BigDecimal("3"), actorId)
        assertThat(result.totalDays).isEqualByComparingTo(BigDecimal("28"))
        assertThat(result.usedDays).isEqualByComparingTo(BigDecimal("5"))
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal("3"))
    }

    // ---- triggerCarryOver ----

    @Test
    fun `triggerCarryOver with no config uses defaults`() {
        val actorId = UUID.randomUUID()
        val prevBalance = VacationBalanceEntity(user = user, year = 2024, totalDays = BigDecimal("30"), usedDays = BigDecimal("15"))
        val curBalance = makeBalance(30, 0, 0)

        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2024)).thenReturn(prevBalance)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(curBalance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.triggerCarryOver(userId, 2025, actorId)
        // 30 - 15 = 15 remaining, max default carry-over = 10
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal("10"))
    }

    @Test
    fun `triggerCarryOver with no previous balance carries zero`() {
        val actorId = UUID.randomUUID()
        val curBalance = makeBalance(30, 0, 0)

        `when`(employeeConfigRepository.findByUserId(userId)).thenReturn(null)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2024)).thenReturn(null)
        `when`(vacationBalanceRepository.findByUserIdAndYear(userId, 2025)).thenReturn(curBalance)
        `when`(vacationBalanceRepository.save(any())).thenAnswer { it.arguments[0] as VacationBalanceEntity }
        `when`(vacationRequestRepository.findByUserIdAndStartDateBetween(
            any(UUID::class.java) ?: userId,
            any(LocalDate::class.java) ?: LocalDate.now(),
            any(LocalDate::class.java) ?: LocalDate.now(),
        )).thenReturn(emptyList())

        val result = service.triggerCarryOver(userId, 2025, actorId)
        assertThat(result.carriedOverDays).isEqualByComparingTo(BigDecimal.ZERO)
    }

    // ---- cancelRequest edge case: rejected ----

    @Test
    fun `cancelRequest throws when rejected`() {
        val request = makeRequest(userId, VacationStatus.REJECTED)
        `when`(vacationRequestRepository.findById(request.id)).thenReturn(Optional.of(request))

        assertThrows<BadRequestException> { service.cancelRequest(request.id, userId) }
    }

    // ---- helpers ----

    private fun makeRequest(userId: UUID, status: VacationStatus): VacationRequestEntity {
        val reqUser = if (userId == this.userId) user
        else if (userId == managerId) manager
        else UserEntity(id = userId, email = "u$userId@t.com", passwordHash = "h", firstName = "F", lastName = "L")
        return VacationRequestEntity(
            user = reqUser, startDate = LocalDate.now().plusDays(10), endDate = LocalDate.now().plusDays(14),
            totalDays = BigDecimal("5"), status = status,
        )
    }

    private fun makeBalance(totalDays: Int, usedDays: Int, carriedOverDays: Int) =
        VacationBalanceEntity(
            user = user, year = 2025,
            totalDays = BigDecimal(totalDays), usedDays = BigDecimal(usedDays),
            carriedOverDays = BigDecimal(carriedOverDays),
        )
}
