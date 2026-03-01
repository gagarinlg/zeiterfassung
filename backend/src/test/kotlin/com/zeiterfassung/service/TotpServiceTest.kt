package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.lang.reflect.Method
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TotpServiceTest {
    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var auditService: AuditService

    private lateinit var totpService: TotpService

    @BeforeEach
    fun setUp() {
        totpService = TotpService(userRepository, auditService)
    }

    private fun makeUser(id: UUID = UUID.randomUUID()) =
        UserEntity(
            id = id,
            email = "user@test.com",
            passwordHash = "hash",
            firstName = "Test",
            lastName = "User",
        )

    private fun generateCode(secret: String): String {
        val method: Method = TotpService::class.java.getDeclaredMethod("generateCode", String::class.java, Long::class.javaPrimitiveType)
        method.isAccessible = true
        val timeStep = System.currentTimeMillis() / 1000 / 30L
        return method.invoke(totpService, secret, timeStep) as String
    }

    // --- generateSetup ---

    @Test
    fun `generateSetup should return setup with secret and QR URI`() {
        val userId = UUID.randomUUID()
        val user = makeUser(userId)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = totpService.generateSetup(userId)

        assertThat(result.secret).isNotBlank()
        assertThat(result.qrCodeUri).contains("otpauth://totp/")
        assertThat(result.qrCodeUri).contains(user.email)
        assertThat(result.qrCodeUri).contains("secret=${result.secret}")
    }

    @Test
    fun `generateSetup should throw ResourceNotFoundException when user not found`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            totpService.generateSetup(userId)
        }
    }

    // --- enableTotp ---

    @Test
    fun `enableTotp should save user with totp enabled when code is valid`() {
        val userId = UUID.randomUUID()
        val user = makeUser(userId)
        val secret = "JBSWY3DPEHPK3PXP"
        val code = generateCode(secret)
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any<UserEntity>())).thenReturn(user)

        totpService.enableTotp(userId, secret, code)

        assertThat(user.totpSecret).isEqualTo(secret)
        assertThat(user.totpEnabled).isTrue()
        verify(userRepository).save(user)
    }

    @Test
    fun `enableTotp should throw UnauthorizedException when code is invalid`() {
        val userId = UUID.randomUUID()
        val secret = "JBSWY3DPEHPK3PXP"

        assertThrows<UnauthorizedException> {
            totpService.enableTotp(userId, secret, "000000")
        }
    }

    @Test
    fun `enableTotp should throw ResourceNotFoundException when user not found`() {
        val userId = UUID.randomUUID()
        val secret = "JBSWY3DPEHPK3PXP"
        val code = generateCode(secret)
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            totpService.enableTotp(userId, secret, code)
        }
    }

    // --- disableTotp ---

    @Test
    fun `disableTotp should clear totp fields`() {
        val userId = UUID.randomUUID()
        val user = makeUser(userId).apply {
            totpSecret = "JBSWY3DPEHPK3PXP"
            totpEnabled = true
        }
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any<UserEntity>())).thenReturn(user)

        totpService.disableTotp(userId)

        assertThat(user.totpSecret).isNull()
        assertThat(user.totpEnabled).isFalse()
        verify(userRepository).save(user)
    }

    @Test
    fun `disableTotp should throw ResourceNotFoundException when user not found`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            totpService.disableTotp(userId)
        }
    }

    // --- verifyCode ---

    @Test
    fun `verifyCode should return true for a valid generated code`() {
        val secret = "JBSWY3DPEHPK3PXP"
        val code = generateCode(secret)

        val result = totpService.verifyCode(secret, code)

        assertThat(result).isTrue()
    }
}
