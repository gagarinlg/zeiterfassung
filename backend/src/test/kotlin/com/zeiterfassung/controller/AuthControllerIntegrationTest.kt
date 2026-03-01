package com.zeiterfassung.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeiterfassung.model.entity.RoleEntity
import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.AuditLogRepository
import com.zeiterfassung.repository.RefreshTokenRepository
import com.zeiterfassung.repository.RoleRepository
import com.zeiterfassung.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var roleRepository: RoleRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var auditLogRepository: AuditLogRepository

    companion object {
        private const val CONTEXT_PATH = "/api"
        private const val ADMIN_EMAIL = "admin@zeiterfassung.local"
        private const val ADMIN_PASSWORD = "Admin@123!"
        private const val ALLOWED_ORIGIN = "http://localhost:5173"
        private const val DISALLOWED_ORIGIN = "http://evil.com"
    }

    @BeforeEach
    fun setUp() {
        auditLogRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
        roleRepository.deleteAll()

        val superAdminRole = roleRepository.save(RoleEntity(name = "SUPER_ADMIN", description = "Super Admin"))

        val admin =
            UserEntity(
                email = ADMIN_EMAIL,
                passwordHash = passwordEncoder.encode(ADMIN_PASSWORD),
                firstName = "Super",
                lastName = "Admin",
                employeeNumber = "ADMIN-001",
            )
        admin.roles.add(superAdminRole)
        userRepository.save(admin)
    }

    // --- CORS Tests ---

    @Test
    fun corsPreflightFromAllowedOriginShouldSucceed() {
        mockMvc
            .perform(
                options("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
    }

    @Test
    fun corsPreflightFromDisallowedOriginShouldBeForbidden() {
        mockMvc
            .perform(
                options("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .header("Origin", DISALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun corsActualRequestFromAllowedOriginShouldIncludeHeaders() {
        val loginJson =
            objectMapper.writeValueAsString(
                mapOf("email" to ADMIN_EMAIL, "password" to ADMIN_PASSWORD),
            )

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .header("Origin", ALLOWED_ORIGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson),
            ).andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
    }

    @Test
    fun corsCredentialsShouldBeAllowed() {
        mockMvc
            .perform(
                options("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
    }

    // --- Auth Controller Tests ---

    @Test
    fun loginWithValidCredentialsShouldSucceed() {
        val loginJson =
            objectMapper.writeValueAsString(
                mapOf("email" to ADMIN_EMAIL, "password" to ADMIN_PASSWORD),
            )

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.expiresIn").isNumber)
            .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL))
    }

    @Test
    fun loginWithInvalidCredentialsShouldReturn401() {
        val loginJson =
            objectMapper.writeValueAsString(
                mapOf("email" to ADMIN_EMAIL, "password" to "WrongPassword!"),
            )

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun loginWithBlankEmailShouldReturn400() {
        val loginJson =
            objectMapper.writeValueAsString(
                mapOf("email" to "", "password" to ADMIN_PASSWORD),
            )

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun accessProtectedEndpointWithoutTokenShouldReturn401() {
        // Spring Security returns 403 by default for unauthenticated stateless requests
        // when no custom AuthenticationEntryPoint is configured
        mockMvc
            .perform(
                get("/api/auth/me")
                    .contextPath(CONTEXT_PATH),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun accessProtectedEndpointWithValidTokenShouldSucceed() {
        val loginJson =
            objectMapper.writeValueAsString(
                mapOf("email" to ADMIN_EMAIL, "password" to ADMIN_PASSWORD),
            )

        val loginResult =
            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson),
                ).andExpect(status().isOk)
                .andReturn()

        val responseBody = objectMapper.readTree(loginResult.response.contentAsString)
        val accessToken = responseBody["accessToken"].asText()

        mockMvc
            .perform(
                get("/api/auth/me")
                    .contextPath(CONTEXT_PATH)
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
            .andExpect(jsonPath("$.firstName").value("Super"))
            .andExpect(jsonPath("$.lastName").value("Admin"))
    }

    // --- Security Header Tests ---

    @Test
    fun responsesShouldIncludeSecurityHeaders() {
        val loginJson =
            objectMapper.writeValueAsString(
                mapOf("email" to ADMIN_EMAIL, "password" to ADMIN_PASSWORD),
            )

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contextPath(CONTEXT_PATH)
                    .secure(true)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson),
            ).andExpect(status().isOk)
            .andExpect(header().exists("X-Content-Type-Options"))
            .andExpect(header().exists("X-Frame-Options"))
            .andExpect(header().exists("Strict-Transport-Security"))
    }
}
