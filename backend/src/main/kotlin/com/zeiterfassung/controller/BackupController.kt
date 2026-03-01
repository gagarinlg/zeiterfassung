package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.BackupInfo
import com.zeiterfassung.model.dto.RestoreResponse
import com.zeiterfassung.service.BackupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/admin/backups")
@PreAuthorize("hasAuthority('admin.users.manage')")
@Tag(name = "Backup")
@SecurityRequirement(name = "bearerAuth")
class BackupController(
    private val backupService: BackupService,
) {
    @GetMapping
    @Operation(summary = "List backups", description = "Returns all available database backups sorted by date descending.")
    @ApiResponse(responseCode = "200", description = "List of backups returned")
    fun listBackups(): ResponseEntity<List<BackupInfo>> = ResponseEntity.ok(backupService.listBackups())

    @PostMapping
    @Operation(summary = "Create backup", description = "Triggers a manual database backup.")
    @ApiResponse(responseCode = "200", description = "Backup created successfully")
    @ApiResponse(responseCode = "500", description = "Backup failed")
    fun createBackup(
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<BackupInfo> = ResponseEntity.ok(backupService.performBackup(UUID.fromString(actorId)))

    @GetMapping("/{filename}")
    @Operation(summary = "Download backup", description = "Downloads a specific backup file.")
    @ApiResponse(responseCode = "200", description = "Backup file returned")
    @ApiResponse(responseCode = "400", description = "Invalid filename")
    @ApiResponse(responseCode = "404", description = "Backup file not found")
    fun downloadBackup(
        @PathVariable filename: String,
    ): ResponseEntity<Resource> {
        val file = backupService.getBackupFile(filename)
        val resource = FileSystemResource(file)
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.name}\"")
            .body(resource)
    }

    @PostMapping("/restore/{filename}")
    @Operation(summary = "Restore from backup", description = "Restores the database from an existing backup file.")
    @ApiResponse(responseCode = "200", description = "Database restored successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filename")
    @ApiResponse(responseCode = "500", description = "Restore failed")
    fun restoreFromBackup(
        @PathVariable filename: String,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<RestoreResponse> {
        backupService.restoreFromBackup(filename, UUID.fromString(actorId))
        return ResponseEntity.ok(RestoreResponse(status = "ok", message = "Database restored from $filename"))
    }

    @PostMapping("/restore/upload")
    @Operation(summary = "Restore from upload", description = "Restores the database from an uploaded backup file.")
    @ApiResponse(responseCode = "200", description = "Database restored successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file")
    @ApiResponse(responseCode = "500", description = "Restore failed")
    fun restoreFromUpload(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<RestoreResponse> {
        backupService.restoreFromUpload(file.inputStream, file.originalFilename ?: "upload.sql.gz", UUID.fromString(actorId))
        return ResponseEntity.ok(RestoreResponse(status = "ok", message = "Database restored from uploaded file"))
    }

    @DeleteMapping("/{filename}")
    @Operation(summary = "Delete backup", description = "Deletes a specific backup file.")
    @ApiResponse(responseCode = "204", description = "Backup deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filename")
    @ApiResponse(responseCode = "404", description = "Backup file not found")
    fun deleteBackup(
        @PathVariable filename: String,
        @AuthenticationPrincipal actorId: String,
    ): ResponseEntity<Void> {
        backupService.deleteBackup(filename, UUID.fromString(actorId))
        return ResponseEntity.noContent().build()
    }
}
