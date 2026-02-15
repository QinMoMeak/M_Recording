package com.qinmomeak.recording

import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AliyunFileTransApiService {
    data class ApiResult(val success: Boolean, val text: String = "", val error: String = "")

    private val client = OkHttpClient()

    suspend fun transcribeByUrl(
        fileUrl: String,
        appKey: String,
        onProgress: (Int, String) -> Unit
    ): ApiResult {
        if (fileUrl.isBlank()) return ApiResult(false, error = "file url is empty")
        if (appKey.isBlank() || appKey == "YOUR_APP_KEY") return ApiResult(false, error = "ALIYUN_APP_KEY not configured")

        // Prefer HTTP API if access key is configured, otherwise return clear message.
        if (BuildConfig.ALIYUN_ACCESS_KEY_ID.isBlank() || BuildConfig.ALIYUN_ACCESS_KEY_SECRET.isBlank()) {
            return ApiResult(false, error = "ALIYUN_ACCESS_KEY_ID/ALIYUN_ACCESS_KEY_SECRET not configured")
        }

        onProgress(30, "Submitting task")
        delay(500)

        val task = JSONObject().apply {
            put("appkey", appKey)
            put("file_link", fileUrl)
            put("version", "4.0")
            put("enable_words", false)
            put("enable_sample_rate_adaptive", true)
            put("auto_split", false)
        }

        // Keep a lightweight fallback to avoid hard crash on service changes.
        val req = Request.Builder()
            .url("https://nls-meta.cn-shanghai.aliyuncs.com/")
            .post(task.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            client.newCall(req).execute().use {
                ApiResult(false, error = "Aliyun filetrans API unavailable in current build, please use SiliconFlow ASR")
            }
        } catch (_: Exception) {
            ApiResult(false, error = "Aliyun filetrans request failed")
        }
    }
}
