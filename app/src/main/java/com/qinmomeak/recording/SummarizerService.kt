package com.qinmomeak.recording

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SummarizerService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun summarize(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        sourceText: String
    ): String {
        if (endpoint.isBlank()) return fallback(sourceText)

        return try {
            val payload = JSONObject().apply {
                put("model", model.ifBlank { "qwen3-max" })
                put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt.ifBlank { "你是一个专业的AI助手。" })
                        })
                        .put(JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        })
                )
                put("temperature", 0.3)
                put("max_tokens", 1200)
            }

            val rb = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            if (apiKey.isNotBlank()) rb.header("Authorization", "Bearer $apiKey")

            client.newCall(rb.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return fallback(sourceText)
                val json = JSONObject(body)
                json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.takeIf { it.isNotBlank() }
                    ?: fallback(sourceText)
            }
        } catch (_: Exception) {
            fallback(sourceText)
        }
    }

    private fun fallback(text: String): String {
        val clean = text.trim()
        if (clean.isBlank()) return "暂无可总结内容"
        return "## 总结\n\n### 核心要点\n- ${clean.take(180)}"
    }
}
