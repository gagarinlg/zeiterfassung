package com.zeiterfassung.config

import com.zeiterfassung.model.entity.UserEntity
import com.zeiterfassung.repository.RoleRepository
import com.zeiterfassung.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("!test")
class DataSeeder(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.seed.admin-password:Admin@123!}") private val adminPassword: String,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(DataSeeder::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        seedSuperAdmin()
    }

    private fun seedSuperAdmin() {
        val adminEmail = "admin@zeiterfassung.local"
        if (userRepository.existsByEmail(adminEmail)) {
            logger.debug("Super Admin already exists, skipping seed")
            return
        }

        val superAdminRole =
            roleRepository.findByName("SUPER_ADMIN").orElse(null) ?: run {
                logger.warn("SUPER_ADMIN role not found in database, skipping seed")
                return
            }

        val admin =
            UserEntity(
                email = adminEmail,
                passwordHash = passwordEncoder.encode(adminPassword),
                firstName = "Super",
                lastName = "Admin",
                employeeNumber = "ADMIN-001",
            )
        admin.roles.add(superAdminRole)
        userRepository.save(admin)
        logger.info("Default Super Admin created: {}", adminEmail)
    }
}
