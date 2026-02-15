package com.qinmomeak.recording

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import com.qinmomeak.recording.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), AliyunAsrManager.Listener {

    private lateinit var binding: ActivityMainBinding

    private val tabs = listOf("转录原文", "AI总结")
    private var transcriptText = ""
    private var summaryText = ""

    private var selectedAudioUri: Uri? = null
    private var selectedMediaPath: String? = null
    private var selectedMediaType: String? = null
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)
    private var currentTask: Job? = null
    private var transcodedFile: File? = null
    private val stopRequested = AtomicBoolean(false)
    private var runtimeJob: Job? = null
    private var runtimeSeconds: Int = 0

    private val audioExtractor by lazy { AudioExtractor(this) }
    private val ossUploader by lazy { OssUploaderService(this) }
    private val asrManager by lazy { AliyunAsrManager(this, listener = this) }
    private val tokenService by lazy { AliyunTokenService() }
    private val fileTransApi by lazy { AliyunFileTransApiService() }
    private val siliconFlowAsr by lazy { SiliconFlowAsrService() }
    private val summarizerService by lazy { SummarizerService() }
    private val nativeAudioProcessor by lazy { NativeAudioProcessor(this) }

    private val scenarioTemplates = mapOf(
        "课堂录音" to "你是课堂学习总结助手。请输出：标题、核心要点、关键结论、行动项、风险与注意事项，结构清晰。",
        "老师点评" to "你是教学反馈分析助手。请提取：优点、问题、改进建议、可执行行动项，语言专业简洁。",
        "自身回答" to "你是表达训练助手。请分析：论点清晰度、逻辑性、表达问题，并给出优化版回答。",
        "通用模式" to "你是通用内容总结助手。请基于原文做结构化总结，避免编造。"
    )
    private val asrOptions = listOf(
        "阿里云录音文件识别",
        "SiliconFlow - SenseVoiceSmall",
        "SiliconFlow - TeleSpeechASR"
    )
    private val formatOptions = listOf("md", "html_doc", "json", "text")

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedMediaType = "video"
            startNewTask(clearInputText = false)
            launchTask { processVideo(uri) }
        }
    }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedAudioUri = uri
        if (uri != null) {
            selectedMediaType = "audio"
            startNewTask(clearInputText = false)
            binding.statusText.text = "已选择音频: ${uri.lastPathSegment ?: "未知"}"
        }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // 鐢ㄦ埛瑙﹀彂鏃跺啀鎵ц鍏蜂綋鍔ㄤ綔
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputLayer()
        setupTabs()
        setupHelpAction()
        setupStopAction()
        setupLibraryAction()
        setTaskRunning(false)
        updateRuntimeText(0)
        ensureBasePermissions()

        val mediaPath = intent.getStringExtra(MediaLibraryActivity.EXTRA_MEDIA_URI)
        val mediaType = intent.getStringExtra(MediaLibraryActivity.EXTRA_MEDIA_TYPE)
        if (!mediaPath.isNullOrBlank()) {
            selectedMediaPath = mediaPath
            selectedAudioUri = Uri.parse(mediaPath)
            selectedMediaType = mediaType
            binding.statusText.text = "已选择媒体库文件"
        }
    }

    private fun setupInputLayer() {
        val scenarios = scenarioTemplates.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scenarios)
        binding.scenarioInput.setAdapter(adapter)
        binding.scenarioInput.setText(scenarios.first(), false)

        val asrAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, asrOptions)
        binding.asrInput.setAdapter(asrAdapter)
        binding.asrInput.setText("SiliconFlow - TeleSpeechASR", false)

        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, formatOptions)
        binding.formatInput.setAdapter(formatAdapter)
        binding.formatInput.setText("md", false)

        binding.cardVideo.setOnClickListener {
            if (hasMediaReadPermission()) {
                pickVideo.launch("video/*")
            } else {
                ensureBasePermissions()
            }
        }

        binding.cardAudio.setOnClickListener {
            if (hasMediaReadPermission()) {
                pickAudio.launch("audio/*")
            } else {
                ensureBasePermissions()
            }
        }

        binding.btnStart.setOnClickListener {
            val inputText = binding.textInput.text?.toString().orEmpty().trim()
            when {
                looksLikeMediaUrl(inputText) -> {
                    startNewTask(clearInputText = false)
                    if (currentAsrProvider() == AsrProvider.ALIYUN) {
                        launchTask { processAudioUrl(inputText) }
                    } else {
                        setProgress(0, "SiliconFlow 仅支持本地文件上传")
                    }
                }

                inputText.isNotEmpty() -> {
                    startNewTask(clearInputText = false)
                    transcriptText = inputText
                    publishTranscript()
                    launchTask { summarize(transcriptText) }
                }

                selectedAudioUri != null -> {
                    startNewTask(clearInputText = false)
                    if (selectedMediaType == "video") {
                        launchTask { processVideo(selectedAudioUri!!) }
                    } else {
                        launchTask { processAudio(selectedAudioUri!!) }
                    }
                }

                else -> {
                    Toast.makeText(this, "请先选择视频/音频或粘贴文本", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun processVideo(videoUri: Uri) {
        ensureNotStopped()
        clearTaskCacheFiles()
        setProgress(5, "正在提取音频")
        val output = withContext(Dispatchers.IO) {
            audioExtractor.extractAudioFromVideo(videoUri)
        }
        if (output == null) {
            setProgress(0, "音频提取失败")
            return
        }
        selectedAudioUri = Uri.fromFile(output)
        selectedMediaType = "audio"
        setProgress(20, "音频提取完成，自动上传识别")
        processAudio(selectedAudioUri!!)
    }

    private suspend fun processAudio(audioUri: Uri) {
        ensureNotStopped()
        when (currentAsrProvider()) {
            AsrProvider.ALIYUN -> {
                val prepared = prepareAudioForUpload(audioUri)
                if (!prepared.success) {
                    setProgress(0, prepared.error.ifBlank { "音频准备失败" })
                    return
                }
                val preparedUri = prepared.uri ?: return
                setProgress(8, "上传音频到 OSS")
                val uploadResult = withContext(Dispatchers.IO) {
                    ossUploader.uploadAudio(preparedUri)
                }
                val audioUrl = uploadResult.url
                if (audioUrl.isNullOrBlank()) {
                    setProgress(0, "OSS 上传失败: ${uploadResult.error ?: "未知错误"}")
                    return
                }
                binding.textInput.setText(audioUrl)
                processAudioUrl(audioUrl)
            }
            AsrProvider.SILICON_SENSEVOICE -> {
                processAudioBySiliconFlow(audioUri, "FunAudioLLM/SenseVoiceSmall")
            }
            AsrProvider.SILICON_TELESPEECH -> {
                processAudioBySiliconFlow(audioUri, "TeleAI/TeleSpeechASR")
            }
        }
    }

    private suspend fun processAudioUrl(audioUrl: String) {
        ensureNotStopped()
        val apiResult = withContext(Dispatchers.IO) {
            fileTransApi.transcribeByUrl(
                fileUrl = audioUrl,
                appKey = BuildConfig.ALIYUN_APP_KEY
            ) { progress, status ->
                runOnUiThread { setProgress(progress, status) }
            }
        }
        if (!apiResult.success) {
            setProgress(0, apiResult.error.ifBlank { "录音文件识别失败" })
            finishTaskCleanup()
            return
        }
        transcriptText = apiResult.text
        publishTranscript()
        summarize(transcriptText)
    }

    private fun looksLikeMediaUrl(text: String): Boolean {
        if (text.isBlank()) return false
        if (!(text.startsWith("http://") || text.startsWith("https://"))) return false
        val exts = listOf(".wav", ".mp3", ".mp4", ".m4a", ".wma", ".aac", ".ogg", ".amr", ".flac")
        return exts.any { text.contains(it, ignoreCase = true) }
    }

    private suspend fun summarize(transcript: String) {
        ensureNotStopped()
        val scenario = binding.scenarioInput.text?.toString().orEmpty()
        val systemPrompt = scenarioTemplates[scenario] ?: scenarioTemplates.getValue("通用模式")
        val format = binding.formatInput.text?.toString().orEmpty().ifBlank { "md" }
        val userPrompt = """
你是“结构化文档总结助手”。
输入参数：
- format: 只能是 "md" / "html_doc" / "json" / "text"
- content: 需要总结的原始内容

通用要求：
1. 输出语言与 content 保持一致。
2. 结构固定为：标题、核心要点、关键结论、行动项（如有）、风险或注意事项（如有）。
3. 不输出解释性前言，不编造信息。
4. 只输出一种格式内容。

format: "$format"
content:
$transcript
""".trimIndent()

        setProgress(80, "AI 总结中")
        val result = withContext(Dispatchers.IO) {
            summarizerService.summarize(
                endpoint = BuildConfig.AI_ENDPOINT,
                apiKey = BuildConfig.AI_API_KEY,
                model = BuildConfig.AI_MODEL,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                sourceText = transcript
            )
        }
        summaryText = result
        ensureNotStopped()
        publishSummary()
        persistResult()
        setProgress(100, "处理完成")
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        tabs.forEach { label ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(label))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showTabDialog(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) {
                showTabDialog(tab.position)
            }
        })
    }

    private fun setupHelpAction() {
        binding.helpButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.help_title))
                .setMessage(getString(R.string.help_body))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun setupLibraryAction() {
        binding.libraryButton.setOnClickListener {
            MediaLibraryActivity.open(this)
        }
    }

    private fun showTextDialog(title: String, content: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_text, null, false)
        val editText = dialogView.findViewById<EditText>(R.id.dialogEditText)
        val previewText = dialogView.findViewById<android.widget.TextView>(R.id.dialogPreviewText)
        val copyButton = dialogView.findViewById<android.view.View>(R.id.dialogCopy)
        val toggleSource = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogToggleSource)
        val togglePreview = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogTogglePreview)

        val resolved = if (content.isBlank()) getString(R.string.dialog_empty) else content
        editText.setText(resolved)
        editText.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        editText.isSingleLine = false
        editText.setTextIsSelectable(true)
        previewText.text = renderMarkdownPreview(resolved)

        fun showSource() {
            editText.visibility = android.view.View.VISIBLE
            previewText.visibility = android.view.View.GONE
            toggleSource.isSelected = true
            togglePreview.isSelected = false
            toggleSource.setBackgroundColor(ContextCompat.getColor(this, R.color.ios_toggle_on))
            togglePreview.setBackgroundColor(ContextCompat.getColor(this, R.color.ios_surface))
            toggleSource.setTextColor(ContextCompat.getColor(this, R.color.ios_accent))
            togglePreview.setTextColor(ContextCompat.getColor(this, R.color.ios_text_secondary))
        }

        fun showPreview() {
            editText.visibility = android.view.View.GONE
            previewText.visibility = android.view.View.VISIBLE
            toggleSource.isSelected = false
            togglePreview.isSelected = true
            toggleSource.setBackgroundColor(ContextCompat.getColor(this, R.color.ios_surface))
            togglePreview.setBackgroundColor(ContextCompat.getColor(this, R.color.ios_toggle_on))
            toggleSource.setTextColor(ContextCompat.getColor(this, R.color.ios_text_secondary))
            togglePreview.setTextColor(ContextCompat.getColor(this, R.color.ios_accent))
        }

        toggleSource.setOnClickListener { showSource() }
        togglePreview.setOnClickListener { showPreview() }
        showSource()

        copyButton.setOnClickListener {
            val text = editText.text?.toString().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(this, "暂无可复制内容", Toast.LENGTH_SHORT).show()
            } else {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .show()
    }

    private fun ensureBasePermissions() {
        val permissions = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += android.Manifest.permission.READ_MEDIA_AUDIO
            permissions += android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            permissions += android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissions.launch(permissions.toTypedArray())
    }

    private fun hasMediaReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(android.Manifest.permission.READ_MEDIA_AUDIO) &&
                hasPermission(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            hasPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun setProgress(progress: Int, status: String) {
        binding.progress.setProgressCompat(progress, true)
        binding.progressPercent.text = "${progress.coerceIn(0, 100)}%"
        binding.statusText.text = status
        updateDebugStatus()
    }

    private fun startNewTask(clearInputText: Boolean) {
        transcriptText = ""
        summaryText = ""
        publishTranscript()
        publishSummary()
        setProgress(0, "等待处理")
        runtimeSeconds = 0
        updateRuntimeText(0)
        setTaskRunning(false)
        if (clearInputText) {
            binding.textInput.setText("")
        }
    }

    private fun finishTaskCleanup() {
        clearTaskCacheFiles()
        selectedAudioUri = null
        transcodedFile = null
        selectedMediaPath = null
        selectedMediaType = null
    }

    private fun clearTaskCacheFiles() {
        val cache = cacheDir.listFiles().orEmpty()
        cache.forEach { file ->
            if (file.name.startsWith("extract_") ||
                file.name.startsWith("oss_upload_") ||
                file.name.startsWith("sf_upload_") ||
                file.name.startsWith("transcoded_")
            ) {
                runCatching { file.delete() }
            }
        }
        transcodedFile?.let { runCatching { it.delete() } }
        val selectedPath = selectedAudioUri?.path
        if (!selectedPath.isNullOrBlank() && selectedPath.startsWith(cacheDir.absolutePath)) {
            runCatching { File(selectedPath).delete() }
        }
    }

    private fun publishTranscript() {
        binding.transcriptOutput.text = if (transcriptText.isBlank()) "暂无内容" else transcriptText
        updateDebugStatus()
    }

    private fun publishSummary() {
        binding.summaryOutput.text = if (summaryText.isBlank()) "暂无内容" else summaryText
        updateDebugStatus()
    }

    private fun updateDebugStatus() {
        val tLen = transcriptText.length
        val sLen = summaryText.length
        val tPreview = transcriptText.take(24).replace("\n", " ")
        val sPreview = summaryText.take(24).replace("\n", " ")
        binding.debugText.text = "T=$tLen ${if (tLen > 0) "[$tPreview]" else ""} | S=$sLen ${if (sLen > 0) "[$sPreview]" else ""}"
    }

    private suspend fun persistResult() {
        val path = selectedMediaPath ?: selectedAudioUri?.toString()
        if (path.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            FileManager(this@MainActivity).saveResult(path, transcriptText, summaryText)
        }
    }

    private fun currentAsrProvider(): AsrProvider {
        return when (binding.asrInput.text?.toString().orEmpty()) {
            "SiliconFlow - SenseVoiceSmall" -> AsrProvider.SILICON_SENSEVOICE
            "SiliconFlow - TeleSpeechASR" -> AsrProvider.SILICON_TELESPEECH
            else -> AsrProvider.ALIYUN
        }
    }

    private suspend fun processAudioBySiliconFlow(audioUri: Uri, model: String) {
        ensureNotStopped()
        if (BuildConfig.SILICONFLOW_API_KEY.isBlank()) {
            setProgress(0, "SiliconFlow API Key 未配置")
            return
        }
        setProgress(12, "准备上传")
        // SiliconFlow 流程不再走 AAC->WAV 伪转码，直接以原始音频进入原生分片/压缩流程。
        val localFile = withContext(Dispatchers.IO) { copyUriToCacheFile(audioUri, "sf_upload_") }
        if (localFile == null) {
            setProgress(0, "音频文件读取失败")
            return
        }

        setProgress(28, "分析音频并分片")
        val segments = nativeAudioProcessor.splitAudio(
            sourcePath = localFile.absolutePath,
            segmentDurationMs = NativeAudioProcessor.DEFAULT_SEGMENT_DURATION_MS,
            callback = object : NativeAudioProcessor.ProgressCallback {
                override fun onProgress(progress: Int, message: String) {
                    runOnUiThread { setProgress(28 + (progress * 30 / 100), message) }
                }
            },
            shouldStop = { stopRequested.get() }
        )
        if (segments.isEmpty()) {
            setProgress(0, "音频分片失败")
            runCatching { localFile.delete() }
            return
        }
        if (!segments.any { it.absolutePath == localFile.absolutePath }) {
            runCatching { localFile.delete() }
        }

        val builder = StringBuilder()
        segments.forEachIndexed { index, seg ->
            ensureNotStopped()
            setProgress(60 + ((index + 1) * 30 / segments.size), "分片识别 ${index + 1}/${segments.size}")
            val result = withContext(Dispatchers.IO) {
                siliconFlowAsr.transcribeFile(seg, model, BuildConfig.SILICONFLOW_API_KEY)
            }
            if (!result.success) {
                setProgress(0, "第${index + 1}段识别失败: ${result.error.ifBlank { "未知错误" }}")
                return
            }
            if (result.text.isNotBlank()) {
                if (builder.isNotEmpty()) builder.append('\n')
                builder.append(result.text.trim())
            }
        }
        transcriptText = builder.toString().trim()
        if (transcriptText.isBlank()) {
            setProgress(0, "识别结果为空")
            return
        }
        publishTranscript()
        summarize(transcriptText)
    }

    private fun copyUriToCacheFile(uri: Uri, prefix: String): File? {
        return try {
            val ext = guessExtension(uri)
            val outFile = File(cacheDir, "${prefix}${System.currentTimeMillis()}.$ext")
            contentResolver.openInputStream(uri)?.use { input ->
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

    private fun launchTask(block: suspend () -> Unit) {
        currentTask?.cancel()
        stopRequested.set(false)
        setTaskRunning(true)
        currentTask = activityScope.launch {
            try {
                block()
            } catch (_: CancellationException) {
                setProgress(0, "已停止")
            } finally {
                if (!isActive) {
                    setProgress(0, "已停止")
                }
                stopRequested.set(false)
                finishTaskCleanup()
                setTaskRunning(false)
            }
        }
    }

    private fun setupStopAction() {
        binding.btnStop.setOnClickListener {
            stopRequested.set(true)
            siliconFlowAsr.cancelCurrentRequest()
            currentTask?.cancel()
        }
    }

    private fun setTaskRunning(running: Boolean) {
        binding.btnStart.isEnabled = !running
        binding.btnStart.setBackgroundColor(
            ContextCompat.getColor(this, if (running) R.color.ios_disabled else R.color.ios_accent)
        )
        binding.btnStop.isEnabled = running
        binding.btnStop.setBackgroundColor(
            ContextCompat.getColor(this, if (running) R.color.ios_danger else R.color.ios_disabled)
        )
        if (running) {
            startRuntimeTicker()
        } else {
            stopRuntimeTicker(reset = true)
        }
    }

    private data class PrepareResult(val success: Boolean, val uri: Uri? = null, val error: String = "")

    private suspend fun prepareAudioForUpload(audioUri: Uri): PrepareResult {
        val ext = guessExtension(audioUri)
        if (ext == "aac") {
            setProgress(10, "正在转码为 WAV")
            val output = withContext(Dispatchers.IO) {
                AudioTranscoder(this@MainActivity).transcodeToWav(audioUri)
            }
            return if (output != null) {
                transcodedFile = output
                PrepareResult(true, Uri.fromFile(output))
            } else {
                PrepareResult(false, error = "转码失败")
            }
        }
        return PrepareResult(true, audioUri)
    }

    private fun renderMarkdownPreview(source: String): String {
        return source
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "- ")
            .replace(Regex("`{1,3}"), "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("__(.*?)__"), "$1")
    }

    private fun showTabDialog(position: Int) {
        if (position == 0) {
            showTextDialog(getString(R.string.dialog_transcript_title), transcriptText)
        } else {
            showTextDialog(getString(R.string.dialog_summary_title), summaryText)
        }
    }

    override fun onAsrProgress(progress: Int) {
        setProgress(20 + (progress * 60 / 100), "语音识别中 $progress%")
    }

    override fun onAsrCompleted(text: String) {
        launchTask {
            transcriptText = text
            publishTranscript()
            summarize(text)
        }
    }

    override fun onAsrError(message: String) {
        setProgress(0, message)
    }

    override fun onDestroy() {
        stopRequested.set(true)
        siliconFlowAsr.cancelCurrentRequest()
        stopRuntimeTicker(reset = false)
        asrManager.release()
        super.onDestroy()
    }

    private fun ensureNotStopped() {
        if (stopRequested.get()) {
            throw CancellationException("stopped by user")
        }
    }

    private fun startRuntimeTicker() {
        runtimeJob?.cancel()
        runtimeSeconds = 0
        updateRuntimeText(runtimeSeconds)
        runtimeJob = activityScope.launch {
            while (isActive && currentTask?.isActive == true && !stopRequested.get()) {
                delay(1000)
                runtimeSeconds += 1
                updateRuntimeText(runtimeSeconds)
            }
        }
    }

    private fun stopRuntimeTicker(reset: Boolean) {
        runtimeJob?.cancel()
        runtimeJob = null
        if (reset) {
            runtimeSeconds = 0
            updateRuntimeText(runtimeSeconds)
        }
    }

    private fun updateRuntimeText(seconds: Int) {
        binding.runtimeText.text = getString(R.string.runtime_seconds_format, seconds)
    }

    private enum class AsrProvider {
        ALIYUN,
        SILICON_SENSEVOICE,
        SILICON_TELESPEECH
    }
}

