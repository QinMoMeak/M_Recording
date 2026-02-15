/**
 * @file flowingttssdk.h
 * @author 挪麦 (lengjiayi.ljy@alibaba-inc.com)
 * @copyright Copyright (c) 2024 Alibaba
 * @date 2024-04-10
 * @brief :流式语音合成接口
 */
#ifndef COMMON_INCLUDE_STREAMINPUTTTSSDK_H_
#define COMMON_INCLUDE_STREAMINPUTTTSSDK_H_

#include "nui_code.h"

enum StreamInputTtsEvent {
  STREAM_INPUT_TTS_EVENT_SYNTHESIS_STARTED = 0,
  STREAM_INPUT_TTS_EVENT_SENTENCE_BEGIN = 1,
  STREAM_INPUT_TTS_EVENT_SENTENCE_SYNTHESIS = 2,
  STREAM_INPUT_TTS_EVENT_SENTENCE_END = 3,
  STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE = 4,
  STREAM_INPUT_TTS_EVENT_TASK_FAILED = 5,
};

typedef void (*FuncNuiStreamInputTtsListenerOnEvent)(
    void *user_data, char *taskid, char *session_id, StreamInputTtsEvent event,
    int code, char *err_message, int err_message_length, char *timestamp,
    int timestamp_length, char *all_response, int all_response_length);
typedef void (*FuncNuiStreamInputTtsListenerOnData)(void *user_data,
                                                    char *taskid,
                                                    char *session_id,
                                                    char *buffer, int len);
typedef void (*FuncStreamInputTtsLogTrackListenerOnMessage)(
    void *user_data, PRODUCT_API_NAMESPACE::NuiSdkLogLevel level,
    const char *log);

struct StreamInputTtsListener {
  FuncNuiStreamInputTtsListenerOnEvent stream_input_tts_event_callback;
  FuncNuiStreamInputTtsListenerOnData stream_input_tts_user_data_callback;
  FuncStreamInputTtsLogTrackListenerOnMessage
      stream_input_tts_log_track_callback;
  void *user_data;
  StreamInputTtsListener() {
    stream_input_tts_event_callback = nullptr;
    stream_input_tts_user_data_callback = nullptr;
    stream_input_tts_log_track_callback = nullptr;
    user_data = nullptr;
  }
};

class StreamInputTtsSdk {
 public:
  StreamInputTtsSdk();
  ~StreamInputTtsSdk();

  /**
   * @brief 启动流式语音合成任务, 需要stop停止服务
   * @param ticket 建连服务和本地相关参数的json形式参数
   * @param parameters 服务相关的json形式参数
   * @param session_id 暂未使用
   * @param listener 回调
   * @param log_level 设置日志级别
   * @param save_log 是否存储本地调试日志
   * @param single_round_text 是否为单句文本,
   * 若非空则发送此单句，否则为流式文本模式
   * @return 参见错误码
   */
  int start(const char *ticket, const char *parameters, const char *session_id,
            StreamInputTtsListener *listener, int log_level, bool save_log,
            const char *single_round_text = nullptr);

  /**
   * @brief 进行单句语音合成任务, 需要stop停止服务
   * @param ticket 建连服务和本地相关参数的json形式参数
   * @param parameters 服务相关的json形式参数
   * @param text 单句文本
   * @param session_id 暂未使用
   * @param listener 回调
   * @param log_level 设置日志级别
   * @param save_log 是否存储本地调试日志
   * @return 参见错误码
   */
  int play(const char *ticket, const char *parameters, const char *text,
           const char *session_id, StreamInputTtsListener *listener,
           int log_level, bool save_log);
  int send(const char *text);
  int stop();
  int async_stop();
  int cancel();
  // int sendPing();

 private:
  int set_request(const char *parameters, const char *appkey, const char *token,
                  const char *url, int complete_waiting_ms,
                  const char *device_id, const char *single_round_text);
#ifdef NUI_INCLUDE_DASHSCOPE
  int set_dashscope_request(const char *parameters, const char *apikey,
                            const char *url, int complete_waiting_ms,
                            const char *device_id,
                            const char *single_round_text);
  void get_service_protocol_from_url(const char *ticket);
#endif /* NUI_INCLUDE_DASHSCOPE */

  void *request_;
  void *listener_;
  int service_protocol_;
  void *easy_et_handler_;
};

#endif  // COMMON_INCLUDE_STREAMINPUTTTSSDK_H_