 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.alibaba.idst.nui.AsrResult
import com.alibaba.idst.nui.Constants
import com.alibaba.idst.nui.INativeFileTransCallback
import com.alibaba.idst.nui.NativeNui
import org.json.JSONObject

class AliyunAsrManager(
    private val context: Context,
    private val listener: Listener
) : INativeFileTransCallback {
    interface Listener {
        fun onAsrProgress(progress: Int)
        fun onAsrCompleted(text: String)
        fun onAsrError(message: String)
    }

    private val nui = NativeNui()
    private var initialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val allowedFormats = setOf("wav", "mp3", "mp4", "m4a", "wma", "aac", "ogg", "amr", "flac")

    private fun postProgress(progress: Int) {
        mainHandler.post { listener.onAsrProgress(progress) }
    }

    private fun postCompleted(text: String) {
        mainHandler.post { listener.onAsrCompleted(text) }
    }

    private fun postError(message: String) {
        mainHandler.post { listener.onAsrError(message) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun startFileTranscription(audioUri: Uri, appKey: String, token: String) {
        postError("褰撳墠鎸夎鑼冧粎鏀寔URL璇嗗埆锛岃鍏堜笂浼犳枃浠跺埌鍙疕TTP璁块棶鍦板潃鍚庡啀璇嗗埆")
    }

    fun startFileTranscriptionByUrl(audioUrl: String, appKey: String, token: String) {
        if (appKey.isBlank() || token.isBlank() || appKey == "YOUR_APP_KEY" || token == "YOUR_TOKEN") {
            postError("璇峰厛閰嶇疆闃块噷浜?AppKey/Token")
            return
        }
        val trimUrl = audioUrl.trim()
        if (!isValidPublicMediaUrl(trimUrl)) {
            postError("闊抽URL涓嶅悎娉曪細闇€http/https銆佷笉鑳芥槸IP銆佷笉鑳藉惈绌烘牸")
            return
        }
        val ext = trimUrl.substringAfterLast('.', "").substringBefore('?').lowercase()
        if (ext !in allowedFormats) {
            postError("URL鏂囦欢鏍煎紡涓嶆敮鎸? .$ext")
            return
        }

        val initRet = ensureInit(appKey, token)
        if (initRet != 0) {
            postError("闃块噷浜?SDK 鍒濆鍖栧け璐? $initRet")
            return
        }

        val dialogParams = buildDialogParamsFromUrl(trimUrl, ext)
        val taskIdBuffer = ByteArray(32)
        val ret = nui.startFileTranscriber(dialogParams, taskIdBuffer)
        if (ret != 0) {
            postError("鍚姩URL鏂囦欢杞啓澶辫触: $ret")
            return
        }
        postProgress(5)
    }

    fun release() {
        nui.release()
        initialized = false
    }

    private fun ensureInit(appKey: String, token: String): Int {
        if (initialized) return 0
        val initParams = buildInitParams(appKey, token)
        val ret = nui.initialize(
            this,
            initParams,
            Constants.LogLevel.LOG_LEVEL_INFO,
            false
        )
        if (ret == 0) {
            initialized = true
            nui.setParams("""{"nls_config":{}}""")
        }
        return ret
    }

    private fun buildInitParams(appKey: String, token: String): String {
        return JSONObject().apply {
            put("app_key", appKey)
            put("token", token)
            put("url", "https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/FlashRecognizer")
            put("service_mode", Constants.ModeFullCloud)
            put("device_id", "m_recording_android")
            put("debug_path", context.externalCacheDir?.absolutePath ?: context.cacheDir.absolutePath)
            put("log_track_level", Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_NONE).toString())
        }.toString()
    }

    private fun buildDialogParamsFromUrl(url: String, format: String): String {
        return JSONObject().apply {
            put("audio_address", url)
            put("version", "4.0")
            put("enable_words", false)
            put("enable_sample_rate_adaptive", true)
            put("auto_split", false)
            put("nls_config", JSONObject().apply {
                put("format", format)
            })
        }.toString()
    }

    private fun isValidPublicMediaUrl(url: String): Boolean {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return false
        if (url.contains(" ")) return false
        val host = try {
            android.net.Uri.parse(url).host.orEmpty()
        } catch (_: Exception) {
            ""
        }
        if (host.isBlank()) return false
        val ipv4 = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (host.matches(ipv4)) return false
        return true
    }

    override fun onFileTransEventCallback(
        event: Constants.NuiEvent,
        resultCode: Int,
        finish: Int,
        asrResult: AsrResult?,
        taskId: String?
    ) {
        when (event) {
            Constants.NuiEvent.EVENT_FILE_TRANS_UPLOAD_PROGRESS -> {
                val p = finish.coerceIn(0, 95)
                postProgress(p)
            }

            Constants.NuiEvent.EVENT_FILE_TRANS_UPLOADED -> {
                postProgress(70)
            }

            Constants.NuiEvent.EVENT_FILE_TRANS_RESULT -> {
                postProgress(100)
                val text = asrResult?.asrResult.orEmpty()
                if (text.isBlank()) {
                    postError("杞啓缁撴灉涓虹┖")
                } else {
                    postCompleted(text)
                }
            }

            Constants.NuiEvent.EVENT_ASR_ERROR -> {
                val detail = asrResult?.allResponse.orEmpty()
                val isTrialExpired = resultCode == 40000010 ||
                    detail.contains("FREE_TRIAL_EXPIRED", ignoreCase = true) ||
                    detail.contains("40000010")
                val hint = if (resultCode == 240075) {
                    "锛堟湇鍔＄閿欒锛屽缓璁鏌ppKey/Token鍖归厤銆侀煶棰戞牸寮忓弬鏁癴ormat銆佹枃浠跺彲璇绘€э級"
                } else {
                    ""
                }
                val msg = if (isTrialExpired) {
                    "璇煶璇嗗埆涓嶅彲鐢細闃块噷浜戣瘯鐢ㄥ凡鍒版湡锛?0000010 FREE_TRIAL_EXPIRED锛夈€傝鍦ㄩ樋閲屼簯鎺у埗鍙板紑閫氬晢鐢ㄦ垨缁垂鍚庨噸璇曘€?
                } else if (detail.isBlank()) {
                    "璇煶璇嗗埆閿欒: $resultCode$hint"
                } else {
                    "璇煶璇嗗埆閿欒: $resultCode$hint\n$detail"
                }
                postError(msg)
            }

            else -> Unit
        }
    }

    override fun onFileTransLogTrackCallback(level: Constants.LogLevel, log: String) {
    }
}


