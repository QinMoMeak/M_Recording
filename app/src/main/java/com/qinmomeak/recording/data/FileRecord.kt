package com.qinmomeak.recording.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_records")
data class FileRecord(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val mediaType: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedTimeSec: Long,
    val isProcessed: Boolean = false,
    val isHidden: Boolean = false,
    val transcriptText: String = "",
    val summaryText: String = ""
)

