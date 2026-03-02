package com.zeiterfassung.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import java.net.URI

class GlobalExceptionHandlerTest {
    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    @Test
    fun `handleNotFound should return 404 with message`() {
        val ex = ResourceNotFoundException("User not found")
        val result = handler.handleNotFound(ex)

        assertThat(result.status).isEqualTo(HttpStatus.NOT_FOUND.value())
        assertThat(result.detail).isEqualTo("User not found")
        assertThat(result.type).isEqualTo(URI.create("about:not-found"))
    }

    @Test
    fun `handleUnauthorized should return 401 with message`() {
        val ex = UnauthorizedException("Invalid credentials")
        val result = handler.handleUnauthorized(ex)

        assertThat(result.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(result.detail).isEqualTo("Invalid credentials")
        assertThat(result.type).isEqualTo(URI.create("about:unauthorized"))
    }

    @Test
    fun `handleForbidden should return 403 with message`() {
        val ex = ForbiddenException("Access denied")
        val result = handler.handleForbidden(ex)

        assertThat(result.status).isEqualTo(HttpStatus.FORBIDDEN.value())
        assertThat(result.detail).isEqualTo("Access denied")
        assertThat(result.type).isEqualTo(URI.create("about:forbidden"))
    }

    @Test
    fun `handleDuplicate should return 409 with message`() {
        val ex = DuplicateResourceException("Email already exists")
        val result = handler.handleDuplicate(ex)

        assertThat(result.status).isEqualTo(HttpStatus.CONFLICT.value())
        assertThat(result.detail).isEqualTo("Email already exists")
        assertThat(result.type).isEqualTo(URI.create("about:conflict"))
    }

    @Test
    fun `handleConflict should return 409 with message`() {
        val ex = ConflictException("Concurrent modification")
        val result = handler.handleConflict(ex)

        assertThat(result.status).isEqualTo(HttpStatus.CONFLICT.value())
        assertThat(result.detail).isEqualTo("Concurrent modification")
        assertThat(result.type).isEqualTo(URI.create("about:conflict"))
    }

    @Test
    fun `handleBadRequest should return 400 with message`() {
        val ex = BadRequestException("Invalid input")
        val result = handler.handleBadRequest(ex)

        assertThat(result.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(result.detail).isEqualTo("Invalid input")
        assertThat(result.type).isEqualTo(URI.create("about:bad-request"))
    }

    @Test
    fun `handleAccountLocked should return 423 with message`() {
        val ex = AccountLockedException("Account is locked for 15 minutes")
        val result = handler.handleAccountLocked(ex)

        assertThat(result.status).isEqualTo(HttpStatus.LOCKED.value())
        assertThat(result.detail).isEqualTo("Account is locked for 15 minutes")
        assertThat(result.type).isEqualTo(URI.create("about:account-locked"))
    }

    @Test
    fun `handleRateLimit should return 429 with message`() {
        val ex = RateLimitExceededException("Too many login attempts")
        val result = handler.handleRateLimit(ex)

        assertThat(result.status).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value())
        assertThat(result.detail).isEqualTo("Too many login attempts")
        assertThat(result.type).isEqualTo(URI.create("about:rate-limit-exceeded"))
    }

    @Test
    fun `handleValidation should return 400 with field errors`() {
        val target = Object()
        val bindingResult = BeanPropertyBindingResult(target, "request")
        bindingResult.addError(FieldError("request", "email", "must not be blank"))
        bindingResult.addError(FieldError("request", "password", "must be at least 8 characters"))

        val methodParam =
            org.springframework.core.MethodParameter(
                this::class.java.getDeclaredMethod("setUp"),
                -1,
            )
        val ex = MethodArgumentNotValidException(methodParam, bindingResult)

        val result = handler.handleValidation(ex)

        assertThat(result.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(result.detail).isEqualTo("Validation failed")
        assertThat(result.type).isEqualTo(URI.create("about:validation-error"))
        @Suppress("UNCHECKED_CAST")
        val errors = result.properties?.get("errors") as? Map<String, String>
        assertThat(errors).containsEntry("email", "must not be blank")
        assertThat(errors).containsEntry("password", "must be at least 8 characters")
    }

    @Test
    fun `handleValidation should use default message when field error message is null`() {
        val target = Object()
        val bindingResult = BeanPropertyBindingResult(target, "request")
        bindingResult.addError(FieldError("request", "field1", null, false, null, null, null))
        val methodParam =
            org.springframework.core.MethodParameter(
                this::class.java.getDeclaredMethod("setUp"),
                -1,
            )
        val ex = MethodArgumentNotValidException(methodParam, bindingResult)

        val result = handler.handleValidation(ex)

        @Suppress("UNCHECKED_CAST")
        val errors = result.properties?.get("errors") as? Map<String, String>
        assertThat(errors).containsEntry("field1", "Invalid value")
    }

    @Test
    fun `handleGeneral should return 500 for unexpected errors`() {
        val ex = RuntimeException("Something went wrong")
        val result = handler.handleGeneral(ex)

        assertThat(result.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(result.detail).isEqualTo("An unexpected error occurred")
        assertThat(result.type).isEqualTo(URI.create("about:internal-error"))
    }

    @Test
    fun `handleGeneral should not expose internal error message`() {
        val ex = NullPointerException("internal NPE details")
        val result = handler.handleGeneral(ex)

        assertThat(result.detail).doesNotContain("internal NPE details")
        assertThat(result.detail).isEqualTo("An unexpected error occurred")
    }
}
