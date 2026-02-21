package com.zeiterfassung.security

import com.zeiterfassung.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(email: String): UserDetails {
        val user =
            userRepository
                .findByEmail(email)
                .orElseThrow { UsernameNotFoundException("User not found: $email") }

        val authorities =
            buildList {
                user.roles.forEach { role ->
                    add(SimpleGrantedAuthority("ROLE_${role.name}"))
                    role.permissions.forEach { perm ->
                        add(SimpleGrantedAuthority(perm.name))
                    }
                }
            }

        return User
            .builder()
            .username(user.id.toString())
            .password(user.passwordHash)
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(user.lockedUntil != null && user.lockedUntil!!.isAfter(Instant.now()))
            .credentialsExpired(false)
            .disabled(!user.isActive || user.isDeleted)
            .build()
    }
}
