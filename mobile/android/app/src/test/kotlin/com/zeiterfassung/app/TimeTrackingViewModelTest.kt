package com.zeiterfassung.app

import app.cash.turbine.test
import com.zeiterfassung.app.data.model.TrackingStatusResponse
import com.zeiterfassung.app.data.repository.AuthRepository
import com.zeiterfassung.app.data.repository.TimeTrackingRepository
import com.zeiterfassung.app.ui.viewmodel.TimeTrackingViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimeTrackingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val timeTrackingRepository = mockk<TimeTrackingRepository>(relaxed = true)

    private val clockedOutStatus = TrackingStatusResponse(
        status = "CLOCKED_OUT",
        clockedInSince = null,
        breakStartedAt = null,
        elapsedWorkMinutes = 0,
        elapsedBreakMinutes = 0,
        todayWorkMinutes = 0,
        todayBreakMinutes = 0,
    )

    private val clockedInStatus = TrackingStatusResponse(
        status = "CLOCKED_IN",
        clockedInSince = "2026-03-01T09:00:00Z",
        breakStartedAt = null,
        elapsedWorkMinutes = 120,
        elapsedBreakMinutes = 0,
        todayWorkMinutes = 120,
        todayBreakMinutes = 0,
    )

    private val onBreakStatus = TrackingStatusResponse(
        status = "ON_BREAK",
        clockedInSince = "2026-03-01T09:00:00Z",
        breakStartedAt = "2026-03-01T11:00:00Z",
        elapsedWorkMinutes = 120,
        elapsedBreakMinutes = 15,
        todayWorkMinutes = 120,
        todayBreakMinutes = 15,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { timeTrackingRepository.getStatus("user-1") } returns Result.success(clockedOutStatus)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TimeTrackingViewModel(authRepository, timeTrackingRepository)

    @Test
    fun `initial load fetches and shows status`() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(clockedOutStatus, vm.uiState.value.status)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load error sets error state when userId is null`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `load error sets error state when API fails`() = runTest {
        coEvery { timeTrackingRepository.getStatus(any()) } returns Result.failure(Exception("Network"))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `clockIn calls repository and refreshes status`() = runTest {
        coEvery { timeTrackingRepository.getStatus("user-1") } returnsMany listOf(
            Result.success(clockedOutStatus),
            Result.success(clockedInStatus),
        )
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("CLOCKED_OUT", vm.uiState.value.status?.status)

        vm.clockIn()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { timeTrackingRepository.clockIn() }
        assertEquals("CLOCKED_IN", vm.uiState.value.status?.status)
    }

    @Test
    fun `clockOut calls repository and refreshes status`() = runTest {
        coEvery { timeTrackingRepository.getStatus("user-1") } returnsMany listOf(
            Result.success(clockedInStatus),
            Result.success(clockedOutStatus),
        )
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.clockOut()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { timeTrackingRepository.clockOut() }
        assertEquals("CLOCKED_OUT", vm.uiState.value.status?.status)
    }

    @Test
    fun `startBreak calls repository and refreshes status`() = runTest {
        coEvery { timeTrackingRepository.getStatus("user-1") } returnsMany listOf(
            Result.success(clockedInStatus),
            Result.success(onBreakStatus),
        )
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startBreak()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { timeTrackingRepository.startBreak() }
        assertEquals("ON_BREAK", vm.uiState.value.status?.status)
    }

    @Test
    fun `endBreak calls repository and refreshes status`() = runTest {
        coEvery { timeTrackingRepository.getStatus("user-1") } returnsMany listOf(
            Result.success(onBreakStatus),
            Result.success(clockedInStatus),
        )
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.endBreak()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { timeTrackingRepository.endBreak() }
        assertEquals("CLOCKED_IN", vm.uiState.value.status?.status)
    }

    @Test
    fun `action failure sets actionError`() = runTest {
        coEvery { timeTrackingRepository.clockIn() } returns Result.failure(Exception("Server error"))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.clockIn()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.actionError)
        assertFalse(vm.uiState.value.isActionLoading)
    }

    @Test
    fun `clearActionError removes actionError`() = runTest {
        coEvery { timeTrackingRepository.clockIn() } returns Result.failure(Exception("Server error"))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.clockIn()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.actionError)

        vm.clearActionError()
        assertNull(vm.uiState.value.actionError)
    }
}
