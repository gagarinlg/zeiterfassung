package com.zeiterfassung.service

import com.zeiterfassung.model.dto.LdapConfigResponse
import com.zeiterfassung.model.dto.UpdateLdapConfigRequest
import com.zeiterfassung.repository.SystemSettingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LdapService(
    private val systemSettingRepository: SystemSettingRepository,
) {
    private val logger = LoggerFactory.getLogger(LdapService::class.java)

    fun getLdapConfig(): LdapConfigResponse {
        fun setting(key: String): String = systemSettingRepository.findByKey(key).map { it.value ?: "" }.orElse("")

        return LdapConfigResponse(
            enabled = setting("ldap.enabled").toBoolean(),
            url = setting("ldap.url"),
            baseDn = setting("ldap.base-dn"),
            userSearchBase = setting("ldap.user-search-base"),
            userSearchFilter = setting("ldap.user-search-filter"),
            groupSearchBase = setting("ldap.group-search-base"),
            groupSearchFilter = setting("ldap.group-search-filter"),
            managerDn = setting("ldap.manager-dn"),
            activeDirectoryMode = setting("ldap.active-directory-mode").toBoolean(),
            activeDirectoryDomain = setting("ldap.active-directory-domain"),
            roleMapping = setting("ldap.role-mapping"),
            emailAttribute = setting("ldap.email-attribute"),
            firstNameAttribute = setting("ldap.first-name-attribute"),
            lastNameAttribute = setting("ldap.last-name-attribute"),
            employeeNumberAttribute = setting("ldap.employee-number-attribute"),
        )
    }

    @Transactional
    fun updateLdapConfig(request: UpdateLdapConfigRequest): LdapConfigResponse {
        fun updateSetting(
            key: String,
            value: String?,
        ) {
            if (value == null) return
            val setting =
                systemSettingRepository.findByKey(key).orElseThrow {
                    IllegalStateException("LDAP setting $key not found")
                }
            setting.value = value
            systemSettingRepository.save(setting)
        }

        updateSetting("ldap.enabled", request.enabled?.toString())
        updateSetting("ldap.url", request.url)
        updateSetting("ldap.base-dn", request.baseDn)
        updateSetting("ldap.user-search-base", request.userSearchBase)
        updateSetting("ldap.user-search-filter", request.userSearchFilter)
        updateSetting("ldap.group-search-base", request.groupSearchBase)
        updateSetting("ldap.group-search-filter", request.groupSearchFilter)
        updateSetting("ldap.manager-dn", request.managerDn)
        updateSetting("ldap.manager-password", request.managerPassword)
        updateSetting("ldap.active-directory-mode", request.activeDirectoryMode?.toString())
        updateSetting("ldap.active-directory-domain", request.activeDirectoryDomain)
        updateSetting("ldap.role-mapping", request.roleMapping)
        updateSetting("ldap.email-attribute", request.emailAttribute)
        updateSetting("ldap.first-name-attribute", request.firstNameAttribute)
        updateSetting("ldap.last-name-attribute", request.lastNameAttribute)
        updateSetting("ldap.employee-number-attribute", request.employeeNumberAttribute)

        return getLdapConfig()
    }

    /**
     * Applies an Active Directory preset with reasonable defaults.
     */
    @Transactional
    fun applyActiveDirectoryPreset(
        url: String,
        domain: String,
        baseDn: String,
        managerDn: String,
        managerPassword: String,
    ): LdapConfigResponse =
        updateLdapConfig(
            UpdateLdapConfigRequest(
                enabled = true,
                url = url,
                baseDn = baseDn,
                activeDirectoryMode = true,
                activeDirectoryDomain = domain,
                managerDn = managerDn,
                managerPassword = managerPassword,
                userSearchFilter = "(sAMAccountName={0})",
                groupSearchFilter = "(member={0})",
                emailAttribute = "mail",
                firstNameAttribute = "givenName",
                lastNameAttribute = "sn",
                employeeNumberAttribute = "employeeNumber",
            ),
        )
}
