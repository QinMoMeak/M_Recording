 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
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

class AliyunTokenService {
    private val client = OkHttpClient()
    data class TokenResult(val token: String?, val error: String? = null)

    fun getToken(): TokenResult {
        // Prefer a manually provided token if one is configured.
        if (BuildConfig.ALIYUN_TOKEN.isNotBlank()) return TokenResult(BuildConfig.ALIYUN_TOKEN)
        if (BuildConfig.ALIYUN_ACCESS_KEY_ID.isBlank() || BuildConfig.ALIYUN_ACCESS_KEY_SECRET.isBlank()) {
            return TokenResult(null, "AK/SK閺堫亪鍘ょ純?)
        }

        val params = linkedMapOf(
            "AccessKeyId" to BuildConfig.ALIYUN_ACCESS_KEY_ID,
            "Action" to "CreateToken",
            "Format" to "JSON",
            "RegionId" to "cn-shanghai",
            "SignatureMethod" to "HMAC-SHA1",
            "SignatureNonce" to UUID.randomUUID().toString(),
            "SignatureVersion" to "1.0",
            "Timestamp" to iso8601Now(),
            "Version" to "2019-02-28"
        )

        val signature = sign("POST", params, BuildConfig.ALIYUN_ACCESS_KEY_SECRET)
        val allParams = params.toMutableMap()
        allParams["Signature"] = signature

        val formBody = allParams.toSortedMap().entries.joinToString("&") {
            "${percentEncode(it.key)}=${percentEncode(it.value)}"
        }
        val url = "https://nls-meta.cn-shanghai.aliyuncs.com/"

        val request = Request.Builder()
            .url(url)
            .post(formBody.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty().ifBlank { "HTTP ${response.code}" }
                return TokenResult(null, "CreateToken婢惰精瑙? $errorBody")
            }
            val body = response.body?.string().orEmpty()
            return parseToken(body)
        }
    }

    private fun parseToken(body: String): TokenResult {
        return try {
            val json = JSONObject(body)
            val token = json.optJSONObject("Token")?.optString("Id").orEmpty()
            if (token.isBlank()) {
                val code = json.optString("Code")
                val message = json.optString("Message")
                val hint = listOf(code, message).filter { it.isNotBlank() }.joinToString(" ")
                TokenResult(null, if (hint.isBlank()) "CreateToken鏉╂柨娲栨稉铏光敄" else "CreateToken婢惰精瑙? $hint")
            } else {
                TokenResult(token)
            }
        } catch (_: Exception) {
            TokenResult(null, "CreateToken閸濆秴绨茬憴锝嗙€芥径杈Е")
        }
    }

    private fun sign(method: String, params: Map<String, String>, accessKeySecret: String): String {
        val canonicalized = params.toSortedMap().entries.joinToString("&") {
            "${percentEncode(it.key)}=${percentEncode(it.value)}"
        }
        val stringToSign = "${method.uppercase()}&%2F&${percentEncode(canonicalized)}"
        val key = "${accessKeySecret}&"

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


