package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.model.dto.BackupInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class BackupService(
    private val auditService: AuditService,
    @Value("\${app.backup.directory:/var/lib/zeiterfassung/backups}")
    private val backupDirectory: String,
    @Value("\${app.backup.max-count:31}")
    private val maxBackupCount: Int,
    @Value("\${spring.datasource.url}")
    private val datasourceUrl: String,
    @Value("\${spring.datasource.username}")
    private val datasourceUsername: String,
    @Value("\${spring.datasource.password}")
    private val datasourcePassword: String,
) {
    private val logger = LoggerFactory.getLogger(BackupService::class.java)

    companion object {
        private val FILENAME_PATTERN = Regex("^[a-zA-Z0-9._-]+$")
        private val BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        private val SAFE_SHELL_ARG = Regex("^[a-zA-Z0-9._:/-]+$")
    }

    @Scheduled(cron = "0 0 2 * * *")
    fun scheduledBackup() {
        try {
            logger.info("Starting scheduled daily backup")
            performBackup()
            logger.info("Scheduled daily backup completed successfully")
        } catch (e: Exception) {
            logger.error("Scheduled daily backup failed: {}", e.message, e)
        }
    }

    fun performBackup(actorId: UUID? = null): BackupInfo {
        val backupDir = ensureBackupDirectory()
        val timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT)
        val filename = "zeiterfassung_backup_$timestamp.sql.gz"
        val backupFile = File(backupDir, filename)

        val (host, port, dbName) = parseDatasourceUrl()
        val safeUsername = shellEscape(datasourceUsername)

        logger.info("Performing backup to {}", backupFile.absolutePath)

        val process =
            ProcessBuilder(
                "bash",
                "-c",
                "pg_dump -h $host -p $port -U $safeUsername -d $dbName | gzip > ${backupFile.absolutePath}",
            ).apply {
                environment()["PGPASSWORD"] = datasourcePassword
                redirectErrorStream(false)
            }.start()

        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            backupFile.delete()
            logger.error("pg_dump failed with exit code {}: {}", exitCode, stderr)
            throw IllegalStateException("Backup failed: $stderr")
        }

        logger.info("Backup created successfully: {} ({} bytes)", filename, backupFile.length())

        if (actorId != null) {
            auditService.logDataChange(actorId, "BACKUP_CREATED", "Backup", UUID.randomUUID(), null, filename)
        }

        cleanupOldBackups()

        return BackupInfo(
            filename = filename,
            sizeBytes = backupFile.length(),
            createdAt = Instant.ofEpochMilli(backupFile.lastModified()),
        )
    }

    fun listBackups(): List<BackupInfo> {
        val backupDir = File(backupDirectory)
        if (!backupDir.exists()) return emptyList()

        val files = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".sql.gz") }
        return files
            ?.map { file ->
                BackupInfo(
                    filename = file.name,
                    sizeBytes = file.length(),
                    createdAt = Instant.ofEpochMilli(file.lastModified()),
                )
            }?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun getBackupFile(filename: String): File {
        validateFilename(filename)
        val file = File(backupDirectory, filename)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("Backup file not found: $filename")
        }
        if (!file.canonicalPath.startsWith(File(backupDirectory).canonicalPath)) {
            throw IllegalArgumentException("Invalid backup file path")
        }
        return file
    }

    fun restoreFromBackup(
        filename: String,
        actorId: UUID,
    ) {
        validateFilename(filename)
        val backupFile = getBackupFile(filename)
        logger.info("Restoring from backup: {}", filename)
        executeRestore(backupFile)
        auditService.logDataChange(actorId, "BACKUP_RESTORED", "Backup", UUID.randomUUID(), null, filename)
        logger.info("Restore from backup completed: {}", filename)
    }

    fun restoreFromUpload(
        inputStream: InputStream,
        originalFilename: String,
        actorId: UUID,
    ) {
        val safeName = originalFilename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        validateFilename(safeName)

        val tempFile = Files.createTempFile("zeiterfassung_upload_", ".sql.gz").toFile()
        try {
            inputStream.use { input ->
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            logger.info("Restoring from uploaded file: {} ({} bytes)", safeName, tempFile.length())
            executeRestore(tempFile)
            auditService.logDataChange(actorId, "BACKUP_RESTORED_UPLOAD", "Backup", UUID.randomUUID(), null, safeName)
            logger.info("Restore from uploaded file completed: {}", safeName)
        } finally {
            tempFile.delete()
        }
    }

    fun deleteBackup(
        filename: String,
        actorId: UUID,
    ) {
        validateFilename(filename)
        val file = getBackupFile(filename)
        if (file.delete()) {
            logger.info("Deleted backup: {}", filename)
            auditService.logDataChange(actorId, "BACKUP_DELETED", "Backup", UUID.randomUUID(), null, filename)
        } else {
            throw IllegalStateException("Failed to delete backup: $filename")
        }
    }

    private fun cleanupOldBackups() {
        val backupDir = File(backupDirectory)
        if (!backupDir.exists()) return

        val backups =
            backupDir
                .listFiles { file -> file.isFile && file.name.endsWith(".sql.gz") }
                ?.sortedByDescending { it.lastModified() }
                ?: return

        if (backups.size > maxBackupCount) {
            backups.drop(maxBackupCount).forEach { file ->
                logger.info("Removing old backup: {}", file.name)
                file.delete()
            }
        }
    }

    private fun executeRestore(backupFile: File) {
        val (host, port, dbName) = parseDatasourceUrl()
        val safeUsername = shellEscape(datasourceUsername)

        val process =
            ProcessBuilder(
                "bash",
                "-c",
                "gunzip -c ${backupFile.absolutePath} | psql -h $host -p $port -U $safeUsername -d $dbName",
            ).apply {
                environment()["PGPASSWORD"] = datasourcePassword
                redirectErrorStream(false)
            }.start()

        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            logger.error("Restore failed with exit code {}: {}", exitCode, stderr)
            throw IllegalStateException("Restore failed: $stderr")
        }
    }

    private fun validateFilename(filename: String) {
        require(FILENAME_PATTERN.matches(filename)) { "Invalid filename: $filename" }
    }

    private fun parseDatasourceUrl(): Triple<String, String, String> {
        val uri = URI(datasourceUrl.removePrefix("jdbc:"))
        val host = shellEscape(uri.host ?: "localhost")
        val port = shellEscape(if (uri.port > 0) uri.port.toString() else "5432")
        val dbName = shellEscape(uri.path.removePrefix("/"))
        return Triple(host, port, dbName)
    }

    private fun shellEscape(value: String): String {
        require(SAFE_SHELL_ARG.matches(value)) {
            "Unsafe shell argument detected: $value"
        }
        return value
    }

    private fun ensureBackupDirectory(): File {
        val dir = File(backupDirectory)
        if (!dir.exists()) {
            dir.mkdirs()
            logger.info("Created backup directory: {}", dir.absolutePath)
        }
        return dir
    }
}
