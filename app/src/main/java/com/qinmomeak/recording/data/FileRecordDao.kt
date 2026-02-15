package com.qinmomeak.recording.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FileRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<FileRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: FileRecord)

    @Update
    suspend fun update(record: FileRecord)

    @Query("SELECT * FROM file_records WHERE isHidden = 0")
    suspend fun getVisibleRecords(): List<FileRecord>

    @Query("SELECT * FROM file_records WHERE isHidden = 1")
    suspend fun getHiddenRecords(): List<FileRecord>

    @Query("SELECT * FROM file_records WHERE filePath = :filePath LIMIT 1")
    suspend fun findByPath(filePath: String): FileRecord?

    @Query("SELECT * FROM file_records")
    suspend fun getAllRecords(): List<FileRecord>

    @Query("SELECT * FROM file_records WHERE filePath IN (:paths)")
    suspend fun findByPaths(paths: List<String>): List<FileRecord>

    @Query("UPDATE file_records SET isHidden = 1 WHERE filePath IN (:paths)")
    suspend fun hideByPaths(paths: List<String>)

    @Query("UPDATE file_records SET isHidden = 0 WHERE filePath IN (:paths)")
    suspend fun unhideByPaths(paths: List<String>)

    @Query("UPDATE file_records SET isProcessed = :processed, transcriptText = :transcript, summaryText = :summary WHERE filePath = :filePath")
    suspend fun updateProcessResult(filePath: String, processed: Boolean, transcript: String, summary: String)

    @Query("UPDATE file_records SET transcriptText = :transcript, summaryText = :summary WHERE filePath = :filePath")
    suspend fun updateContent(filePath: String, transcript: String, summary: String)

    @Query("DELETE FROM file_records")
    suspend fun clearAll()
}

