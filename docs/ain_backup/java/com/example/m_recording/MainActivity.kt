 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), AliyunAsrManager.Listener {

    private lateinit var binding: ActivityMainBinding

    private val tabs = listOf("转录原文", "AI总结")
    private var transcriptText = ""
    private var summaryText = ""

    private var selectedAudioUri: Uri? = null
    private var selectedMediaPath: String? = null
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)
    private var currentTask: Job? = null
    private var transcodedFile: File? = null

    private val audioExtractor by lazy { AudioExtractor(this) }
    private val ossUploader by lazy { OssUploaderService(this) }
    private val asrManager by lazy { AliyunAsrManager(this, listener = this) }
    private val tokenService by lazy { AliyunTokenService() }
    private val fileTransApi by lazy { AliyunFileTransApiService() }
    private val siliconFlowAsr by lazy { SiliconFlowAsrService() }
    private val summarizerService by lazy { SummarizerService() }

    private val scenarioTemplates = mapOf(
        "课堂录音" to """
Role: 瀛︽湳鍐呭鎬荤粨涓撳

浣犲繀椤婚伒瀹堜互涓嬭鍒欙紝鎸夋祦绋嬭緭鍑猴細
- 涓ユ牸鎸夌粰瀹氭ā寮忓鐞嗭紝涓嶅亸绂诲姛鑳姐€?- 杈撳嚭浣跨敤缁撴瀯鍖?Markdown銆?- 涓嶈兘娣诲姞鐢ㄦ埛鏈寚瀹氱殑鍐呭銆?
妯″紡锛氳€佸笀课堂录音 (Classroom Lecture)
Role: 浣犳槸涓€浣嶆瀬鍏朵笓涓氱殑瀛︽湳鍔╂暀锛屾搮闀垮皢澶嶆潅鐨勮鍫傚綍闊宠浆鍖栦负缁撴瀯娓呮櫚鐨勫涔犵瑪璁般€?Task: 璇峰垎鏋愭彁渚涚殑璇惧爞杞枃瀛楃锛屽苟鐢熸垚涓€浠芥繁搴︽€荤粨銆?Requirements:
1. 鏍稿績涓婚锛氱敤涓€鍙ヨ瘽姒傛嫭鏈璇剧▼鐨勬牳蹇冭璁哄唴瀹广€?2. 鐭ヨ瘑澶х翰锛氭⒊鐞嗚绋嬬殑閫昏緫鏋舵瀯锛屼娇鐢ㄥ绾ф爣棰橈紙# ## ###锛夊憟鐜般€?3. 閲嶇偣姒傚康锛氭彁鍙栬鍫備腑鎻愬埌鐨勪笓涓氭湳璇垨鏍稿績姒傚康锛屽苟缁欏嚭绠€鏄庤В閲娿€?4. 妗堜緥/渚嬮锛氳褰曡€佸笀涓轰簡瑙ｉ噴鐭ヨ瘑鐐规墍寮曠敤鐨勬渚嬨€佹晠浜嬫垨渚嬮銆?5. 寰呭姙/寤鸿锛氭牴鎹绋嬪唴瀹癸紝鍒楀嚭瀛︾敓璇惧悗闇€瑕佽繘涓€姝ュ涔犳垨鏌ラ槄鐨勮祫鏂欏缓璁€?Output Style: 閫昏緫涓ュ瘑銆佽鐐硅瀹烇紝涓嶈鐪佺暐缁嗚妭锛屽瓧鏁颁笉灏戜簬 500 瀛楋紙瑙嗗師鏂囬暱搴﹁€屽畾锛夈€?        """.trimIndent(),
        "老师点评" to """
Role: 瀛︽湳鍐呭鎬荤粨涓撳

浣犲繀椤婚伒瀹堜互涓嬭鍒欙紝鎸夋祦绋嬭緭鍑猴細
- 涓ユ牸鎸夌粰瀹氭ā寮忓鐞嗭紝涓嶅亸绂诲姛鑳姐€?- 杈撳嚭浣跨敤缁撴瀯鍖?Markdown銆?- 涓嶈兘娣诲姞鐢ㄦ埛鏈寚瀹氱殑鍐呭銆?
妯″紡锛氳€佸笀鐐硅瘎鍙嶉 (Teacher's Feedback)
Role: 浣犳槸涓€浣嶆暀瀛﹁瘖鏂笓瀹讹紝鎿呴暱浠庤€佸笀鐨勫弽棣堜腑绮惧噯鎻愬彇鏀硅繘鎰忚銆?Task: 璇峰垎鏋愯€佸笀瀵瑰鐢熷洖绛旂殑鐐硅瘎鍐呭锛屾暣鐞嗘垚涓€浠解€滆瘖鏂姤鍛娾€濄€?Requirements:
1. 鎬讳綋璇勪环锛氳€佸笀瀵规娆¤〃鐜扮殑鏁翠綋鎬佸害锛堝鑲畾銆佷弗鍘夈€侀紦鍔辩瓑锛夈€?2. 浜偣鎬荤粨锛氳€佸笀鏄庣‘琛ㄦ壃鎴栬鍙殑鍦版柟锛堜紭鐐规槸浠€涔堬紵锛夈€?3. 鐥涚偣鍒嗘瀽锛氳€佸笀鎸囧嚭鐨勬牳蹇冮棶棰樸€侀€昏緫婕忔礊鎴栫煡璇嗙洸鍖猴紙鍝噷鍋氶敊浜嗭紵锛夈€?4. 鏀硅繘寤鸿锛氳€佸笀缁欏嚭鐨勫叿浣撲慨鏀规柟妗堟垨涓嬩竴姝ヨ鍔ㄦ寚寮曪紙鎬庝箞鍋氭洿濂斤紵锛夈€?5. 閲戝彞鏀跺綍锛氭憳褰曡€佸笀鐐硅瘎涓瀬鍏锋寚瀵兼剰涔夌殑鍘熻瘽銆?Output Style: 閲囩敤鍒楄〃褰㈠紡锛岃瑷€涓偗涓撲笟锛岄噸鐐圭獊鍑恒€?        """.trimIndent(),
        "自身回答" to """
Role: 瀛︽湳鍐呭鎬荤粨涓撳

浣犲繀椤婚伒瀹堜互涓嬭鍒欙紝鎸夋祦绋嬭緭鍑猴細
- 涓ユ牸鎸夌粰瀹氭ā寮忓鐞嗭紝涓嶅亸绂诲姛鑳姐€?- 杈撳嚭浣跨敤缁撴瀯鍖?Markdown銆?- 涓嶈兘娣诲姞鐢ㄦ埛鏈寚瀹氱殑鍐呭銆?
妯″紡锛氳嚜韬洖绛旇褰?(Self-Response Analysis)
Role: 浣犳槸涓€浣嶄笓涓氱殑琛ㄨ揪鏁欑粌鍜屾€濈淮閫昏緫鍒嗘瀽甯堛€?Task: 杩欐槸鎴戝洖绛旈棶棰樼殑褰曢煶杞枃瀛楃锛岃甯垜杩涜娣卞害澶嶇洏鍜屽鐩樹紭鍖栥€?Requirements:
1. 璁虹偣妫€鏍革細鎴戠殑鏍稿績瑙傜偣鏄惁娓呮櫚锛熸槸鍚︾洿鎺ュ洖绛斾簡闂锛?2. 閫昏緫鎺ㄥ锛氭垜鐨勮璇佽繃绋嬫槸鍚﹀瓨鍦ㄩ€昏緫璺宠穬鎴栧墠鍚庣煕鐩撅紵
3. 璇█琛ㄨ揪锛氬垎鏋愭垜鐨勫彛璇範鎯紙鏄惁鏈夎繃澶氱殑璧樿瘝濡傗€滈偅涓€佺劧鍚庘€濄€佽姘旀槸鍚﹁嚜淇°€佹帾杈炴槸鍚︿笓涓氾級銆?4. 璇濇湳浼樺寲锛氳鍩轰簬鎴戠殑鍘熷鍥炵瓟锛屾彁渚涗竴浠解€滀笓涓氳繘闃剁増鈥濈殑鍥炵瓟鑼冩枃锛岃姹傝〃杈炬洿绮惧噯銆佹洿鍏疯鏈嶅姏銆?Output Style: 榧撳姳鎬т笌鎵瑰垽鎬у苟瀛橈紝渚ч噸浜庘€滃浣曡寰楁洿濂解€濄€?        """.trimIndent(),
        "通用模式" to """
Role: 瀛︽湳鍐呭鎬荤粨涓撳

浣犲繀椤婚伒瀹堜互涓嬭鍒欙紝鎸夋祦绋嬭緭鍑猴細
- 涓ユ牸鎸夌粰瀹氭ā寮忓鐞嗭紝涓嶅亸绂诲姛鑳姐€?- 杈撳嚭浣跨敤缁撴瀯鍖?Markdown銆?- 涓嶈兘娣诲姞鐢ㄦ埛鏈寚瀹氱殑鍐呭銆?
妯″紡锛氶€氱敤/鍏朵粬瀛︿範璧勬枡 (General Study)
Role: 浣犳槸涓€浣嶉珮鏁堢殑淇℃伅鏁寸悊涓撳锛屾搮闀夸粠鏉備贡鐨勫璇濇垨闄堣堪涓彁鍙栦环鍊笺€?Task: 璇峰浠ヤ笅鏂囨湰杩涜娣卞害澶嶇洏鍜屼俊鎭彁鐐笺€?Requirements:
1. 淇℃伅鎽樿锛氱畝瑕佽鏄庤繖娈靛唴瀹圭殑鏍稿績鑳屾櫙銆?2. 鍏抽敭瑕佺偣锛氬垪鍑烘枃鏈腑娑夊強鐨勬墍鏈夐噸瑕佷簨瀹炪€佹暟鎹垨瑙傜偣銆?3. 閫昏緫鍏崇郴锛氬垎鏋愬悇瑕佺偣涔嬮棿鐨勫洜鏋溿€佸苟鍒楁垨閫掕繘鍏崇郴銆?4. 琛屽姩鐐?缁撹锛氬鏋滄秹鍙婂喅绛栨垨浠诲姟锛岃鏄庣‘鍒楀嚭 Action Items锛堝緟鍔炰簨椤癸級銆?Output Style: 缁撴瀯鍖?Markdown 鏍煎紡锛屽瓧鏁伴€備腑锛屾兜鐩栨墍鏈夋牳蹇冧俊鎭紝涓嶉仐婕忕粏鑺傘€?        """.trimIndent()
    )
    private val asrOptions = listOf(
        "闃块噷浜戝綍闊虫枃浠惰瘑鍒?,
        "SiliconFlow - SenseVoiceSmall",
        "SiliconFlow - TeleSpeechASR"
    )
    private val formatOptions = listOf("md", "html_doc", "json", "text")

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            startNewTask(clearInputText = false)
            launchTask { processVideo(uri) }
        }
    }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedAudioUri = uri
        if (uri != null) {
            startNewTask(clearInputText = false)
            binding.statusText.text = "宸查€夋嫨闊抽: ${uri.lastPathSegment ?: "鏈煡"}"
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
        ensureBasePermissions()

        val mediaPath = intent.getStringExtra(MediaLibraryActivity.EXTRA_MEDIA_URI)
        if (!mediaPath.isNullOrBlank()) {
            selectedMediaPath = mediaPath
            selectedAudioUri = Uri.parse(mediaPath)
            binding.statusText.text = "宸查€夋嫨濯掍綋搴撴枃浠?
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
                        setProgress(0, "SiliconFlow 浠呮敮鎸佹湰鍦版枃浠朵笂浼?)
                    }
                }

                inputText.isNotEmpty() -> {
                    startNewTask(clearInputText = false)
                    transcriptText = inputText
                    publishTranscript()
                    summarize(transcriptText)
                }

                selectedAudioUri != null -> {
                    startNewTask(clearInputText = false)
                    launchTask { processAudio(selectedAudioUri!!) }
                }

                else -> {
                    Toast.makeText(this, "璇峰厛閫夋嫨瑙嗛/闊抽鎴栫矘璐存枃鏈?, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun processVideo(videoUri: Uri) {
        clearTaskCacheFiles()
        setProgress(5, "姝ｅ湪鎻愬彇闊抽")
        val output = withContext(Dispatchers.IO) {
            audioExtractor.extractAudioFromVideo(videoUri)
        }
        if (output == null) {
            setProgress(0, "闊抽鎻愬彇澶辫触")
            return
        }
        selectedAudioUri = Uri.fromFile(output)
        setProgress(20, "闊抽鎻愬彇瀹屾垚锛岃嚜鍔ㄤ笂浼犺瘑鍒?)
        processAudio(selectedAudioUri!!)
    }

    private suspend fun processAudio(audioUri: Uri) {
        when (currentAsrProvider()) {
            AsrProvider.ALIYUN -> {
                val prepared = prepareAudioForUpload(audioUri)
                if (!prepared.success) {
                    setProgress(0, prepared.error.ifBlank { "闊抽鍑嗗澶辫触" })
                    return
                }
                val preparedUri = prepared.uri ?: return
                setProgress(8, "涓婁紶闊抽鍒癘SS")
                val uploadResult = withContext(Dispatchers.IO) {
                    ossUploader.uploadAudio(preparedUri)
                }
                val audioUrl = uploadResult.url
                if (audioUrl.isNullOrBlank()) {
                    setProgress(0, "OSS涓婁紶澶辫触: ${uploadResult.error ?: "鏈煡閿欒"}")
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
        val apiResult = withContext(Dispatchers.IO) {
            fileTransApi.transcribeByUrl(
                fileUrl = audioUrl,
                appKey = BuildConfig.ALIYUN_APP_KEY
            ) { progress, status ->
                runOnUiThread { setProgress(progress, status) }
            }
        }
        if (!apiResult.success) {
            setProgress(0, apiResult.error.ifBlank { "褰曢煶鏂囦欢璇嗗埆澶辫触" })
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

    private fun summarize(transcript: String) {
        val scenario = binding.scenarioInput.text?.toString().orEmpty()
        val systemPrompt = scenarioTemplates[scenario] ?: scenarioTemplates.getValue("通用模式")
        val format = binding.formatInput.text?.toString().orEmpty().ifBlank { "md" }
        val userPrompt = """
浣犳槸鈥滅粨鏋勫寲鏂囨。鎬荤粨鍔╂墜鈥濄€備綘浼氭牴鎹敤鎴锋寚瀹氱殑杈撳嚭鏍煎紡锛屽皢杈撳叆鍐呭鎬荤粨骞舵帓鐗堟垚鍙洿鎺ヤ娇鐢ㄧ殑鏂囨。鍐呭銆?
銆愯緭鍏ュ弬鏁般€?- format: 鍙兘鏄?"md" / "html_doc" / "json" / "text" 涔嬩竴
- content: 闇€瑕佹€荤粨鐨勫師濮嬪唴瀹?
銆愰€氱敤瑕佹眰銆?1. 杈撳嚭璇█涓?content 淇濇寔涓€鑷淬€?2. 鍐呭缁撴瀯鍥哄畾涓猴細
   - 鏍囬
   - 鏍稿績瑕佺偣
   - 鍏抽敭缁撹
   - 琛屽姩椤癸紙濡傛湁锛?   - 椋庨櫓鎴栨敞鎰忎簨椤癸紙濡傛湁锛?3. 涓嶈娣诲姞瑙ｉ噴鎬ф枃瀛椼€?4. 涓嶈杈撳嚭鈥滀互涓嬫槸鈥?鎴戝皢鈥︹€濈瓑寮€鍦鸿銆?5. 涓嶈娣峰悎鏍煎紡銆?6. 涓嶈缂栭€犱笉瀛樺湪鐨勪俊鎭紝涓嶇‘瀹氫俊鎭敤鈥滐紙寰呯‘璁わ級鈥濇爣娉ㄣ€?7. 鍙緭鍑烘渶缁堢粨鏋溿€?
--------------------------------------------------
銆愬綋 format = "md" 鏃躲€?
- 杈撳嚭鏍囧噯 Markdown锛堝吋瀹瑰父瑙佺紪杈戝櫒锛夈€?- 涓绘爣棰樹娇鐢?##
- 灏忔爣棰樹娇鐢?###
- 鍒楄〃浣跨敤 -
- 鏈夐『搴忎娇鐢?1.
- 閲嶈璇嶄娇鐢?**鍔犵矖**
- 娈佃惤涔嬮棿绌轰竴琛?- 绂佹浣跨敤浠ｇ爜鍧楋紙涓嶈鍑虹幇 ```锛?
--------------------------------------------------
銆愬綋 format = "html_doc" 鏃躲€戯紙閫傜敤浜?Word 2003锛?
- 鍙緭鍑?HTML 鐗囨
- 涓嶈鍖呭惈 <html>銆?head>銆?body>
- 涓绘爣棰樹娇鐢?<h2>
- 灏忔爣棰樹娇鐢?<h3>
- 鍒楄〃浣跨敤 <ul><li>鈥?/li></ul>
- 鏈夊簭鍒楄〃浣跨敤 <ol><li>鈥?/li></ol>
- 寮鸿皟鐢?<b>
- 涓嶈浣跨敤 CSS
- 涓嶈浣跨敤 class
- 涓嶈浣跨敤 style
- 涓嶈杈撳嚭瑙ｉ噴鎬ф枃鏈?
--------------------------------------------------
銆愬綋 format = "json" 鏃躲€?
- 鍙緭鍑哄悎娉?JSON
- 涓嶈杈撳嚭 Markdown
- 涓嶈杈撳嚭瑙ｉ噴鏂囧瓧
- 蹇呴』鍙 JSON.parse 姝ｇ‘瑙ｆ瀽
- 缁撴瀯蹇呴』濡備笅锛?
{
  "title": string,
  "summary_bullets": string[],
  "key_findings": string[],
  "action_items": [
    {
      "item": string,
      "owner": string | null,
      "due": string | null
    }
  ],
  "risks": string[],
  "open_questions": string[]
}

- 濡傛灉鏌愰」娌℃湁鍐呭锛岃緭鍑虹┖鏁扮粍 []
- owner/due 涓嶇‘瀹氭椂杈撳嚭 null

--------------------------------------------------
銆愬綋 format = "text" 鏃躲€戯紙绾枃鏈帓鐗堬級

- 鍙緭鍑虹函鏂囨湰
- 涓嶄娇鐢?Markdown 绗﹀彿
- 涓嶄娇鐢?HTML 鏍囩
- 涓嶄娇鐢ㄤ唬鐮佸潡
- 浣跨敤鎹㈣鍜岀缉杩涙帓鐗?- 涓€绾ф爣棰樺崟鐙竴琛?- 灏忔爣棰樺悗鍔犲啋鍙?- 鍒楄〃椤瑰墠浣跨敤 "- "
- 鍒楄〃椤圭缉杩涗袱涓┖鏍?- 鍚勯儴鍒嗕箣闂寸┖涓€琛?- 淇濇寔娓呮櫚鍙锛屽彲鐩存帴澶嶅埗鍒?Word 2003

绀轰緥缁撴瀯鏍煎紡锛堜粎浣滀负鏍煎紡鍙傝€冿級锛?
椤圭洰鎬荤粨

鏍稿績瑕佺偣锛?  - 瑕佺偣涓€
  - 瑕佺偣浜?
鍏抽敭缁撹锛?  - 缁撹涓€
  - 缁撹浜?
--------------------------------------------------

銆愯緭鍑恒€?涓ユ牸鎸夌収 format 杈撳嚭瀵瑰簲鏍煎紡鍐呭銆?
format: "$format"
content:
$transcript
""".trimIndent()

        activityScope.launch {
            setProgress(80, "AI 鎬荤粨涓?)
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
            publishSummary()
            persistResult()
            setProgress(100, "澶勭悊瀹屾垚")
            finishTaskCleanup()
            setTaskRunning(false)
        }
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
                Toast.makeText(this, "鏆傛棤鍙鍒跺唴瀹?, Toast.LENGTH_SHORT).show()
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
        setProgress(0, "绛夊緟澶勭悊")
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
        binding.transcriptOutput.text = if (transcriptText.isBlank()) "鏆傛棤鍐呭" else transcriptText
        updateDebugStatus()
    }

    private fun publishSummary() {
        binding.summaryOutput.text = if (summaryText.isBlank()) "鏆傛棤鍐呭" else summaryText
        updateDebugStatus()
    }

    private fun updateDebugStatus() {
        val tLen = transcriptText.length
        val sLen = summaryText.length
        val tPreview = transcriptText.take(24).replace("\n", " ")
        val sPreview = summaryText.take(24).replace("\n", " ")
        binding.debugText.text = "T=$tLen ${if (tLen > 0) "[$tPreview]" else ""} | S=$sLen ${if (sLen > 0) "[$sPreview]" else ""}"
    }

    private fun persistResult() {
        val path = selectedMediaPath ?: selectedAudioUri?.toString()
        if (path.isNullOrBlank()) return
        activityScope.launch(Dispatchers.IO) {
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
        if (BuildConfig.SILICONFLOW_API_KEY.isBlank()) {
            setProgress(0, "SiliconFlow API Key 鏈厤缃?)
            return
        }
        setProgress(12, "鍑嗗涓婁紶")
        val prepared = prepareAudioForUpload(audioUri)
        if (!prepared.success) {
            setProgress(0, prepared.error.ifBlank { "闊抽鍑嗗澶辫触" })
            return
        }
        val preparedUri = prepared.uri ?: return
        val localFile = withContext(Dispatchers.IO) { copyUriToCacheFile(preparedUri, "sf_upload_") }
        if (localFile == null) {
            setProgress(0, "闊抽鏂囦欢璇诲彇澶辫触")
            return
        }
        if (localFile.length() > 50L * 1024 * 1024) {
            setProgress(0, "闊抽瓒呰繃 50MB锛孲iliconFlow 涓嶆敮鎸?)
            runCatching { localFile.delete() }
            return
        }
        setProgress(45, "涓婁紶骞惰浆鍐?)
        val result = withContext(Dispatchers.IO) {
            siliconFlowAsr.transcribeFile(localFile, model, BuildConfig.SILICONFLOW_API_KEY)
        }
        if (!result.success) {
            setProgress(0, result.error.ifBlank { "SiliconFlow 杞啓澶辫触" })
            finishTaskCleanup()
            return
        }
        transcriptText = result.text
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
        setTaskRunning(true)
        currentTask = activityScope.launch {
            try {
                block()
            } finally {
                if (!isActive) {
                    setProgress(0, "宸插仠姝?)
                }
                finishTaskCleanup()
                setTaskRunning(false)
            }
        }
    }

    private fun setupStopAction() {
        binding.btnStop.setOnClickListener {
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
    }

    private data class PrepareResult(val success: Boolean, val uri: Uri? = null, val error: String = "")

    private suspend fun prepareAudioForUpload(audioUri: Uri): PrepareResult {
        val ext = guessExtension(audioUri)
        if (ext == "aac") {
            setProgress(10, "姝ｅ湪杞爜涓?WAV")
            val output = withContext(Dispatchers.IO) {
                AudioTranscoder(this@MainActivity).transcodeToWav(audioUri)
            }
            return if (output != null) {
                transcodedFile = output
                PrepareResult(true, Uri.fromFile(output))
            } else {
                PrepareResult(false, error = "杞爜澶辫触")
            }
        }
        return PrepareResult(true, audioUri)
    }

    private fun renderMarkdownPreview(source: String): String {
        return source
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "鈥?")
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
        setProgress(20 + (progress * 60 / 100), "璇煶璇嗗埆涓? $progress%")
    }

    override fun onAsrCompleted(text: String) {
        transcriptText = text
        publishTranscript()
        summarize(text)
    }

    override fun onAsrError(message: String) {
        setProgress(0, message)
    }

    override fun onDestroy() {
        asrManager.release()
        super.onDestroy()
    }

    private enum class AsrProvider {
        ALIYUN,
        SILICON_SENSEVOICE,
        SILICON_TELESPEECH
    }
}


