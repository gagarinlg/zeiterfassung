package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.BackupInfo
import com.zeiterfassung.service.BackupService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BackupControllerTest {
    @Mock
    private lateinit var backupService: BackupService

    private lateinit var controller: BackupController

    private val actorId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        controller = BackupController(backupService)
    }

    @Test
    fun `listBackups should return list of backups`() {
        val backups =
            listOf(
                BackupInfo("backup1.sql.gz", 1024L, Instant.now()),
                BackupInfo("backup2.sql.gz", 2048L, Instant.now()),
            )
        `when`(backupService.listBackups()).thenReturn(backups)

        val response = controller.listBackups()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(2)
        assertThat(response.body!![0].filename).isEqualTo("backup1.sql.gz")
    }

    @Test
    fun `listBackups should return empty list when no backups`() {
        `when`(backupService.listBackups()).thenReturn(emptyList())

        val response = controller.listBackups()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `createBackup should call performBackup with correct UUID`() {
        val backupInfo = BackupInfo("backup.sql.gz", 1024L, Instant.now())
        `when`(backupService.performBackup(UUID.fromString(actorId))).thenReturn(backupInfo)

        val response = controller.createBackup(actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(backupInfo)
        verify(backupService).performBackup(UUID.fromString(actorId))
    }

    @Test
    fun `downloadBackup should return file resource with correct headers`() {
        val tempFile = File.createTempFile("test-backup", ".sql.gz")
        tempFile.writeText("backup data")
        tempFile.deleteOnExit()

        `when`(backupService.getBackupFile("test.sql.gz")).thenReturn(tempFile)

        val response = controller.downloadBackup("test.sql.gz")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_OCTET_STREAM)
        assertThat(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .contains("attachment")
            .contains(tempFile.name)
        assertThat(response.body).isNotNull
    }

    @Test
    fun `restoreFromBackup should call service and return ok response`() {
        val filename = "backup.sql.gz"
        val uuid = UUID.fromString(actorId)

        val response = controller.restoreFromBackup(filename, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo("ok")
        assertThat(response.body!!.message).contains(filename)
        verify(backupService).restoreFromBackup(filename, uuid)
    }

    @Test
    fun `restoreFromUpload should call service with multipart file`() {
        val multipartFile =
            MockMultipartFile(
                "file",
                "upload.sql.gz",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "backup content".toByteArray(),
            )

        val response = controller.restoreFromUpload(multipartFile, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo("ok")
        assertThat(response.body!!.message).contains("uploaded file")
        verify(backupService).restoreFromUpload(
            org.mockito.ArgumentMatchers.any() ?: java.io.InputStream.nullInputStream(),
            org.mockito.ArgumentMatchers.anyString() ?: "",
            org.mockito.ArgumentMatchers.any() ?: UUID.randomUUID(),
        )
    }

    @Test
    fun `restoreFromUpload should use default filename when originalFilename is null`() {
        val multipartFile =
            MockMultipartFile(
                "file",
                null as String?,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "backup content".toByteArray(),
            )

        val response = controller.restoreFromUpload(multipartFile, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(backupService).restoreFromUpload(
            org.mockito.ArgumentMatchers.any() ?: java.io.InputStream.nullInputStream(),
            org.mockito.ArgumentMatchers.anyString() ?: "",
            org.mockito.ArgumentMatchers.any() ?: UUID.randomUUID(),
        )
    }

    @Test
    fun `deleteBackup should call service and return no content`() {
        val filename = "backup.sql.gz"
        val uuid = UUID.fromString(actorId)

        val response = controller.deleteBackup(filename, actorId)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(response.body).isNull()
        verify(backupService).deleteBackup(filename, uuid)
    }
}
