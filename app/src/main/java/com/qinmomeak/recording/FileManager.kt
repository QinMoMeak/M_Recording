package com.qinmomeak.recording

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.qinmomeak.recording.data.AppDatabase
import com.qinmomeak.recording.data.FileRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager(private val context: Context) {
    private val dao = AppDatabase.get(context).fileRecordDao()
    private val folderStore = FolderFilterStore(context)

    suspend fun syncMediaStore() = withContext(Dispatchers.IO) {
        val selectedPrefixes = folderStore.getPrefixes().toList()
        if (selectedPrefixes.isEmpty()) {
            dao.clearAll()
            return@withContext
        }

        val records = mutableListOf<FileRecord>()
        records += queryMedia(
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaType = "audio",
            selectedPrefixes = selectedPrefixes
        )
        records += queryMedia(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = "video",
            selectedPrefixes = selectedPrefixes
        )

        if (records.isEmpty()) {
            dao.clearAll()
            return@withContext
        }

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
        dao.clearAll()
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
        val processed = transcript.isNotBlank() || summary.isNotBlank()
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

    suspend fun updateResultContent(filePath: String, transcript: String, summary: String) = withContext(Dispatchers.IO) {
        dao.updateContent(filePath, transcript, summary)
    }

    suspend fun exportAllRecordsCsv(): File = withContext(Dispatchers.IO) {
        val all = dao.getAllRecords()
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "file_records_$time.csv")
        val header = "filePath,fileName,mediaType,durationMs,sizeBytes,addedTimeSec,isProcessed,isHidden,transcriptText,summaryText\n"
        val body = buildString {
            all.forEach { row ->
                append(csv(row.filePath)).append(',')
                append(csv(row.fileName)).append(',')
                append(csv(row.mediaType)).append(',')
                append(row.durationMs).append(',')
                append(row.sizeBytes).append(',')
                append(row.addedTimeSec).append(',')
                append(row.isProcessed).append(',')
                append(row.isHidden).append(',')
                append(csv(row.transcriptText)).append(',')
                append(csv(row.summaryText)).append('\n')
            }
        }
        out.writeText(header + body, Charsets.UTF_8)
        out
    }

    private fun sortRecords(records: List<FileRecord>, sortBy: SortBy, ascending: Boolean): List<FileRecord> {
        val sorted = when (sortBy) {
            SortBy.TIME -> records.sortedBy { it.addedTimeSec }
            SortBy.NAME -> records.sortedBy { it.fileName.lowercase() }
            SortBy.SIZE -> records.sortedBy { it.sizeBytes }
        }
        return if (ascending) sorted else sorted.reversed()
    }

    private fun queryMedia(collection: Uri, mediaType: String, selectedPrefixes: List<String>): List<FileRecord> {
        val resolver = context.contentResolver
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA
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
            val relativePathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val dataPathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val relativePath = if (relativePathIndex >= 0) cursor.getString(relativePathIndex).orEmpty() else ""
                val dataPath = if (dataPathIndex >= 0) cursor.getString(dataPathIndex).orEmpty() else ""
                if (!isInSelectedFolder(relativePath, dataPath, selectedPrefixes)) continue

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

    private fun isInSelectedFolder(relativePath: String, dataPath: String, selectedPrefixes: List<String>): Boolean {
        if (selectedPrefixes.isEmpty()) return false
        val rel = normalizePath(relativePath)
        if (rel.isNotBlank() && selectedPrefixes.any { rel.startsWith(it, ignoreCase = true) }) {
            return true
        }
        val data = normalizePath(dataPath)
        if (data.isBlank()) return false
        return selectedPrefixes.any { prefix ->
            data.contains("/$prefix", ignoreCase = true) || data.startsWith(prefix, ignoreCase = true)
        }
    }

    private fun normalizePath(value: String): String {
        return value.replace('\\', '/').trim().trim('/')
            .let { if (it.isBlank()) "" else "$it/" }
    }

    private fun buildRecordFromUri(uri: Uri): FileRecord? {
        val resolver = context.contentResolver
        val columns = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.MIME_TYPE
        )
        resolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

            val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "unknown"
            val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            val dateAdded = if (dateIndex >= 0) cursor.getLong(dateIndex) else (System.currentTimeMillis() / 1000)
            val duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
            val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex).orEmpty() else ""
            return FileRecord(
                filePath = uri.toString(),
                fileName = name.ifBlank { "unknown" },
                mediaType = if (mime.startsWith("video")) "video" else "audio",
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
            mediaType = if (filePath.contains("video", true)) "video" else "audio",
            durationMs = 0L,
            sizeBytes = size,
            addedTimeSec = added
        )
    }

    private fun csv(v: String): String {
        return "\"" + v.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
    }
}

