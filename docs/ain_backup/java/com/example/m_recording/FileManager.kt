 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.qinmomeak.recording.data.AppDatabase
import com.qinmomeak.recording.data.FileRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManager(private val context: Context) {
    private val dao = AppDatabase.get(context).fileRecordDao()

    suspend fun syncMediaStore() = withContext(Dispatchers.IO) {
        val records = mutableListOf<FileRecord>()
        records += queryMedia(
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaType = "audio"
        )
        records += queryMedia(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = "video"
        )

        if (records.isEmpty()) return@withContext

        val existing = dao.findByPaths(records.map { it.filePath }).associateBy { it.filePath }
        val merged = records.map { fresh ->
            val old = existing[fresh.filePath]
            if (old == null) fresh else {
                fresh.copy(
                    isProcessed = old.isProcessed,
                    isHidden = old.isHidden,
                    transcriptText = old.transcriptText,
                    summaryText = old.summaryText
                )
            }
        }
        dao.upsertAll(merged)
    }

    suspend fun getRecords(scope: MediaScope, sortBy: SortBy, ascending: Boolean): List<FileRecord> = withContext(Dispatchers.IO) {
        val raw = if (scope == MediaScope.VISIBLE) dao.getVisibleRecords() else dao.getHiddenRecords()
        sortRecords(raw, sortBy, ascending)
    }

    suspend fun hide(paths: List<String>) = withContext(Dispatchers.IO) {
        dao.hideByPaths(paths)
    }

    suspend fun unhide(paths: List<String>) = withContext(Dispatchers.IO) {
        dao.unhideByPaths(paths)
    }

    suspend fun find(filePath: String): FileRecord? = withContext(Dispatchers.IO) {
        dao.findByPath(filePath)
    }

    suspend fun saveResult(filePath: String, transcript: String, summary: String) = withContext(Dispatchers.IO) {
        val processed = transcript.isNotBlank()
        val exists = dao.findByPath(filePath)
        if (exists != null) {
            dao.updateProcessResult(
                filePath = filePath,
                processed = processed,
                transcript = transcript,
                summary = summary
            )
            return@withContext
        }
        val uri = Uri.parse(filePath)
        val record = buildRecordFromUri(uri) ?: fallbackRecord(filePath)
        val finalRecord = record.copy(
            isProcessed = processed,
            transcriptText = transcript,
            summaryText = summary
        )
        dao.upsert(finalRecord)
    }

    private fun sortRecords(records: List<FileRecord>, sortBy: SortBy, ascending: Boolean): List<FileRecord> {
        val sorted = when (sortBy) {
            SortBy.TIME -> records.sortedBy { it.addedTimeSec }
            SortBy.NAME -> records.sortedBy { it.fileName.lowercase() }
            SortBy.SIZE -> records.sortedBy { it.sizeBytes }
        }
        return if (ascending) sorted else sorted.reversed()
    }

    private fun queryMedia(collection: Uri, mediaType: String): List<FileRecord> {
        val resolver = context.contentResolver
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION
        )
        val result = mutableListOf<FileRecord>()
        resolver.query(
            collection,
            columns,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(collection, id)
                val name = cursor.getString(nameIndex).orEmpty().ifBlank { "unknown" }
                val size = cursor.getLong(sizeIndex)
                val dateAdded = cursor.getLong(dateIndex)
                val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
                result += FileRecord(
                    filePath = uri.toString(),
                    fileName = name,
                    mediaType = mediaType,
                    durationMs = duration,
                    sizeBytes = size,
                    addedTimeSec = dateAdded
                )
            }
        }
        return result
    }

    private fun buildRecordFromUri(uri: Uri): FileRecord? {
        val resolver = context.contentResolver
        val columns = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION
        )
        resolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

            val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "unknown"
            val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            val dateAdded = if (dateIndex >= 0) cursor.getLong(dateIndex) else (System.currentTimeMillis() / 1000)
            val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
            return FileRecord(
                filePath = uri.toString(),
                fileName = name.ifBlank { "unknown" },
                mediaType = guessMediaType(uri),
                durationMs = duration,
                sizeBytes = size,
                addedTimeSec = dateAdded
            )
        }
        return null
    }

    private fun fallbackRecord(filePath: String): FileRecord {
        val uri = Uri.parse(filePath)
        val name = uri.lastPathSegment?.ifBlank { "unknown" } ?: "unknown"
        val file = if (uri.scheme == "file") File(uri.path.orEmpty()) else null
        val size = file?.takeIf { it.exists() }?.length() ?: 0L
        val added = if (file?.exists() == true) file.lastModified() / 1000 else (System.currentTimeMillis() / 1000)
        return FileRecord(
            filePath = filePath,
            fileName = name,
            mediaType = guessMediaType(uri),
            durationMs = 0L,
            sizeBytes = size,
            addedTimeSec = added
        )
    }

    private fun guessMediaType(uri: Uri): String {
        val raw = uri.toString().lowercase()
        return if (raw.contains("video")) "video" else "audio"
    }
}


