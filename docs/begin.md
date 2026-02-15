既然你指定了使用 **阿里云智能语音交互（Intelligent Speech Interaction, ISI）** 的 Android SDK，我为你重新修订了开发文档。

阿里云的 SDK 通常需要处理 `Token` 获取（建议后端获取）、`AppKey` 配置以及音频格式采样率（通常要求 16k 采样率）。

以下是为你整理的详细开发指令，你可以直接复制发送给大模型。

---

# 开发任务书：智能学习助手（基于阿里云 ASR）

## 1. 项目目标
开发一款 Android 应用（需兼容鸿蒙系统），实现视频/音频转文字，并利用大模型进行深度总结。
- **ASR 引擎**：阿里云智能语音 Android SDK (录音文件识别/实时语音转写)。
- **LLM 引擎**：通义千问 (Qwen) 或其他主流大模型。

## 2. 核心功能模块要求

### A. 媒体预处理模块 (Media Processing)
- **功能**：用户选择视频文件（MP4/MOV）后，App 自动提取音频轨道。
- **技术要求**：
    - 使用 Android 原生 `MediaExtractor` 识别音频轨。
    - 使用 `MediaMuxer` 将其封装为 `.aac` 或 `.m4a` 格式。
    - **关键点**：由于阿里云 ASR 对采样率有要求，请在代码中预留音频重采样逻辑（推荐转为 16000Hz, 16bit, 单声道），以提高识别准确度。

