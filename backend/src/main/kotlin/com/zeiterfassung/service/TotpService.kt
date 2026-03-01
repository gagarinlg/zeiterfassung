package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.exception.ResourceNotFoundException
import com.zeiterfassung.exception.UnauthorizedException
import com.zeiterfassung.model.dto.TotpSetupResponse
import com.zeiterfassung.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TotpService(
    private val userRepository: UserRepository,
    private val auditService: AuditService,
) {
    companion object {
        private const val SECRET_LENGTH = 20
        private const val CODE_DIGITS = 6
        private const val TIME_STEP_SECONDS = 30L
        private const val ISSUER = "Zeiterfassung"
        private val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()
    }

    fun generateSetup(userId: UUID): TotpSetupResponse {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found") }

        val secretBytes = ByteArray(SECRET_LENGTH)
        SecureRandom().nextBytes(secretBytes)
        val secret = base32Encode(secretBytes)

        val otpAuthUri =
            "otpauth://totp/$ISSUER:${user.email}?secret=$secret&issuer=$ISSUER&digits=$CODE_DIGITS&period=$TIME_STEP_SECONDS"
        return TotpSetupResponse(secret = secret, qrCodeUri = otpAuthUri)
    }

    @Transactional
    fun enableTotp(
        userId: UUID,
        secret: String,
        code: String,
    ) {
        if (!verifyCode(secret, code)) {
            throw UnauthorizedException("Invalid TOTP code")
        }
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found") }
        user.totpSecret = secret
        user.totpEnabled = true
        userRepository.save(user)
        auditService.logDataChange(userId, "TOTP_ENABLED", "User", userId, null, null)
    }

    @Transactional
    fun disableTotp(userId: UUID) {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found") }
        user.totpSecret = null
        user.totpEnabled = false
        userRepository.save(user)
        auditService.logDataChange(userId, "TOTP_DISABLED", "User", userId, null, null)
    }

    fun verifyCode(
        secret: String,
        code: String,
    ): Boolean {
        val timeStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS
        for (i in -1..1) {
            val computed = generateCode(secret, timeStep + i)
            if (computed == code) return true
        }
        return false
    }

    private fun generateCode(
        secret: String,
        counter: Long,
    ): String {
        val key = base32Decode(secret)
        val data = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val truncated =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val code = truncated % Math.pow(10.0, CODE_DIGITS.toDouble()).toInt()
        return code.toString().padStart(CODE_DIGITS, '0')
    }

    private fun base32Encode(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS[(buffer shr (bitsLeft - 5)) and 0x1F])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return sb.toString()
    }

    private fun base32Decode(encoded: String): ByteArray {
        val data = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (c in encoded.uppercase()) {
            val idx = BASE32_CHARS.indexOf(c)
            if (idx < 0) continue
            buffer = (buffer shl 5) or idx
            bitsLeft += 5
            if (bitsLeft >= 8) {
                data.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return data.toByteArray()
    }
}
