package com.qinmomeak.recording

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SiliconFlowAsrService {
    data class ApiResult(val success: Boolean, val text: String = "", val error: String = "")
    @Volatile
    private var activeCall: Call? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun transcribeFile(file: File, model: String, apiKey: String): ApiResult {
        if (apiKey.isBlank()) return ApiResult(false, error = "SILICONFLOW_API_KEY not configured")
        if (!file.exists()) return ApiResult(false, error = "Audio file not found")

        val contentType = when (file.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            else -> "application/octet-stream"
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(contentType.toMediaType()))
            .addFormDataPart("model", model)
            .build()

        val request = Request.Builder()
            .url("https://api.siliconflow.cn/v1/audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        return try {
            val call = client.newCall(request)
            activeCall = call
            call.execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    ApiResult(false, error = "SiliconFlow HTTP ${response.code}: ${responseBody.ifBlank { "empty" }}")
                } else {
                    val text = JSONObject(responseBody).optString("text").trim()
                    if (text.isBlank()) ApiResult(false, error = "SiliconFlow returned empty text")
                    else ApiResult(true, text = text)
                }
            }
        } catch (e: Exception) {
            ApiResult(false, error = "SiliconFlow request failed")
        } finally {
            activeCall = null
            runCatching { file.delete() }
        }
    }

    fun cancelCurrentRequest() {
        runCatching { activeCall?.cancel() }
    }
}
