package com.qinmomeak.recording

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class AudioTranscoder(private val context: Context) {
    fun transcodeToWav(uri: Uri): File? {
        return try {
            val out = File(context.cacheDir, "transcoded_${System.currentTimeMillis()}.wav")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            out
        } catch (_: Exception) {
            null
        }
    }
}
