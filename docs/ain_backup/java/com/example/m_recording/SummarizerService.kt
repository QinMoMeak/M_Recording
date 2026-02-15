 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

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
        if (endpoint.isBlank() || endpoint.contains("api.example.com")) {
            return localFallbackSummary(sourceText)
        }
        return try {
            val payload = JSONObject().apply {
                put("model", model.ifBlank { "TBStars2-200B-A13B" })
                put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject().apply {
                                put("role", "system")
                                put("content", systemPrompt.ifBlank { "浣犳槸涓€涓笓涓氱殑AI鍔╂墜銆? })
                            }
                        )
                        .put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", userPrompt)
                            }
                        )
                )
                put("temperature", 0.7)
                put("max_tokens", 1000)
            }

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return localFallbackSummary(sourceText, "AI鎺ュ彛涓嶅彲鐢?HTTP ${response.code})锛屽凡浣跨敤鏈湴鎬荤粨銆?)
                }
                val body = response.body?.string().orEmpty()
                val parsed = parseSummary(body)
                if (parsed.isBlank() || parsed == "AI 杩斿洖涓虹┖") {
                    localFallbackSummary(sourceText, "AI杩斿洖涓虹┖锛屽凡浣跨敤鏈湴鎬荤粨銆?)
                } else {
                    parsed
                }
            }
        } catch (e: Exception) {
            localFallbackSummary(sourceText, "AI鎺ュ彛寮傚父锛屽凡浣跨敤鏈湴鎬荤粨銆?)
        }
    }

    private fun parseSummary(body: String): String {
        return try {
            val json = JSONObject(body)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .ifBlank { "AI 杩斿洖涓虹┖" }
        } catch (_: Exception) {
            if (body.isBlank()) "AI 杩斿洖涓虹┖" else body
        }
    }

    private fun localFallbackSummary(text: String, title: String = "鏈湴鎬荤粨"): String {
        val clean = text.replace("\r", "\n").trim()
        if (clean.isBlank()) return "$title\n\n鍘熸枃涓虹┖銆?
        val lines = clean.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        val sentences = clean.split("銆?, "锛?, "锛?, ".", "!", "?")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val keyPoints = (if (sentences.isNotEmpty()) sentences else lines).take(5)
        val preview = clean.take(280)
        val action = when {
            keyPoints.size >= 3 -> "寤鸿鍏堝涔犺鐐?-3锛屽啀鍥炲惉鍘熼煶棰戣ˉ鍏呯粏鑺傘€?
            else -> "寤鸿琛ュ厖鏇村涓婁笅鏂囧悗鍐嶆鐢熸垚鎬荤粨銆?
        }
        val bullets = keyPoints.joinToString("\n") { "- $it" }
        return """
$title

鏍稿績瑕佺偣
$bullets

鍐呭姒傝
$preview

涓嬩竴姝?$action
""".trim()
    }
}


