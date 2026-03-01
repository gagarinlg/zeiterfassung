package com.zeiterfassung.service

import com.zeiterfassung.model.dto.UpdateLdapConfigRequest
import com.zeiterfassung.model.entity.SystemSettingEntity
import com.zeiterfassung.repository.SystemSettingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LdapServiceTest {
    @Mock private lateinit var systemSettingRepository: SystemSettingRepository

    private lateinit var ldapService: LdapService

    private val ldapKeys =
        listOf(
            "ldap.enabled",
            "ldap.url",
            "ldap.base-dn",
            "ldap.user-search-base",
            "ldap.user-search-filter",
            "ldap.group-search-base",
            "ldap.group-search-filter",
            "ldap.manager-dn",
            "ldap.active-directory-mode",
            "ldap.active-directory-domain",
            "ldap.role-mapping",
            "ldap.email-attribute",
            "ldap.first-name-attribute",
            "ldap.last-name-attribute",
            "ldap.employee-number-attribute",
        )

    @BeforeEach
    fun setUp() {
        ldapService = LdapService(systemSettingRepository)
    }

    private fun stubSetting(
        key: String,
        value: String?,
    ) {
        val entity = SystemSettingEntity(key = key, value = value, description = "Description for $key")
        `when`(systemSettingRepository.findByKey(key)).thenReturn(Optional.of(entity))
    }

    private fun stubAllSettings(value: String = "", includeManagerPassword: Boolean = false) {
        for (key in ldapKeys) {
            stubSetting(key, value)
        }
        if (includeManagerPassword) {
            stubSetting("ldap.manager-password", value)
        }
    }

    // --- getLdapConfig ---

    @Test
    fun `getLdapConfig should return config from system settings`() {
        stubSetting("ldap.enabled", "true")
        stubSetting("ldap.url", "ldap://localhost:389")
        stubSetting("ldap.base-dn", "dc=example,dc=com")
        stubSetting("ldap.user-search-base", "ou=users")
        stubSetting("ldap.user-search-filter", "(uid={0})")
        stubSetting("ldap.group-search-base", "ou=groups")
        stubSetting("ldap.group-search-filter", "(member={0})")
        stubSetting("ldap.manager-dn", "cn=admin")
        stubSetting("ldap.active-directory-mode", "false")
        stubSetting("ldap.active-directory-domain", "")
        stubSetting("ldap.role-mapping", "{}")
        stubSetting("ldap.email-attribute", "mail")
        stubSetting("ldap.first-name-attribute", "givenName")
        stubSetting("ldap.last-name-attribute", "sn")
        stubSetting("ldap.employee-number-attribute", "employeeNumber")

        val result = ldapService.getLdapConfig()

        assertThat(result.enabled).isTrue()
        assertThat(result.url).isEqualTo("ldap://localhost:389")
        assertThat(result.baseDn).isEqualTo("dc=example,dc=com")
        assertThat(result.userSearchBase).isEqualTo("ou=users")
        assertThat(result.userSearchFilter).isEqualTo("(uid={0})")
        assertThat(result.groupSearchBase).isEqualTo("ou=groups")
        assertThat(result.groupSearchFilter).isEqualTo("(member={0})")
        assertThat(result.managerDn).isEqualTo("cn=admin")
        assertThat(result.activeDirectoryMode).isFalse()
        assertThat(result.emailAttribute).isEqualTo("mail")
        assertThat(result.firstNameAttribute).isEqualTo("givenName")
        assertThat(result.lastNameAttribute).isEqualTo("sn")
        assertThat(result.employeeNumberAttribute).isEqualTo("employeeNumber")
    }

    @Test
    fun `getLdapConfig should return defaults when settings not found`() {
        for (key in ldapKeys) {
            `when`(systemSettingRepository.findByKey(key)).thenReturn(Optional.empty())
        }

        val result = ldapService.getLdapConfig()

        assertThat(result.enabled).isFalse()
        assertThat(result.url).isEmpty()
        assertThat(result.baseDn).isEmpty()
        assertThat(result.activeDirectoryMode).isFalse()
    }

    // --- updateLdapConfig ---

    @Test
    fun `updateLdapConfig should update existing settings`() {
        stubAllSettings()
        `when`(systemSettingRepository.save(any<SystemSettingEntity>())).thenAnswer { it.arguments[0] }

        val request =
            UpdateLdapConfigRequest(
                enabled = true,
                url = "ldap://new-server:389",
                baseDn = "dc=new,dc=com",
            )

        val result = ldapService.updateLdapConfig(request)

        assertThat(result).isNotNull()
        verify(systemSettingRepository, atLeastOnce()).save(any<SystemSettingEntity>())
    }

    @Test
    fun `updateLdapConfig should skip null values`() {
        stubAllSettings()
        `when`(systemSettingRepository.save(any<SystemSettingEntity>())).thenAnswer { it.arguments[0] }

        val request = UpdateLdapConfigRequest(url = "ldap://updated:389")

        val result = ldapService.updateLdapConfig(request)

        assertThat(result).isNotNull()
    }

    @Test
    fun `updateLdapConfig should throw when setting key not found`() {
        `when`(systemSettingRepository.findByKey("ldap.enabled")).thenReturn(Optional.empty())

        val request = UpdateLdapConfigRequest(enabled = true)

        assertThrows<IllegalStateException> {
            ldapService.updateLdapConfig(request)
        }
    }
}
