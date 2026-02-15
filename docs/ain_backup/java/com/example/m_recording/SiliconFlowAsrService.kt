 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SiliconFlowAsrService {
    data class ApiResult(val success: Boolean, val text: String = "", val error: String = "")

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun transcribeFile(file: File, model: String, apiKey: String): ApiResult {
        if (apiKey.isBlank()) return ApiResult(false, error = "SiliconFlow API Key 鏈厤缃?)
        if (!file.exists()) return ApiResult(false, error = "闊抽鏂囦欢涓嶅瓨鍦?)

        val contentType = guessContentType(file.extension.lowercase())
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
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = if (responseBody.isBlank()) {
                        "HTTP ${response.code}"
                    } else {
                        responseBody
                    }
                    ApiResult(false, error = "SiliconFlow 璇锋眰澶辫触: $message")
                } else {
                    val text = JSONObject(responseBody).optString("text").trim()
                    if (text.isBlank()) ApiResult(false, error = "SiliconFlow 杩斿洖涓虹┖")
                    else ApiResult(true, text = text)
                }
            }
        } catch (e: Exception) {
            ApiResult(false, error = "SiliconFlow 璇锋眰寮傚父")
        } finally {
            runCatching { file.delete() }
        }
    }

    private fun guessContentType(ext: String): String {
        return when (ext) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "m4a" -> "audio/mp4"
            "wma" -> "audio/x-ms-wma"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "amr" -> "audio/amr"
            "flac" -> "audio/flac"
            else -> "application/octet-stream"
        }
    }
}


