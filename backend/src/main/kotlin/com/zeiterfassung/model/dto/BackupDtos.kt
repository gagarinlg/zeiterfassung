package com.zeiterfassung.model.dto

import java.time.Instant

data class BackupInfo(
    val filename: String,
    val sizeBytes: Long,
    val createdAt: Instant,
)

data class RestoreResponse(
    val status: String,
    val message: String,
)
