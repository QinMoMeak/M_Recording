 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.content.Context
import android.net.Uri
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OssUploaderService(private val context: Context) {
    data class UploadResult(val url: String?, val error: String? = null)

    private val client = OkHttpClient()
    private val allowedFormats = setOf("wav", "mp3", "mp4", "m4a", "wma", "aac", "ogg", "amr", "flac")
    private val maxAudioBytes = 512L * 1024 * 1024

    fun uploadAudio(uri: Uri): UploadResult {
        if (BuildConfig.OSS_ENDPOINT.isBlank() || BuildConfig.OSS_BUCKET.isBlank()) {
            return UploadResult(null, "OSS_ENDPOINT/OSS_BUCKET 鏈厤缃?)
        }
        if (BuildConfig.ALIYUN_ACCESS_KEY_ID.isBlank() || BuildConfig.ALIYUN_ACCESS_KEY_SECRET.isBlank()) {
            return UploadResult(null, "AK/SK 鏈厤缃紝鏃犳硶涓婁紶 OSS")
        }

        val localFile = copyUriToCacheFile(uri) ?: return UploadResult(null, "闊抽鏂囦欢璇诲彇澶辫触")
        try {
            val ext = localFile.extension.lowercase()
            if (ext !in allowedFormats) return UploadResult(null, "鏂囦欢鏍煎紡涓嶆敮鎸? .$ext")
            if (localFile.length() > maxAudioBytes) return UploadResult(null, "闊抽鏂囦欢瓒呰繃 512MB锛屾棤娉曚笂浼?)

            val objectKey = buildObjectKey(ext)
            val host = "${BuildConfig.OSS_BUCKET}.${BuildConfig.OSS_ENDPOINT}"
            val encodedKey = objectKey.split('/').joinToString("/") { urlEncodePathSegment(it) }
            val putUrl = "https://$host/$encodedKey"
            val date = rfc1123Now()
            val contentType = contentTypeFor(ext)
            val canonicalResource = "/${BuildConfig.OSS_BUCKET}/$objectKey"
            val stringToSign = "PUT\n\n$contentType\n$date\n$canonicalResource"
            val signature = hmacSha1Base64(BuildConfig.ALIYUN_ACCESS_KEY_SECRET, stringToSign)
            val authorization = "OSS ${BuildConfig.ALIYUN_ACCESS_KEY_ID}:$signature"

            val request = Request.Builder()
                .url(putUrl)
                .put(localFile.asRequestBody(contentType.toMediaType()))
                .header("Date", date)
                .header("Authorization", authorization)
                .header("Content-Type", contentType)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val detail = response.body?.string().orEmpty()
                    return UploadResult(null, "OSS 涓婁紶澶辫触: HTTP ${response.code} ${detail.ifBlank { "" }}".trim())
                }
            }

            val isPublic = BuildConfig.OSS_PUBLIC_READ.equals("true", ignoreCase = true)
            val finalUrl = if (isPublic) {
                putUrl
            } else {
                signGetUrl(host, objectKey, encodedKey)
            }
            return UploadResult(finalUrl)
        } finally {
            runCatching { localFile.delete() }
        }
    }

    private fun signGetUrl(host: String, objectKey: String, encodedKey: String): String {
        val expiresSec = BuildConfig.OSS_SIGN_EXPIRE_SECONDS.toLongOrNull() ?: 3600L
        val expires = (System.currentTimeMillis() / 1000L) + expiresSec
        val canonicalResource = "/${BuildConfig.OSS_BUCKET}/$objectKey"
        val stringToSign = "GET\n\n\n$expires\n$canonicalResource"
        val signature = hmacSha1Base64(BuildConfig.ALIYUN_ACCESS_KEY_SECRET, stringToSign)
        val signatureEncoded = URLEncoder.encode(signature, "UTF-8")
        val ak = URLEncoder.encode(BuildConfig.ALIYUN_ACCESS_KEY_ID, "UTF-8")
        return "https://$host/$encodedKey?OSSAccessKeyId=$ak&Expires=$expires&Signature=$signatureEncoded"
    }

    private fun buildObjectKey(ext: String): String {
        val prefix = BuildConfig.OSS_OBJECT_PREFIX.trim().ifBlank { "m_recording/" }
        val fixedPrefix = if (prefix.endsWith("/")) prefix else "$prefix/"
        val ts = System.currentTimeMillis()
        return "${fixedPrefix}${ts}_${UUID.randomUUID().toString().replace("-", "")}.$ext"
    }

    private fun contentTypeFor(ext: String): String {
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

    private fun copyUriToCacheFile(uri: Uri): File? {
        return try {
            val ext = guessExtension(uri)
            val outFile = File(context.cacheDir, "oss_upload_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            } ?: return null
            outFile
        } catch (_: Exception) {
            null
        }
    }

    private fun guessExtension(uri: Uri): String {
        val s = uri.toString().lowercase()
        return when {
            s.contains(".mp3") -> "mp3"
            s.contains(".aac") -> "aac"
            s.contains(".m4a") -> "m4a"
            s.contains(".mp4") -> "mp4"
            s.contains(".wma") -> "wma"
            s.contains(".ogg") -> "ogg"
            s.contains(".amr") -> "amr"
            s.contains(".flac") -> "flac"
            else -> "wav"
        }
    }

    private fun rfc1123Now(): String {
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("GMT")
        return formatter.format(Date())
    }

    private fun hmacSha1Base64(secret: String, content: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA1"))
        return Base64.encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun urlEncodePathSegment(segment: String): String {
        return URLEncoder.encode(segment, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
}


