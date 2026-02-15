 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.util.Base64
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AliyunFileTransApiService {
    data class ApiResult(val success: Boolean, val text: String = "", val error: String = "")

    private val client = OkHttpClient()

    suspend fun transcribeByUrl(
        fileUrl: String,
        appKey: String,
        onProgress: (Int, String) -> Unit
    ): ApiResult {
        if (BuildConfig.ALIYUN_ACCESS_KEY_ID.isBlank() || BuildConfig.ALIYUN_ACCESS_KEY_SECRET.isBlank()) {
            return ApiResult(false, error = "AK/SK閺堫亪鍘ょ純?)
        }
        if (appKey.isBlank()) {
            return ApiResult(false, error = "AppKey閺堫亪鍘ょ純?)
        }

        onProgress(28, "閹绘劒姘︾拠鍡楀焼娴犺濮?)
        val submit = submitTask(fileUrl, appKey)
        if (!submit.success) return submit

        var progress = 35
        repeat(120) { // up to ~10 minutes with 5s polling
            delay(5000)
            onProgress(progress.coerceAtMost(95), "閺屻儴顕楃拠鍡楀焼缂佹挻鐏?)
            progress += 2

            val poll = getTaskResult(submit.text)
            if (!poll.success && poll.error.contains("QUEUEING", true).not() &&
                poll.error.contains("RUNNING", true).not()
            ) {
                return poll
            }
            if (poll.success && poll.text.isNotBlank()) {
                onProgress(100, "鐠囧棗鍩嗙€瑰本鍨?)
                return poll
            }
        }
        return ApiResult(false, error = "鐠囧棗鍩嗙搾鍛閿涘矁顕粙宥呮倵闁插秷鐦?)
    }

    private fun submitTask(fileUrl: String, appKey: String): ApiResult {
        val task = JSONObject().apply {
            put("appkey", appKey)
            put("file_link", fileUrl)
            put("version", "4.0")
            put("enable_words", false)
            put("enable_sample_rate_adaptive", true)
            put("auto_split", false)
        }.toString()

        val params = mutableMapOf(
            "Action" to "SubmitTask",
            "Task" to task
        )
        val body = callPopApi("POST", params) ?: return ApiResult(false, error = "SubmitTask鐠囬攱鐪版径杈Е")
        return try {
            val json = JSONObject(body)
            val status = json.optString("StatusText")
            val code = json.optInt("StatusCode")
            val taskId = json.optString("TaskId")
            when {
                taskId.isBlank() -> ApiResult(false, error = "SubmitTask婢惰精瑙? ${json.toString()}")
                status.equals("SUCCESS", true) || code == 21050000 || code == 21050002 || code == 21050001 ->
                    ApiResult(true, text = taskId)
                else -> ApiResult(false, error = "SubmitTask婢惰精瑙? $status($code)")
            }
        } catch (_: Exception) {
            ApiResult(false, error = "SubmitTask鏉╂柨娲栫憴锝嗙€芥径杈Е")
        }
    }

    private fun getTaskResult(taskId: String): ApiResult {
        val params = mutableMapOf(
            "Action" to "GetTaskResult",
            "TaskId" to taskId
        )
        val body = callPopApi("GET", params) ?: return ApiResult(false, error = "GetTaskResult鐠囬攱鐪版径杈Е")
        return try {
            val json = JSONObject(body)
            val status = json.optString("StatusText")
            val code = json.optInt("StatusCode")
            when {
                status.equals("SUCCESS", true) || code == 21050000 -> {
                    val text = extractText(json)
                    if (text.isBlank()) ApiResult(false, error = "鐠囧棗鍩嗙紒鎾寸亯娑撹櫣鈹?)
                    else ApiResult(true, text = text)
                }
                status.equals("RUNNING", true) || status.equals("QUEUEING", true) ||
                    code == 21050001 || code == 21050002 -> {
                    ApiResult(false, error = status.ifBlank { "RUNNING" })
                }
                else -> ApiResult(false, error = "GetTaskResult婢惰精瑙? $status($code)\n${json.toString()}")
            }
        } catch (_: Exception) {
            ApiResult(false, error = "GetTaskResult鏉╂柨娲栫憴锝嗙€芥径杈Е")
        }
    }

    private fun extractText(json: JSONObject): String {
        val result = json.optJSONObject("Result") ?: return ""
        val sentences = result.optJSONArray("Sentences") ?: JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until sentences.length()) {
            val s = sentences.optJSONObject(i) ?: continue
            val t = s.optString("Text").trim()
            if (t.isNotBlank()) list += t
        }
        return list.joinToString("")
    }

    private fun callPopApi(method: String, bizParams: Map<String, String>): String? {
        val common = mutableMapOf(
            "Format" to "JSON",
            "Version" to "2018-08-17",
            "AccessKeyId" to BuildConfig.ALIYUN_ACCESS_KEY_ID,
            "SignatureMethod" to "HMAC-SHA1",
            "Timestamp" to iso8601Now(),
            "SignatureVersion" to "1.0",
            "SignatureNonce" to UUID.randomUUID().toString(),
            "RegionId" to BuildConfig.FILETRANS_REGION_ID,
            "Product" to "nls-filetrans"
        )
        common.putAll(bizParams)

        val signature = sign(method, common, BuildConfig.ALIYUN_ACCESS_KEY_SECRET)
        common["Signature"] = signature

        val form = common.toSortedMap().entries.joinToString("&") {
            "${percentEncode(it.key)}=${percentEncode(it.value)}"
        }
        val endpoint = BuildConfig.FILETRANS_DOMAIN
        val url = "https://$endpoint/"
        val request = if (method.equals("GET", true)) {
            Request.Builder().url("$url?$form").get().build()
        } else {
            Request.Builder()
                .url(url)
                .post(form.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType()))
                .build()
        }
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful && body.isBlank()) null else body
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sign(method: String, params: Map<String, String>, accessKeySecret: String): String {
        val canonicalized = params.toSortedMap().entries.joinToString("&") {
            "${percentEncode(it.key)}=${percentEncode(it.value)}"
        }
        val stringToSign = "${method.uppercase()}&%2F&${percentEncode(canonicalized)}"
        val key = "$accessKeySecret&"
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1"))
        val digest = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun percentEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun iso8601Now(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
}


