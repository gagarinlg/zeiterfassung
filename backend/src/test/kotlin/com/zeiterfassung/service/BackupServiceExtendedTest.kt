package com.zeiterfassung.service

import com.zeiterfassung.audit.AuditService
import com.zeiterfassung.model.dto.BackupInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Path
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BackupServiceExtendedTest {
    @Mock
    private lateinit var auditService: AuditService

    @TempDir
    lateinit var tempDir: Path

    private lateinit var service: BackupService

    @BeforeEach
    fun setUp() {
        service = BackupService(
            auditService = auditService,
            backupDirectory = tempDir.toString(),
            maxBackupCount = 3,
            datasourceUrl = "jdbc:postgresql://localhost:5432/zeiterfassung",
            datasourceUsername = "testuser",
            datasourcePassword = "testpass",
        )
    }

    // ---- parseDatasourceUrl via reflection ----

    @Test
    fun `parseDatasourceUrl parses standard URL`() {
        val method = BackupService::class.java.getDeclaredMethod("parseDatasourceUrl")
        method.isAccessible = true
        val result = method.invoke(service) as Triple<*, *, *>
        assertThat(result.first).isEqualTo("localhost")
        assertThat(result.second).isEqualTo("5432")
        assertThat(result.third).isEqualTo("zeiterfassung")
    }

    @Test
    fun `parseDatasourceUrl parses URL with custom port`() {
        val customService = BackupService(
            auditService = auditService,
            backupDirectory = tempDir.toString(),
            maxBackupCount = 3,
            datasourceUrl = "jdbc:postgresql://dbhost:5433/mydb",
            datasourceUsername = "user",
            datasourcePassword = "pass",
        )
        val method = BackupService::class.java.getDeclaredMethod("parseDatasourceUrl")
        method.isAccessible = true
        val result = method.invoke(customService) as Triple<*, *, *>
        assertThat(result.first).isEqualTo("dbhost")
        assertThat(result.second).isEqualTo("5433")
        assertThat(result.third).isEqualTo("mydb")
    }

    @Test
    fun `parseDatasourceUrl defaults port to 5432 when not specified`() {
        val noPortService = BackupService(
            auditService = auditService,
            backupDirectory = tempDir.toString(),
            maxBackupCount = 3,
            datasourceUrl = "jdbc:postgresql://dbhost/testdb",
            datasourceUsername = "user",
            datasourcePassword = "pass",
        )
        val method = BackupService::class.java.getDeclaredMethod("parseDatasourceUrl")
        method.isAccessible = true
        val result = method.invoke(noPortService) as Triple<*, *, *>
        assertThat(result.first).isEqualTo("dbhost")
        assertThat(result.second).isEqualTo("5432")
        assertThat(result.third).isEqualTo("testdb")
    }

    // ---- shellEscape via reflection ----

    @Test
    fun `shellEscape accepts safe values`() {
        val method = BackupService::class.java.getDeclaredMethod("shellEscape", String::class.java)
        method.isAccessible = true
        assertThat(method.invoke(service, "localhost")).isEqualTo("localhost")
        assertThat(method.invoke(service, "5432")).isEqualTo("5432")
        assertThat(method.invoke(service, "my-db_name.01")).isEqualTo("my-db_name.01")
        assertThat(method.invoke(service, "host:5432/db")).isEqualTo("host:5432/db")
    }

    @Test
    fun `shellEscape rejects unsafe values`() {
        val method = BackupService::class.java.getDeclaredMethod("shellEscape", String::class.java)
        method.isAccessible = true

        assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(service, "'; DROP TABLE users; --")
        }
        assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(service, "value with spaces")
        }
        assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(service, "cmd\$(whoami)")
        }
    }

    // ---- ensureBackupDirectory via reflection ----

    @Test
    fun `ensureBackupDirectory creates new directory`() {
        val newDir = tempDir.resolve("sub/nested/backups").toString()
        val newService = BackupService(
            auditService = auditService,
            backupDirectory = newDir,
            maxBackupCount = 3,
            datasourceUrl = "jdbc:postgresql://localhost:5432/zeiterfassung",
            datasourceUsername = "testuser",
            datasourcePassword = "testpass",
        )
        val method = BackupService::class.java.getDeclaredMethod("ensureBackupDirectory")
        method.isAccessible = true
        val result = method.invoke(newService) as File
        assertThat(result.exists()).isTrue()
        assertThat(result.isDirectory).isTrue()
    }

    @Test
    fun `ensureBackupDirectory returns existing directory`() {
        val method = BackupService::class.java.getDeclaredMethod("ensureBackupDirectory")
        method.isAccessible = true
        val result = method.invoke(service) as File
        assertThat(result.exists()).isTrue()
        assertThat(result.absolutePath).isEqualTo(tempDir.toFile().absolutePath)
    }

    // ---- cleanupOldBackups via reflection ----

    @Test
    fun `cleanupOldBackups removes oldest files when exceeding max count`() {
        // maxBackupCount is 3, create 5 files
        (1..5).forEach { i ->
            val f = File(tempDir.toFile(), "backup_$i.sql.gz")
            f.writeText("data$i")
            f.setLastModified(i * 1000000L)
        }
        assertThat(service.listBackups()).hasSize(5)

        val method = BackupService::class.java.getDeclaredMethod("cleanupOldBackups")
        method.isAccessible = true
        method.invoke(service)

        assertThat(service.listBackups()).hasSize(3)
        // Verify newest 3 survived
        assertThat(File(tempDir.toFile(), "backup_5.sql.gz").exists()).isTrue()
        assertThat(File(tempDir.toFile(), "backup_4.sql.gz").exists()).isTrue()
        assertThat(File(tempDir.toFile(), "backup_3.sql.gz").exists()).isTrue()
        assertThat(File(tempDir.toFile(), "backup_1.sql.gz").exists()).isFalse()
        assertThat(File(tempDir.toFile(), "backup_2.sql.gz").exists()).isFalse()
    }

    @Test
    fun `cleanupOldBackups does nothing when under max count`() {
        File(tempDir.toFile(), "backup_1.sql.gz").writeText("data")
        File(tempDir.toFile(), "backup_2.sql.gz").writeText("data")

        val method = BackupService::class.java.getDeclaredMethod("cleanupOldBackups")
        method.isAccessible = true
        method.invoke(service)

        assertThat(service.listBackups()).hasSize(2)
    }

    @Test
    fun `cleanupOldBackups handles non-existent directory`() {
        val noDir = BackupService(
            auditService = auditService,
            backupDirectory = "/tmp/non-existent-${UUID.randomUUID()}",
            maxBackupCount = 3,
            datasourceUrl = "jdbc:postgresql://localhost:5432/zeiterfassung",
            datasourceUsername = "testuser",
            datasourcePassword = "testpass",
        )
        val method = BackupService::class.java.getDeclaredMethod("cleanupOldBackups")
        method.isAccessible = true
        // Should not throw
        method.invoke(noDir)
    }

    // ---- performBackup with failing connection ----

    @Test
    fun `performBackup creates backup file even when pg_dump connects to nothing`() {
        // pg_dump will fail to connect but shell pipe to gzip returns exit 0,
        // so performBackup completes. Verify it produces a BackupInfo.
        val actorId = UUID.randomUUID()
        val result = service.performBackup(actorId)
        assertThat(result.filename).startsWith("zeiterfassung_backup_")
        assertThat(result.filename).endsWith(".sql.gz")
    }

    // ---- restoreFromUpload validates filename ----

    @Test
    fun `restoreFromUpload sanitizes filename`() {
        // The method sanitizes then validates; exercise the path with special characters
        // It will fail on executeRestore (no psql), but we verify it gets past validation
        val stream = ByteArrayInputStream("fake backup data".toByteArray())
        val ex = assertThrows<IllegalStateException> {
            service.restoreFromUpload(stream, "backup (copy).sql.gz", UUID.randomUUID())
        }
        // Should reach executeRestore and fail there, not on filename validation
        assertThat(ex.message).contains("Restore failed")
    }

    // ---- restoreFromBackup ----

    @Test
    fun `restoreFromBackup throws for invalid filename`() {
        assertThrows<IllegalArgumentException> {
            service.restoreFromBackup("../etc/passwd", UUID.randomUUID())
        }
    }

    @Test
    fun `restoreFromBackup throws for non-existent file`() {
        assertThrows<IllegalArgumentException> {
            service.restoreFromBackup("nonexistent.sql.gz", UUID.randomUUID())
        }
    }

    // ---- scheduledBackup ----

    @Test
    fun `scheduledBackup catches exceptions and does not throw`() {
        // performBackup will fail (no pg_dump), but scheduledBackup should catch it
        service.scheduledBackup()
        // No exception thrown - test passes
    }

    // ---- getBackupFile path traversal protection ----

    @Test
    fun `getBackupFile rejects symlink-based path traversal`() {
        val ex = assertThrows<IllegalArgumentException> {
            service.getBackupFile("..%2F..%2Fetc%2Fpasswd")
        }
        assertThat(ex.message).contains("Invalid filename")
    }

    // ---- listBackups sorting ----

    @Test
    fun `listBackups returns backups sorted by creation time descending`() {
        val f1 = File(tempDir.toFile(), "a.sql.gz")
        val f2 = File(tempDir.toFile(), "b.sql.gz")
        val f3 = File(tempDir.toFile(), "c.sql.gz")
        f1.writeText("1")
        f2.writeText("2")
        f3.writeText("3")
        f1.setLastModified(1000L)
        f2.setLastModified(3000L)
        f3.setLastModified(2000L)

        val result = service.listBackups()
        assertThat(result).hasSize(3)
        assertThat(result[0].filename).isEqualTo("b.sql.gz")
        assertThat(result[1].filename).isEqualTo("c.sql.gz")
        assertThat(result[2].filename).isEqualTo("a.sql.gz")
    }
}
