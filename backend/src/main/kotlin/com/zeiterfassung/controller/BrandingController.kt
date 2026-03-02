package com.zeiterfassung.controller

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.repository.SystemSettingRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@Tag(name = "Branding")
class BrandingController(
    private val auditService: AuditService,
    private val systemSettingRepository: SystemSettingRepository,
    @Value("\${app.branding.directory:/var/lib/zeiterfassung/branding}")
    private val brandingDirectory: String,
) {
    private val logger = LoggerFactory.getLogger(BrandingController::class.java)

    companion object {
        private val ALLOWED_TYPES = setOf("image/png", "image/jpeg", "image/svg+xml", "image/webp")
        private const val MAX_LOGO_SIZE = 2 * 1024 * 1024L // 2 MB
        private const val LOGO_FILENAME = "company-logo"
    }

    @GetMapping("/branding/logo")
    @Operation(summary = "Get company logo", description = "Returns the uploaded company logo image. Public endpoint.")
    fun getLogo(): ResponseEntity<ByteArray> {
        val logoFile = findLogoFile() ?: return ResponseEntity.notFound().build()
        val contentType = contentTypeFor(logoFile.extension)
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(logoFile.readBytes())
    }

    @GetMapping("/branding/info")
    @Operation(summary = "Get company branding info", description = "Returns company name and whether a logo exists. Public endpoint.")
    fun getBrandingInfo(): ResponseEntity<Map<String, Any>> {
        val companyName =
            systemSettingRepository
                .findByKey("company.name")
                .map { it.value ?: "" }
                .orElse("")
        val hasLogo = findLogoFile() != null
        return ResponseEntity.ok(mapOf("companyName" to companyName, "hasLogo" to hasLogo))
    }

    @PostMapping("/admin/branding/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @SecurityRequirement(name = "bearerAuth")
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    @Operation(summary = "Upload company logo", description = "Uploads a company logo image (PNG, JPEG, SVG, WebP, max 2 MB).")
    fun uploadLogo(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Map<String, String>> {
        val contentType = file.contentType ?: ""
        if (contentType !in ALLOWED_TYPES) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Invalid file type. Allowed: PNG, JPEG, SVG, WebP"))
        }
        if (file.size > MAX_LOGO_SIZE) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "File too large. Maximum size: 2 MB"))
        }

        val dir = ensureBrandingDirectory()

        // Remove any existing logo files
        deleteExistingLogos(dir)

        val extension = extensionFor(contentType)
        val logoFile = File(dir, "$LOGO_FILENAME.$extension")
        file.transferTo(logoFile)

        logger.info("Company logo uploaded: {} ({} bytes)", logoFile.name, file.size)
        auditService.logDataChange(
            UUID.fromString(actorId),
            "LOGO_UPLOADED",
            "Branding",
            UUID.randomUUID(),
            null,
            logoFile.name,
        )

        return ResponseEntity.ok(mapOf("status" to "ok", "filename" to logoFile.name))
    }

    @DeleteMapping("/admin/branding/logo")
    @PreAuthorize("hasAuthority('admin.users.manage')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete company logo", description = "Removes the company logo.")
    fun deleteLogo(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Map<String, String>> {
        val dir = File(brandingDirectory)
        val deleted = deleteExistingLogos(dir)
        if (deleted) {
            logger.info("Company logo deleted")
            auditService.logDataChange(
                UUID.fromString(actorId),
                "LOGO_DELETED",
                "Branding",
                UUID.randomUUID(),
                null,
                null,
            )
        }
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }

    private fun findLogoFile(): File? {
        val dir = File(brandingDirectory)
        if (!dir.exists()) return null
        return dir.listFiles { f -> f.isFile && f.nameWithoutExtension == LOGO_FILENAME }?.firstOrNull()
    }

    private fun deleteExistingLogos(dir: File): Boolean {
        if (!dir.exists()) return false
        var deleted = false
        dir.listFiles { f -> f.isFile && f.nameWithoutExtension == LOGO_FILENAME }?.forEach {
            it.delete()
            deleted = true
        }
        return deleted
    }

    private fun ensureBrandingDirectory(): File {
        val dir = File(brandingDirectory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun extensionFor(contentType: String): String =
        when (contentType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/svg+xml" -> "svg"
            "image/webp" -> "webp"
            else -> "png"
        }

    private fun contentTypeFor(extension: String): String =
        when (extension) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
}
