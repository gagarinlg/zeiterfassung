package com.zeiterfassung.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    private val publicPaths = setOf("/auth/login", "/auth/refresh", "/v3/api-docs", "/swagger-ui", "/actuator/health")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestPath = request.requestURI.removePrefix("/api")

        if (publicPaths.any { requestPath.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val token = extractToken(request)
        if (token != null && jwtService.validateToken(token)) {
            val userId = jwtService.extractUserId(token)
            val roles = jwtService.extractRoles(token)
            val permissions = jwtService.extractPermissions(token)

            val authorities =
                buildList {
                    roles.forEach { add(SimpleGrantedAuthority("ROLE_$it")) }
                    permissions.forEach { add(SimpleGrantedAuthority(it)) }
                }

            val authentication = UsernamePasswordAuthenticationToken(userId, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7)
    }
}
