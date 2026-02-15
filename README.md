# M_Recording

基于 Kotlin 的 Android 语音学习助手 Demo：
- 视频抽取音频（MediaExtractor + MediaMuxer）
- 音频转写流程（Aliyun ASR 预留接入位）
- 场景化 Prompt 总结（课堂/点评/回答/通用）
- Tab 双页显示（转录原文 + AI 总结）和一键复制

## 关键配置
1. 在 `app/src/main/java/com/example/m_recording/MainActivity.kt` 中替换：
   - `YOUR_APP_KEY`
   - `YOUR_TOKEN`
2. 在 `app/src/main/java/com/example/m_recording/MainActivity.kt` 中替换总结接口地址与 `apiKey`。
3. 在 `AliyunAsrManager` 中替换为阿里云 ISI Android SDK 的真实调用代码。
4. 如果 `local.properties` 中配置了 `ALIYUN_ACCESS_KEY_ID` 和 `ALIYUN_ACCESS_KEY_SECRET`，应用会在运行时自动请求 `CreateToken` 获取 `ALIYUN_TOKEN`。

## 文档位置
- 参考文档与提示词已移动到 `docs/`。
- 开发记录写入 `docs/begin.md`。

## 说明
当前仓库未包含 `gradlew` wrapper 文件；可直接用 Android Studio 导入项目并同步依赖后运行。

## 已接入 SDK 包
- 已接入本地 AAR：`app/libs/nuisdk-release.aar`
- 来源：`V2.7.2-039-20260123_Android_OpenSSL/...\MinSizeRel/nuisdk-release.aar`
- 当前 `AliyunAsrManager` 已使用 `NativeNui + INativeFileTransCallback` 进行文件转写。

## OSS 自动上传识别
- 已支持流程：选择本地音频 -> 上传到 OSS -> 获取 URL -> 提交阿里云录音文件识别。
- 需要在 `local.properties` 配置：
- `OSS_ENDPOINT`（例如 `oss-cn-shanghai.aliyuncs.com`）
- `OSS_BUCKET`（你的 bucket 名）
- `OSS_OBJECT_PREFIX`（可选，默认 `m_recording/`）
- `OSS_PUBLIC_READ`（`true/false`）
- `OSS_SIGN_EXPIRE_SECONDS`（私有桶签名 URL 有效期秒数）

## 普通版录音文件识别（POP API）
- 当前识别链路已切换为 `SubmitTask + GetTaskResult`（普通版 v4.0），不再依赖极速版试用额度。
- 可选配置：
- `FILETRANS_REGION_ID`（默认随 OSS endpoint 推断）
- `FILETRANS_DOMAIN`（默认 `filetrans.{region}.aliyuncs.com`）
