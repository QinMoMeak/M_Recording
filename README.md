# M Recording

Android 录音/视频转写与总结工具（Kotlin，包名 `com.qinmomeak.recording`）。

## 核心能力
- 视频提取音频并转写。
- 双 ASR 引擎可选：
  - 阿里云录音文件识别（URL 提交，`SubmitTask + GetTaskResult`，v4.0）。
  - SiliconFlow（`TeleSpeechASR` / `SenseVoiceSmall`）。
- 超限音频自动分片（按时长与体积双约束）并串行识别后合并文本。
- 场景化总结（课堂录音/老师点评/自身回答/通用模式）。
- 总结输出格式可选：`md`、`html_doc`、`json`、`text`。
- 结果可编辑、可复制；支持 MD 源码与预览切换。
- 媒体库管理：
  - 仅扫描“用户已选文件夹”内音视频文件。
  - 已识别筛选、排序记忆、隐藏/取消隐藏。
  - 历史结果详情页（查看结果/重新识别）。
- Room 本地存储 + CSV 导入导出备份。

## 项目结构
- `app/`：Android 应用源码
- `docs/`：项目文档与开发记录
- `docs/begin.md`：迭代变更日志
- `docs/Reference.md`、`docs/API手册.md`、`docs/prompt.md`：接口与提示词参考

## 本地配置
在 `local.properties` 配置（或同名环境变量）：

- 阿里云 ASR
  - `ALIYUN_APP_KEY`
  - `ALIYUN_ACCESS_KEY_ID`
  - `ALIYUN_ACCESS_KEY_SECRET`
  - `ALIYUN_TOKEN`（可选，手动 token 优先）

- OSS 上传
  - `OSS_ENDPOINT`（如 `oss-cn-beijing.aliyuncs.com`）
  - `OSS_BUCKET`
  - `OSS_OBJECT_PREFIX`（默认 `m_recording/`）
  - `OSS_PUBLIC_READ`（`true/false`）
  - `OSS_SIGN_EXPIRE_SECONDS`（默认 `3600`）

- 阿里云文件识别域名
  - `FILETRANS_REGION_ID`（默认根据 endpoint 推断）
  - `FILETRANS_DOMAIN`（默认 `filetrans.{region}.aliyuncs.com`）

- AI 总结
  - `AI_ENDPOINT`（默认 `https://apis.iflow.cn/v1/chat/completions`）
  - `AI_API_KEY`
  - `AI_MODEL`（默认 `qwen3-max`）

- SiliconFlow ASR
  - `SILICONFLOW_API_KEY`

## 构建与版本
- 构建 Debug：
```bash
./gradlew :app:assembleDebug
```
- 输出 APK：`app/build/outputs/apk/debug/app-debug.apk`
- 版本来源：`version.properties`
- 每次 `assembleDebug` 成功后，自动执行补丁号 `+0.0.1`，并同步 `versionCode +1`。

## 使用限制（按服务商）
- 阿里云试用：每天约 2 小时音频时长额度。
- SiliconFlow：单文件建议不超过 1 小时且不超过 50MB；超过后会进入自动切片流程。

## 更新地址
https://github.com/QinMoMeak/M_Recording/releases
