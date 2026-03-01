package com.zeiterfassung.app

import com.zeiterfassung.app.data.model.PageResponse
import com.zeiterfassung.app.data.model.VacationBalance
import com.zeiterfassung.app.data.model.VacationRequest
import com.zeiterfassung.app.data.repository.AuthRepository
import com.zeiterfassung.app.data.repository.VacationRepository
import com.zeiterfassung.app.ui.viewmodel.VacationViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VacationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val vacationRepository = mockk<VacationRepository>(relaxed = true)

    private val testBalance = VacationBalance(
        id = "bal-1",
        userId = "user-1",
        year = 2026,
        totalDays = 30.0,
        usedDays = 5.0,
        carriedOverDays = 0.0,
        remainingDays = 25.0,
    )

    private val testRequests = listOf(
        VacationRequest(
            id = "req-1",
            userId = "user-1",
            startDate = "2026-04-01",
            endDate = "2026-04-05",
            isHalfDayStart = false,
            isHalfDayEnd = false,
            totalDays = 5.0,
            status = "PENDING",
            approvedBy = null,
            rejectionReason = null,
            notes = null,
            createdAt = "2026-03-01T09:00:00Z",
        ),
        VacationRequest(
            id = "req-2",
            userId = "user-1",
            startDate = "2026-05-10",
            endDate = "2026-05-10",
            isHalfDayStart = false,
            isHalfDayEnd = false,
            totalDays = 1.0,
            status = "APPROVED",
            approvedBy = "manager-1",
            rejectionReason = null,
            notes = null,
            createdAt = "2026-03-01T10:00:00Z",
        ),
    )

    private val testPageResponse = PageResponse(
        content = testRequests,
        totalElements = 2L,
        totalPages = 1,
        pageNumber = 0,
        pageSize = 20,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { vacationRepository.getBalance("user-1") } returns Result.success(testBalance)
        coEvery { vacationRepository.getRequests("user-1") } returns Result.success(testPageResponse)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = VacationViewModel(authRepository, vacationRepository)

    @Test
    fun `initial load fetches balance and requests`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(testBalance, vm.uiState.value.balance)
        assertEquals(testRequests, vm.uiState.value.requests)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `error when userId is null`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `balance failure sets error state`() = runTest {
        coEvery { vacationRepository.getBalance(any()) } returns Result.failure(Exception("API error"))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.balance)
    }

    @Test
    fun `requests failure still shows balance with empty list`() = runTest {
        coEvery { vacationRepository.getRequests(any()) } returns Result.failure(Exception("API error"))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        assertEquals(testBalance, vm.uiState.value.balance)
        assertEquals(emptyList<VacationRequest>(), vm.uiState.value.requests)
    }

    @Test
    fun `remaining days calculated correctly`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(25.0, vm.uiState.value.balance?.remainingDays)
    }

    @Test
    fun `requests are sorted by creation date`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val requests = vm.uiState.value.requests
        assertEquals(2, requests.size)
        assertEquals("req-1", requests[0].id)
        assertEquals("req-2", requests[1].id)
    }

    @Test
    fun `reload refreshes data`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testBalance, vm.uiState.value.balance)

        val newBalance = testBalance.copy(usedDays = 10.0, remainingDays = 20.0)
        coEvery { vacationRepository.getBalance("user-1") } returns Result.success(newBalance)

        vm.loadVacationData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(newBalance, vm.uiState.value.balance)
    }
}