### B. 阿里云 ASR 集成模块
- **参考文档**：[阿里云录音文件识别 Android SDK](https://help.aliyun.com/zh/isi/developer-reference/sdk-for-android-2)
- **实现逻辑**：
    1. **初始化**：配置 `AppKey` 和 `Token`（注：Token 应通过接口获取，代码中需留出获取 Token 的占位方法）。
    2. **文件上传与识别**：
        - 采用“录音文件识别”模式。
        - 监听 `onTranscriptionCompleted` 回调获取全文。
        - 处理识别中的进度条反馈。

### C. 场景化总结模块 (LLM Prompt Logic)
提供一个选择界面（课堂/点评/回答/通用），根据选择将 ASR 结果拼接至对应的 Prompt：

- **Prompt 模板字典**：
    - **[课堂录音]**："你是一位专业助教。请将此课堂内容整理为结构化笔记。包含：核心概念、逻辑大纲、重点案例、课后复习建议。"
    - **[老师点评]**："你是一位教学分析师。请总结老师对我表现的反馈。包含：肯定的优点、指出的不足、具体的改进建议、行动计划。"
    - **[自身回答]**："你是一位表达教练。请分析我的回答内容。包含：论点是否清晰、逻辑是否有漏洞、语言精炼建议。"
    - **[通用模式]**："请对以下文本进行深度总结，要求结构清晰，要点齐全，字数不宜过短。"

### D. UI/UX 界面要求
- **输入层**：三个大卡片按钮（视频提取、音频上传、文本粘贴）。
- **处理层**：显示识别进度的环形进度条。
- **输出层**：
    - 使用 `TabLayout` 分为“转录原文”和“AI总结”两个页面。
    - **一键复制**：每个页面下方设有浮动按钮或顶部菜单栏图标，点击即调用 `ClipboardManager` 复制全文。
    - **交互**：文字支持自由选择、长按复制。

## 3. 技术协议与接口

### 代码开发大模型需实现的具体类：
1. `AliyunAsrManager`: 封装阿里云 SDK 的初始化、Token 管理、启动识别任务。
2. `AudioExtractor`: 封装 `MediaExtractor` + `MediaMuxer` 的逻辑。
3. `SummarizerService`: 负责调用大模型接口（如调用阿里云 DashScope API 或 OpenAI 格式接口）。
4. `MainActivity`: 处理权限请求（STORAGE, RECORD_AUDIO, INTERNET）及 UI 跳转。

---

## 4. 给大模型的 Prompt 指令示例（你可以直接发这段话）

> “请按照以下要求帮我编写 Android 代码（Kotlin）：
> 1. 集成阿里云智能语音 SDK，实现一个录音文件转文字的功能。
> 2. 编写一个工具类 `MediaTool`，使用原生 `MediaExtractor` 从视频文件中抽取音频。
> 3. 设计一个 UI 界面，用户上传视频/音频并选择‘场景模式’（课堂、点评、回答、通用），识别完成后将文字发送给 AI 接口（假设接口地址为 `https://api.example.com/v1/chat/completions`）进行总结。
> 4. 总结结果必须结构清晰，通过两个 Tab 展示原文和总结，并实现一键复制功能。
> 5. 请给出核心的 `Activity` 代码、`AudioExtractor` 逻辑以及阿里云 SDK 的集成 demo 代码。”

---

## 5. 开发注意事项（提醒开发者）
1. **采样率适配**：阿里云录音识别通常支持 16000Hz。如果是视频抽取的音频是 44100Hz，请提醒模型是否需要加入音频转码步骤（如使用 AudioRecord 或 FFmpeg）。
2. **异步处理**：音频处理和 ASR 识别属于耗时操作，必须在协程（Coroutines）或异步任务中执行，避免阻塞 UI。
3. **权限申请**：需要动态申请 `READ_EXTERNAL_STORAGE`（或 Android 13+ 的 `READ_MEDIA_VIDEO`）权限。
---

## 执行约定（由用户指定）
- 每次代码修改后，将修改记录追加到本文件（`begin.md`）。
- 每次开始新一轮开发前，先读取本文件。
- 开发过程中默认直接执行，不额外询问用户确认（除非遇到阻塞或缺少必要信息）。

## 修改记录
### 2026-02-14
- 初始化 Android Kotlin 项目结构（`app` 模块、Gradle 配置、Manifest、资源文件）。
- 实现核心类：`MainActivity`、`AliyunAsrManager`、`AudioExtractor`、`SummarizerService`、`OutputFragment`。
- 完成 UI：三入口卡片、场景选择、环形进度、Tab 双页展示、复制按钮。
- 完成权限处理：`RECORD_AUDIO`、媒体读取权限（Android 13+ 与低版本兼容）。
- 预留阿里云 ASR 真正接入位与 16k 重采样入口。
- 接入 `V2.7.2-039-20260123_Android_OpenSSL` 的 `nuisdk-release.aar` 到 `app/libs`，并在 `app/build.gradle.kts` 增加本地 AAR 依赖。
- `AliyunAsrManager` 改为真实阿里云 NUI 文件转写流程：`NativeNui.initialize`、`startFileTranscriber`、`INativeFileTransCallback` 事件回调处理。
- 新增 `content://` 音频 URI 复制到缓存文件逻辑，确保可传递 `file_path` 给 SDK。
- `MainActivity` 增加 `onDestroy` 中 `asrManager.release()` 释放 SDK。
- `README.md` 更新 SDK 使用说明与当前接入状态。
- 新增本地凭证注入：`app/build.gradle.kts` 从 `local.properties` 读取 `ALIYUN_APP_KEY`、`ALIYUN_ACCESS_KEY_ID`、`ALIYUN_ACCESS_KEY_SECRET`、`ALIYUN_TOKEN` 并注入 `BuildConfig`。
- `MainActivity` 的 ASR 调用改为直接使用 `BuildConfig.ALIYUN_APP_KEY` 与 `BuildConfig.ALIYUN_TOKEN`。
- 写入 `local.properties` 默认凭证（AppKey/AK/SK），后续开发直接内置使用。
- 新增开发参考文档 `Reference.md`（阿里云 Android SDK 说明），后续开发遇到问题优先参考该文档。
- 新增 `AliyunTokenService`：基于 `CreateToken`（RPC 签名）流程，使用 `ALIYUN_ACCESS_KEY_ID/ALIYUN_ACCESS_KEY_SECRET` 动态换取 `ALIYUN_TOKEN`。
- `MainActivity` 在启动 ASR 前先自动获取 Token（若 `BuildConfig.ALIYUN_TOKEN` 已配置则优先使用手动Token）。
- 更新 `README.md`：补充 AK/SK 自动换取 Token 说明。
- 修复 Gradle 仓库冲突：移除 `app/build.gradle.kts` 中的 `repositories { flatDir(...) }`，避免与 `settings.gradle.kts` 的 `FAIL_ON_PROJECT_REPOS` 冲突。
- 本地 AAR 仍通过 `implementation(files("libs/nuisdk-release.aar"))` 生效，无需项目级仓库声明。
- 修复 `activity_main.xml` 资源合并失败：移除文件 UTF-8 BOM（解决 `mismatched input '﻿'` / `root is null`）。
- 修复资源链接失败：将 `@android:drawable/ic_menu_copy`（私有资源）替换为应用内 `@drawable/ic_copy_24`（新增矢量图标）。
- 重新执行 `:app:assembleDebug :app:assembleDebugUnitTest :app:assembleDebugAndroidTest`，构建通过。
- 修复 `AliyunTokenService` 的 CreateToken 调用方式：改为官方推荐的 `POST` 请求，并按 `POST` 方法生成签名。
- Token 获取返回结构改为 `TokenResult(token,error)`，在失败时透传服务端/HTTP错误信息。
- `MainActivity` 更新 Token 获取逻辑：在状态栏显示具体失败原因（不再仅通用提示）。
- 重新执行 `:app:assembleDebug`，构建通过。
- 修复 AK/SK 注入为空问题：`app/build.gradle.kts` 的 `localOrProject` 改为“取第一个非空值”（project/local.properties/env/default），避免空字符串阻断回退。
- 重新构建并校验 `BuildConfig`：`ALIYUN_ACCESS_KEY_ID/ALIYUN_ACCESS_KEY_SECRET/ALIYUN_APP_KEY` 已正确注入。
- 修复“识别到5%后闪退”高概率问题：`AliyunAsrManager` 将 SDK 回调统一切回主线程再通知 UI（`Handler(Looper.getMainLooper())`）。
- 增加回调空值保护：`onFileTransEventCallback` 的 `asrResult/taskId` 改为可空并做安全访问。
- 重新执行 `:app:assembleDebug`，构建通过。
- 针对 `语音识别错误240075` 优化 `AliyunAsrManager`：
  - `nls_config.format` 规范化（`m4a` 自动映射为 `aac`，避免服务端不识别 `m4a` 字面值）。
  - `sample_rate` 仅对 `wav` 设置（压缩格式不强行指定）。
  - `EVENT_ASR_ERROR` 增加服务端响应透传（`asrResult.allResponse`）和 240075 定向提示。
- 重新执行 `:app:assembleDebug`，构建通过。
- 增加服务端错误细分：识别到 `40000010` / `FREE_TRIAL_EXPIRED` 时，明确提示“试用到期，需要开通商用/续费”，避免误判为代码问题。
- 重新执行 `:app:assembleDebug`，构建通过。
- 按录音文件识别限制补强 ASR 逻辑：
  - 新增支持 `audio_address(URL)` 识别入口（`startFileTranscriptionByUrl`）。
  - 新增 URL 合法性校验：仅 `http/https`、不可含空格、host 不能是IP。
  - 新增格式白名单校验：`wav/mp3/mp4/m4a/wma/aac/ogg/amr/flac`。
  - 新增本地文件大小校验：超过 512MB 直接拦截。
- `MainActivity` 调整：输入框内容若为媒体URL则走ASR识别（不再按纯文本总结）。
- 重新执行 `:app:assembleDebug`，构建通过。
- 按“仅支持可HTTP访问URL识别”规范收敛：
  - `AliyunAsrManager.startFileTranscription` 改为直接提示不支持本地提交。
  - 保留并使用 `startFileTranscriptionByUrl(audio_address)` 作为唯一识别通道。
  - `MainActivity` 在仅选本地音频时提示“请先上传并粘贴可访问URL”。
- 重新执行 `:app:assembleDebug`，构建通过。
- 新增 OSS 自动上传服务 `OssUploaderService`：本地音频上传到 OSS 后返回可访问 URL（公有读直链/私有桶签名URL）。
- `MainActivity` 接入自动流程：选本地音频后自动执行“上传OSS -> 获取Token -> 调ASR(URL)”。
- `app/build.gradle.kts` 新增 OSS 配置注入：`OSS_ENDPOINT/OSS_BUCKET/OSS_OBJECT_PREFIX/OSS_PUBLIC_READ/OSS_SIGN_EXPIRE_SECONDS`。
- `local.properties` 补充 OSS 配置键（默认占位）。
- 按你规范保留 URL 识别主通道，构建验证通过：`:app:assembleDebug`。
- 按 Bucket 信息完成 OSS 项目配置：
  - `OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com`
  - `OSS_BUCKET=mrecording`
  - `OSS_OBJECT_PREFIX=m_recording/`
  - `OSS_PUBLIC_READ=false`
  - `OSS_SIGN_EXPIRE_SECONDS=3600`
- 重新构建并校验 `BuildConfig`：OSS 配置已成功注入。
- 参考 `Reference.md` 补充 URL 识别请求参数：`version=4.0`、`enable_sample_rate_adaptive=true`、`enable_words=false`、`auto_split=false`。
- 重新执行 `:app:assembleDebug`，构建通过。
- 按“普通版录音文件识别 v4.0”完成链路切换：新增 `AliyunFileTransApiService`，使用 POP API `SubmitTask + GetTaskResult` 提交URL并轮询结果。
- `MainActivity` 识别流程调整为：本地音频上传OSS -> 获取URL -> 调用普通版录音文件识别API -> 返回结果后自动总结。
- `app/build.gradle.kts` 新增 FILETRANS 配置注入：`FILETRANS_REGION_ID`、`FILETRANS_DOMAIN`。
- 更新 `README.md`：补充普通版录音文件识别说明。
- 重新执行 `:app:assembleDebug`，构建通过。
- 修复“原文正常但 AI 总结为空”：SummarizerService 增加本地回退总结逻辑（AI接口未配置、HTTP失败、返回为空、异常时自动回退），避免总结页空白。
### 2026-02-15
- begin.md 统一为 UTF-8 编码写入。
- 帮助按钮移至右上角，避免被视频提取按钮遮挡。
- 移除“文本粘贴”按钮与右下角复制按钮。
- 转录/总结弹窗由 Tab 本身触发；新增美化弹窗布局并提供一键复制。
### 2026-02-15
- 仅对 AAC 进行转码，MP3 保持原格式不变。
- 转录/总结弹窗改为 Tab 点击触发（不允许连续点击同一 Tab 触发）。
- 弹窗 UI 美化：圆角卡片、加长高度、移除确认按钮，新增关闭按钮与一键复制。
- AI 总结弹窗支持 MD 源码/预览切换（简易渲染）。
- 活动页空文本占位改为 @string/dialog_empty，修复乱码并确保 UTF-8 写入。

### 2026-02-15
- 修复 activity_main.xml UTF-8 BOM 导致的资源编译失败，改为无 BOM UTF-8 写入。
### 2026-02-15
- 清理 begin.md 中文乱码与控制字符，移除损坏的记录块并保持 UTF-8 无 BOM。
### 2026-02-15
- 帮助说明补充 SiliconFlow 上传限制（1小时/50MB）与阿里云试用配额（每日2小时）。
### 2026-02-15
- Tab 点击弹窗支持重复点击触发（onTabReselected 同步弹窗）。
- 弹窗移除关闭按钮，保留一键复制。
- 新增“总结输出格式”选择（md/html_doc/json），并将格式约束注入总结提示词。
### 2026-02-15
- 整理项目结构：新增 docs/，并将 Reference.md、API手册.md、prompt.md、begin.md 迁移至 docs/。
- README.md 更新：补充 docs/ 文档位置与 begin.md 迁移说明。
### 2026-02-15
- 总结输出格式增加 text 选项，并更新提示词为“结构化文档总结助手”版本（md/html_doc/json/text）。
### 2026-02-15
- 弹窗切换按钮增加选中态高亮与描边，提升 MD 源码/预览切换可见性；“MD源码”改为“MD源码（可编辑）”。
- 弹窗切换按钮高亮可见性优化：选中态背景高亮+文字颜色变化（MD源码标注可编辑）。

### 2026-02-15
- 新增 Room 基础依赖与 kapt：`app/build.gradle.kts` 增加 `org.jetbrains.kotlin.kapt` 与 Room 依赖。
- 修复构建环境：`build.gradle.kts` 增加 kapt 插件版本，`gradle/wrapper/gradle-wrapper.properties` 切换到 Gradle 8.7。
- 新增 Room 数据层：`FileRecord`、`FileRecordDao`、`AppDatabase`（位于 `app/src/main/java/com/example/m_recording/data/`）。
- 新增 `FileManager`：MediaStore 扫描、去重合并、排序、隐藏/取消隐藏、写回识别结果（`app/src/main/java/com/example/m_recording/FileManager.kt`）。
- 执行 `:app:assembleDebug`，构建成功，APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

### 2026-02-15
- 新增媒体库功能：Room 数据层接入并实现 MediaStore 扫描（`FileRecord`/`FileRecordDao`/`AppDatabase` + `FileManager`）。
- 新增媒体库页面：`MediaLibraryActivity` + `MediaLibraryViewModel` + `MediaLibraryAdapter` + 排序/隐藏/多选操作。
- 新增结果详情页：`ResultDetailActivity` 展示转录与总结。
- 主界面新增媒体库入口按钮，并支持从媒体库选中文件后回到主页直接处理。
- 处理完成后写回数据库：转录与总结保存到对应 `FileRecord`。
- 新增 UI 资源：`activity_media_library.xml`、`item_media_record.xml`、`activity_result_detail.xml`、`bg_media_item.xml`、`bg_ios_badge.xml`、`bg_select_dot.xml`、`ic_back_24.xml`、`ic_refresh_24.xml`、`ic_folder_24.xml`。
- 修复资源 BOM 导致的 XML 合并失败。
- 修复 SiliconFlow 服务类中文乱码提示。
- 执行 `:app:assembleDebug`，构建成功，APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

### 2026-02-15
- 修复媒体库历史结果不显示：`FileManager.saveResult` 在记录不存在时自动补建并写入转录/总结，确保“查看结果”可用。
- MediaLibraryViewModel 属性名调整避免 Kotlin JVM 签名冲突（`currentScope`/`changeScope`）。
- 重新构建 `:app:assembleDebug` 成功，APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

### 2026-02-15
- 更新帮助弹窗文案为完整使用说明与引擎/格式提示。
- 重新构建 `:app:assembleDebug` 成功，APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

### 2026-02-15
- 版本管理调整：新增 ersion.properties，当前固定为 VERSION_NAME=1.0.1、VERSION_CODE=1。
- 自动递增机制：每次执行 ssembleDebug 成功后，自动将下一次构建版本递增  .0.1（同时 ersionCode +1）。
- Gradle 已改为从 ersion.properties 读取版本号，便于持续迭代。

### 2026-02-15
- 包名确认：
amespace 与 pplicationId 均为 com.qinmomeak.recording。
- App 图标调整：将 pp/src/main/res/mipmap-hdpi 下 ic_launcher.webp、ic_launcher_round.webp、ic_launcher_foreground.png 同步覆盖到 mdpi/xhdpi/xxhdpi/xxxhdpi，统一使用该图标资源。

### 2026-02-15
- 修复编译错误：修正 MainActivity.kt、MediaLibraryActivity.kt、OssUploaderService.kt 中多处字符串断裂导致的 Kotlin 语法错误。
- 恢复服务类：新增 AliyunAsrManager.kt、AliyunTokenService.kt、AliyunFileTransApiService.kt、SiliconFlowAsrService.kt、SummarizerService.kt、AudioTranscoder.kt，补齐主流程依赖。
- 异常源码隔离：将损坏副本目录 pp/src/main/java/com/qinmomeak/recording/ain 迁移至 docs/ain_backup，避免参与编译。
- 构建通过：./gradlew :app:assembleDebug 成功，APK 输出 pp/build/outputs/apk/debug/app-debug.apk。
- 版本自动递增生效：本次构建后已自动更新到下一构建版本 1.0.2（versionCode=2）。

### 2026-02-15
- 修复中文乱码：清理 MainActivity.kt、MediaLibraryActivity.kt、OssUploaderService.kt、ResultDetailActivity.kt 中所有用户可见乱码文本。
- 修复 MD 预览乱码：enderMarkdownPreview 的列表符号从乱码字符改为标准 - 。
- 修复任务状态文案：处理中/完成/等待/失败等状态全部改为正常中文。
- 重新构建通过：./gradlew :app:assembleDebug 成功，APK 输出 pp/build/outputs/apk/debug/app-debug.apk。
### 2026-02-15
- 修复“媒体库选视频被转为 AAC 后不可用”：媒体库启动主页时新增透传 `extra_media_type`。
- `MainActivity` 按类型分流：`video` 强制走 `processVideo`（先提取再识别），`audio` 走 `processAudio`。
- 修复提取音频后缀：`AudioExtractor` 对 MediaMuxer 输出统一使用 `.m4a`（避免 AAC 裸流/容器不匹配导致不可用）。
- 构建验证通过：`./gradlew :app:assembleDebug` 成功，APK 输出 `app/build/outputs/apk/debug/app-debug.apk`。
### 2026-02-15
- 修复“识别后仍只显示开始处理”：在 `MediaLibraryActivity` 新增 `onResume()` 自动执行 `viewModel.syncAndLoad()`，确保从处理页返回后 `isProcessed` 状态立即刷新。
- 构建验证通过：`./gradlew :app:assembleDebug` 成功，APK 输出 `app/build/outputs/apk/debug/app-debug.apk`。
### 2026-02-15
- 修复“已识别但媒体库没有查看结果”竞态：`MainActivity.persistResult()` 从异步 fire-and-forget 改为 `suspend + withContext(IO)` 同流程落库，确保任务完成前 `isProcessed` 已写入数据库。
- 构建验证通过：`./gradlew :app:assembleDebug` 成功，APK 输出 `app/build/outputs/apk/debug/app-debug.apk`。

### 2026-02-15
- 完成“场景 B（已处理文件）”闭环：`MediaLibraryActivity` 点击文件时先读取数据库最新记录，再判断是否显示“查看结果/重新识别”。
- 修复媒体库弹窗文案乱码：已处理项正确显示“查看结果”。
- 新增结果内容更新接口：`FileRecordDao.updateContent(...)` 与 `FileManager.updateResultContent(...)`。
- 重构 `ResultDetailActivity`：
  - 使用 Tab 切换“转录原文 / AI总结”；
  - 内容可编辑；
  - 支持“保存”回写 Room；
  - 支持“一键复制”当前页文本；
  - 顶部显示文件名，便于确认当前记录。
- 重做 `activity_result_detail.xml` 以匹配新交互（返回、复制、保存、Tab、编辑区）。
- 执行 `./gradlew :app:assembleDebug` 成功，APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增已生效：本次构建后下次版本为 `1.0.7`（`versionCode=7`）。

### 2026-02-15
- 修复“媒体库点击后无查看结果”体验：媒体库点击文件时改为先读取数据库最新记录再判断状态，且“已识别”判定统一为 `isProcessed || transcriptText非空 || summaryText非空`。
- 新增媒体库筛选按钮“已识别”：可一键仅查看已完成识别的文件（支持与“可见/隐藏”范围叠加）。
- 识别徽标显示逻辑同步：列表项“已完成”标签改为同一判定逻辑，避免状态不一致。
- 优化空列表提示：在已识别筛选下显示“暂无已识别文件/暂无已识别隐藏文件”。
- 调整保存结果状态：`saveResult` 的 processed 标记改为“转录或总结任一非空即为已处理”。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.8`（`versionCode=8`）。

### 2026-02-15
- 媒体库扫描改为“仅已选文件夹”：新增 `FolderFilterStore` 持久化已选择的目录（SAF Tree URI -> 相对路径前缀）。
- `FileManager.syncMediaStore()` 增加目录白名单过滤：只加载选中文件夹下的音视频；未选择文件夹时清空媒体库记录并提示先选目录。
- 同步策略调整：每次扫描后先 `clearAll()` 再写入过滤结果，移除此前全量扫描残留（例如歌曲库内容）。
- 媒体库 UI 新增文件夹控制区：`选择文件夹`、`清空`、已选数量提示。
- `MediaLibraryActivity` 新增 `OpenDocumentTree` 选择目录与持久授权逻辑，选择后自动刷新列表。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.9`（`versionCode=9`）。

### 2026-02-15
- 新增 Room 数据表 CSV 导出能力，用于排查“是否真正写入数据库”：
  - `FileRecordDao` 新增 `getAllRecords()`；
  - `FileManager` 新增 `exportAllRecordsCsv()`，导出 `file_records` 全表到应用文件目录，包含全部字段（含转录/总结文本）。
- 媒体库新增“导出CSV”按钮（与文件夹操作同一行），点击后直接导出并 Toast 显示完整路径。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.10`（`versionCode=10`）。

### 2026-02-15
- 修复“CSV中 transcriptText/summaryText 为空”的核心问题：
  - `MainActivity.summarize` 从异步 `launch` 改为 `suspend` 串行执行；
  - 总结与 `persistResult()` 现在在同一任务协程内完成，避免 `launchTask.finally` 提前清理 `selectedMediaPath/selectedAudioUri` 导致写库丢失。
- `btnStart` 的文本直总结路径改为走 `launchTask { summarize(...) }`，保持任务生命周期一致。
- `onAsrCompleted` 改为 `launchTask` 包裹，确保识别回调场景下也会稳定落库。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.11`（`versionCode=11`）。

### 2026-02-15
- 新增 `NativeAudioProcessor`（仅原生 API）：
  - `splitAudio(sourcePath, segmentDurationMs)`：使用 `MediaExtractor + MediaMuxer` 进行音频分片；
  - 超过 50 分钟自动切段（默认 50min/段，预留安全余量）；
  - 分片时使用 `seekTo(..., SEEK_TO_PREVIOUS_SYNC)` 保证切点稳定；
  - 分片后若单段仍 >50MB，触发 `MediaCodec` 解码/重采样/单声道/低码率 AAC（16k + mono + 32kbps）压缩，再由 `MediaMuxer` 封装。
- SiliconFlow 流程升级为“分片串行识别”：
  - 在 `MainActivity.processAudioBySiliconFlow` 中接入分片；
  - 按分片顺序逐段调用 ASR，上一段成功后才处理下一段；
  - 各段文本通过 `StringBuilder` 拼接为完整转录，再交给总结流程。
- 增加分片与识别进度回调映射到主进度条（分片阶段/逐段识别阶段可见）。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.12`（`versionCode=12`）。

### 2026-02-15
- 修复 SiliconFlow 两类失败场景：
  1) `<1小时但>50MB` 的 MP3：不再走单段直拷贝分片，改为直接低码率重编码压缩（16k/mono/32kbps AAC）后再识别。
  2) `>1小时` 的 AAC：SiliconFlow 流程不再先走 `prepareAudioForUpload` 的 AAC->WAV 路径，改为原始音频进入 `NativeAudioProcessor` 分片/压缩，输出兼容的 `.m4a` 分片再识别。
- `NativeAudioProcessor` 增强：当无法读取元信息时不再回退原文件直传，先尝试压缩为兼容格式，失败才返回空并报分片失败。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.13`（`versionCode=13`）。

### 2026-02-15
- 按需求切换为“快速切片模式”：`NativeAudioProcessor` 不再进行本地重编码压缩（移除 MediaCodec 解码/重采样/编码路径）。
- 新策略改为仅使用 `MediaExtractor + MediaMuxer`：
  - 同时按时长与体积约束计算分片数（目标 `<1h` 且尽量 `<50MB`）；
  - 默认分片阈值 50 分钟，硬约束不超过 59 分钟；
  - 使用 `seekTo(..., SEEK_TO_PREVIOUS_SYNC)` 切点对齐；
  - 增加二次校正分片（若仍超限继续拆分该片段）。
- SiliconFlow 串行识别流程保持不变：逐片识别并拼接文本后再总结。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.14`（`versionCode=14`）。

### 2026-02-15
- 更新帮助弹窗文案为新版《M Recording 使用指南》：
  - 强化 ASR 选择说明（阿里云大文件优先、SiliconFlow 超限自动切片且速度较慢）；
  - 补充音频转码说明、四种场景模式、历史存档与前台运行建议；
  - 文案已替换到 `strings.xml` 的 `help_body`。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.15`（`versionCode=15`）。

### 2026-02-15
- 按“高可靠性流式切片方案”改造 `NativeAudioProcessor`：
  - 引入双重约束切片计算：`MaxDurationBySize = (45MB / totalBytes) * totalDuration`，最终分片时长 `SegmentTime = min(50min, MaxDurationBySize, 59min)`；
  - 目标确保每段满足 `<1h` 且尽量 `<50MB`（含 45MB 安全缓冲）。
- 新增 MP3 专用切片路径（不走 MediaMuxer）：
  - 检测到 `audio/mpeg` 或 `.mp3` 时，改用 `RandomAccessFile` 字节流切片；
  - 在目标字节位置附近查找 MP3 帧同步头（0xFFEx）后切分，提升 MP3 切片成功率与稳定性。
- AAC/M4A 路径继续使用 `MediaExtractor + MediaMuxer`，并加固封装格式字段：`sampleRate/channel/bitRate/csd-0/csd-1`。
- 新增超限二次拆分兜底：若某段仍超时长/体积约束，会继续拆分该段。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.16`（`versionCode=16`）。

### 2026-02-15
- 修复“停止任务只停主流程、不停分片/上传”问题：
  - `MainActivity` 新增 `stopRequested` 全局停止标记；
  - 停止按钮触发时同时执行：`stopRequested=true`、取消当前协程、取消 SiliconFlow 正在进行的 HTTP 请求；
  - 在分片、识别循环、总结等关键路径增加 `ensureNotStopped()` 检查，及时中断后续流程。
- `NativeAudioProcessor.splitAudio` 新增 `shouldStop` 回调，分片主循环/MP3字节切片/超限二次切分均可响应停止。
- `SiliconFlowAsrService` 新增当前请求句柄与 `cancelCurrentRequest()`，支持在停止时立刻中断网络请求。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.17`（`versionCode=17`）。

### 2026-02-15
- 新增“识别历史 CSV 备份导入/导出”完整能力：
  - 媒体库新增 `导入CSV` 按钮（配合已有 `导出CSV`）；
  - 支持从本地选择 CSV 并导入 Room 数据表 `file_records`；
  - 导入结果会提示：总计/成功/失败条数。
- `FileManager` 新增 `importRecordsCsv(uri)`：
  - 解析带引号/逗号/双引号转义的 CSV；
  - 恢复 `\n`、`\r` 到真实换行；
  - 批量 `upsertAll` 写入，避免覆盖失败。
- 同步逻辑调整：`syncMediaStore()` 不再无条件清空历史，保留“已识别记录”（`isProcessed` 或转录/总结非空），避免导入的历史在同步时被清空。
- 执行 `./gradlew :app:assembleDebug` 成功，APK：`app/build/outputs/apk/debug/app-debug.apk`。
- 自动版本递增生效：下次构建版本为 `1.0.18`（`versionCode=18`）。
