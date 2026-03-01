package com.zeiterfassung.app

import app.cash.turbine.test
import com.zeiterfassung.app.data.model.LoginResponse
import com.zeiterfassung.app.data.model.User
import com.zeiterfassung.app.data.repository.AuthRepository
import com.zeiterfassung.app.ui.viewmodel.AuthUiState
import com.zeiterfassung.app.ui.viewmodel.AuthViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private lateinit var viewModel: AuthViewModel

    private val testUser = User(
        id = "user-1",
        email = "test@test.com",
        firstName = "Test",
        lastName = "User",
        employeeNumber = null,
        phone = null,
        photoUrl = null,
        managerId = null,
        isActive = true,
        roles = listOf("EMPLOYEE"),
        permissions = emptyList(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.isLoggedIn() } returns false
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle when not logged in`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
        assertFalse(viewModel.isAuthenticated.value)
    }

    @Test
    fun `initial state reflects authenticated user`() = runTest {
        coEvery { authRepository.isLoggedIn() } returns true
        val vm = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.isAuthenticated.value)
    }

    @Test
    fun `login with valid credentials sets Success state`() = runTest {
        val mockResponse = LoginResponse(
            accessToken = "token123",
            refreshToken = "refresh123",
            expiresIn = 900,
            user = testUser,
        )
        coEvery { authRepository.login("test@test.com", "password") } returns Result.success(mockResponse)

        viewModel.uiState.test {
            assertEquals(AuthUiState.Idle, awaitItem())
            viewModel.login("test@test.com", "password")
            assertEquals(AuthUiState.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is AuthUiState.Success)
            assertEquals(testUser, (success as AuthUiState.Success).user)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.isAuthenticated.value)
    }

    @Test
    fun `login with invalid credentials sets Error state`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(Exception("Unauthorized"))

        viewModel.uiState.test {
            assertEquals(AuthUiState.Idle, awaitItem())
            viewModel.login("bad@test.com", "wrong")
            assertEquals(AuthUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is AuthUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.isAuthenticated.value)
    }

    @Test
    fun `login with empty email sets Error state without API call`() = runTest {
        viewModel.uiState.test {
            assertEquals(AuthUiState.Idle, awaitItem())
            viewModel.login("", "password")
            val error = awaitItem()
            assertTrue(error is AuthUiState.Error)
            assertEquals("email_and_password_required", (error as AuthUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `login with empty password sets Error state without API call`() = runTest {
        viewModel.uiState.test {
            assertEquals(AuthUiState.Idle, awaitItem())
            viewModel.login("test@test.com", "")
            val error = awaitItem()
            assertTrue(error is AuthUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `logout clears authentication state`() = runTest {
        // First simulate a logged in state
        coEvery { authRepository.isLoggedIn() } returns true
        val vm = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.isAuthenticated.value)

        // Now log out
        vm.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.isAuthenticated.value)
        assertEquals(AuthUiState.Idle, vm.uiState.value)
        coVerify { authRepository.logout() }
    }

    @Test
    fun `clearError resets Error state to Idle`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(Exception("error"))
        viewModel.login("bad@test.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        viewModel.clearError()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `clearError does nothing when state is not Error`() = runTest {
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
        viewModel.clearError()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }
}
