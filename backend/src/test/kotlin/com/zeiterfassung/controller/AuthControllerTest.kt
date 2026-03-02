package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.AuthResponse
import com.zeiterfassung.model.dto.LoginRequest
import com.zeiterfassung.model.dto.PasswordResetConfirmRequest
import com.zeiterfassung.model.dto.PasswordResetLinkRequest
import com.zeiterfassung.model.dto.RefreshRequest
import com.zeiterfassung.model.dto.TotpSetupResponse
import com.zeiterfassung.model.dto.TotpVerifyRequest
import com.zeiterfassung.model.dto.UserResponse
import com.zeiterfassung.service.AuthService
import com.zeiterfassung.service.PasswordResetService
import com.zeiterfassung.service.TotpService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthControllerTest {
    @Mock
    private lateinit var authService: AuthService

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var passwordResetService: PasswordResetService

    private lateinit var controller: AuthController

    private val userId = UUID.randomUUID()
    private val actorId = userId.toString()

    private fun sampleUserResponse(): UserResponse =
        UserResponse(
            id = userId.toString(),
            email = "user@test.com",
            firstName = "Max",
            lastName = "Mustermann",
            employeeNumber = null,
            phone = null,
            photoUrl = null,
            managerId = null,
            substituteId = null,
            isActive = true,
            roles = listOf("EMPLOYEE"),
            permissions = listOf("time.edit.own"),
            dateFormat = null,
            timeFormat = null,
        )

    private fun sampleAuthResponse(): AuthResponse =
        AuthResponse(
            accessToken = "access-token-jwt",
            refreshToken = "refresh-token-jwt",
            expiresIn = 900,
            user = sampleUserResponse(),
        )

    @BeforeEach
    fun setUp() {
        controller = AuthController(authService, totpService, passwordResetService)
    }

    @Test
    fun `login should return auth response`() {
        val request = LoginRequest("user@test.com", "password123", null)
        val httpRequest = MockHttpServletRequest()
        val expected = sampleAuthResponse()
        `when`(authService.login(request, httpRequest)).thenReturn(expected)

        val response = controller.login(request, httpRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.accessToken).isEqualTo("access-token-jwt")
    }

    @Test
    fun `refresh should return new auth response`() {
        val request = RefreshRequest("refresh-token-jwt")
        val expected = sampleAuthResponse()
        `when`(authService.refreshToken(request)).thenReturn(expected)

        val response = controller.refresh(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `logout should return no content`() {
        val httpRequest = MockHttpServletRequest()
        val body = mapOf("refreshToken" to "refresh-token")

        val response = controller.logout(actorId, body, httpRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(authService).logout(userId, "refresh-token", httpRequest)
    }

    @Test
    fun `logout should handle null body`() {
        val httpRequest = MockHttpServletRequest()

        val response = controller.logout(actorId, null, httpRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(authService).logout(userId, null, httpRequest)
    }

    @Test
    fun `logoutAll should return no content`() {
        val httpRequest = MockHttpServletRequest()

        val response = controller.logoutAll(actorId, httpRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(authService).logoutAll(userId, httpRequest)
    }

    @Test
    fun `getCurrentUser should return user profile`() {
        val expected = sampleUserResponse()
        `when`(authService.getCurrentUser(userId)).thenReturn(expected)

        val response = controller.getCurrentUser(actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.email).isEqualTo("user@test.com")
    }

    @Test
    fun `setupTotp should return TOTP setup response`() {
        val setup = TotpSetupResponse("SECRET123", "otpauth://totp/Zeiterfassung:user@test.com?secret=SECRET123")
        `when`(totpService.generateSetup(userId)).thenReturn(setup)

        val response = controller.setupTotp(actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.secret).isEqualTo("SECRET123")
    }

    @Test
    fun `enableTotp should return no content`() {
        val request = TotpVerifyRequest("123456", "SECRET123")

        val response = controller.enableTotp(request, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(totpService).enableTotp(userId, "SECRET123", "123456")
    }

    @Test
    fun `disableTotp should return no content`() {
        val response = controller.disableTotp(actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(totpService).disableTotp(userId)
    }

    @Test
    fun `requestPasswordReset should return ok`() {
        val request = PasswordResetLinkRequest("user@test.com")

        val response = controller.requestPasswordReset(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(passwordResetService).requestPasswordReset("user@test.com")
    }

    @Test
    fun `confirmPasswordReset should return no content`() {
        val request = PasswordResetConfirmRequest("token123", "newpass123", "newpass123")

        val response = controller.confirmPasswordReset(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(passwordResetService).confirmPasswordReset(request)
    }
}
