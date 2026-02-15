本页总览

API 手册
======
了解支持的模型
我们提供多种高性能的 AI 模型供您选择使用。为了获取最新的模型信息、详细参数配置和使用说明，请访问我们的模型页面：

查看完整模型列表
点击查看所有支持的模型

在模型页面，您可以：

查看所有可用模型的详细信息
了解每个模型的上下文长度和输出限制
查看模型的性能特点和适用场景
复制 模型ID 用于 API 调用

第三步：配置接口参数
使用以下配置信息来调用心流 API：

参数名称	参数值	说明
HTTP URL	https://apis.iflow.cn/v1/chat/completions	聊天接口，支持流式和非流式
API Key	你的密钥	在 控制台 获取
OpenAi Base URL	https://apis.iflow.cn/v1	OpenAI SDK使用
创建文本对话请求[​](# "创建文本对话请求的直接链接")
------------------------------

POST

`[https://apis.iflow.cn/v1/chat/completions](https://apis.iflow.cn/v1/chat/completions)`

**Content-Type:**`application/json`

* * *

### 请求参数说明[​](# "请求参数说明的直接链接")

Authorization

*   类型
    
    `string`
    
*   位置
    
    `header`
    
*   是否必填
    
    是
    
*   描述
    
    使用以下格式进行身份验证：`Bearer <your api key>` （访问[心流](https://iflow.cn/?open=setting)官网登陆获取API KEY）。
    

* * *

`LLM 模型`

参数名

类型

是否必填

默认值

描述

`messages`

`object[]`

是

\-

构成当前对话的消息列表。

`messages.content`

`string`

是

`中国大模型行业2025年将会迎来哪些机遇和挑战？`

消息的内容。

`messages.role`

`enum<string>`

是

`user`

消息作者的角色。 可选值：`user`  
, `assistant`  
, `system`

`model`

`enum<string>`

是

`tstars2.0`

对应的模型名称。 为更好的提升服务质量，我们将不定期对本服务提供的模型做相关变更，包括但不限于模型上下线、模型服务能力调整，我们会在可行的情况下以公告、消息推送等适当的方式进行通知。 支持的模型请参考快速开始页面。

`frequency_penalty`

`number`

否

`0.5`

调整生成 token 的频率惩罚，用于控制重复性。

`max_tokens`

`integer`

否

`512`

生成的最大 token 数量。 取值范围：`1 < x < 8192`

`n`

`integer`

否

`1`

返回的生成结果数量。

`response_format`

`object`

否

\-

指定模型输出格式的对象。

`response_format.type`

`string`

否

\-

响应格式的类型。

`stop`

`string[]`

`null`

否

\-

`stream`

`boolean`

否

`false`

如果设置为 `true`  
，token 将作为服务器发送事件（SSE）逐步返回。

`temperature`

`number`

否

`0.7`

控制响应的随机性。值越低，输出越确定；值越高，输出越随机。

`tools`

`object[]`

否

\-

模型可能调用的工具列表。目前仅支持函数作为工具。使用此参数提供一个函数列表，模型可能会为其生成 JSON 输入。最多支持 128 个函数。

`tools.function`

`object`

否

\-

函数对象。

`tools.function.name`

`string`

否

\-

要调用的函数名称。必须由字母、数字、下划线或短横线组成，最大长度为 64。

`tools.function.description`

`string`

否

\-

函数的描述，用于模型选择何时以及如何调用该函数。

`tools.function.parameters`

`object`

否

\-

函数接受的参数，描述为 JSON Schema 对象。如果不指定参数，则定义了一个空参数列表的函数。

`tools.function.strict`

`boolean`

`null`

否

`false`

`tools.type`

`enum<string>`

否

`function`

工具的类型。目前仅支持 `function`  
。

`top_k`

`number`

否

`50`

限制 token 选择范围为前 k 个候选。

`top_p`

`number`

否

`0.7`

核采样参数，用于根据累积概率动态调整每个预测 token 的选择范围。

* * *

### 请求举例[​](# "请求举例的直接链接")

*   CURL
*   Python
*   Javascript
*   Java

    curl --request POST \  --url https://apis.iflow.cn/v1/chat/completions \  --header 'Authorization: Bearer <iflow API KEY>' \  --header 'Content-Type: application/json' \  --data '{  "model": "tstars2.0",  "messages": [    {      "role": "user",      "content": "中国大模型行业2025年将会迎来哪些机遇和挑战？"    }  ],  "stream": false,  "max_tokens": 512,  "stop": [    "null"  ],  "temperature": 0.7,  "top_p": 0.7,  "top_k": 50,  "frequency_penalty": 0.5,  "n": 1,  "response_format": {    "type": "text"  },  "tools": [    {      "type": "function",      "function": {        "description": "<string>",        "name": "<string>",        "parameters": {},        "strict": false      }    }  ]}'

    import requestsurl = "https://apis.iflow.cn/v1/chat/completions"payload = {    "model": "tstars2.0",    "messages": [        {            "role": "user",            "content": "中国大模型行业2025年将会迎来哪些机遇和挑战？"        }    ],    "stream": False,    "max_tokens": 512,    "stop": ["null"],    "temperature": 0.7,    "top_p": 0.7,    "top_k": 50,    "frequency_penalty": 0.5,    "n": 1,    "response_format": {"type": "text"},    "tools": [        {            "type": "function",            "function": {                "description": "<string>",                "name": "<string>",                "parameters": {},                "strict": False            }        }    ]}headers = {    "Authorization": "Bearer <XinLiu API KEY>",    "Content-Type": "application/json"}response = requests.request("POST", url, json=payload, headers=headers)print(response.text)

    const url = "https://apis.iflow.cn/v1/chat/completions";const payload = {  "model": "tstars2.0",  "messages": [    {      "role": "user",      "content": "中国大模型行业2025年将会迎来哪些机遇和挑战？"    }  ],  "stream": false,  "max_tokens": 512,  "stop": ["null"],  "temperature": 0.7,  "top_p": 0.7,  "top_k": 50,  "frequency_penalty": 0.5,  "n": 1,  "response_format": {"type": "text"},  "tools": [    {      "type": "function",      "function": {        "description": "<string>",        "name": "<string>",        "parameters": {},        "strict": false      }    }  ]};const headers = {  "Authorization": "Bearer <XinLiu API KEY>",  "Content-Type": "application/json"};fetch(url, {  method: "POST",  headers: headers,  body: JSON.stringify(payload)}).then(response => response.json()).then(data => console.log(data));

    import com.fasterxml.jackson.databind.ObjectMapper;import java.util.*;// 构建消息对象Map<String, Object> message = new HashMap<>();message.put("role", "user");message.put("content", "中国大模型行业2025年将会迎来哪些机遇和挑战？");// 构建工具函数对象Map<String, Object> function = new HashMap<>();function.put("description", "<string>");function.put("name", "<string>");function.put("parameters", new HashMap<>());function.put("strict", false);Map<String, Object> tool = new HashMap<>();tool.put("type", "function");tool.put("function", function);// 构建响应格式对象Map<String, Object> responseFormat = new HashMap<>();responseFormat.put("type", "text");// 构建请求参数Map<String, Object> requestBody = new HashMap<>();requestBody.put("model", "tstars2.0");requestBody.put("messages", Arrays.asList(message));requestBody.put("stream", false);requestBody.put("max_tokens", 512);requestBody.put("stop", Arrays.asList("null"));requestBody.put("temperature", 0.7);requestBody.put("top_p", 0.7);requestBody.put("top_k", 50);requestBody.put("frequency_penalty", 0.5);requestBody.put("n", 1);requestBody.put("response_format", responseFormat);requestBody.put("tools", Arrays.asList(tool));// 转换为JSON字符串ObjectMapper mapper = new ObjectMapper();String jsonBody = mapper.writeValueAsString(requestBody);// 发送请求HttpResponse<String> response = Unirest.post("https://apis.iflow.cn/v1/chat/completions")  .header("Authorization", "Bearer <XinLiu API KEY>")  .header("Content-Type", "application/json")  .body(jsonBody)  .asString();

* * *

### 响应参数[​](# "响应参数的直接链接")

*   非流式输出
*   流式输出

参数名

类型

是否必填

默认值

描述

`choices`

`object[]`

是

\-

模型生成的选择列表。

`choices.finish_reason`

`enum<string>`

否

\-

生成结束的原因。 可选值： - `stop`  
: 自然结束。 - `eos`  
: 到达句子结束符。 - `length`  
: 达到最大 token 长度限制。 - `tool_calls`  
: 调用了工具（如函数）。

`choices.message`

`object`

是

\-

模型返回的消息对象。

`created`

`integer`

是

\-

响应生成的时间戳。

`id`

`string`

是

\-

响应的唯一标识符。

`model`

`string`

是

\-

使用的模型名称。

`object`

`enum<string>`

是

\-

响应类型。 可选值： - `chat.completion`  
: 表示这是一个聊天完成响应。

`tool_calls`

`object[]`

否

\-

模型生成的工具调用，例如函数调用。

`tool_calls.function`

`object`

否

\-

模型调用的函数。

`tool_calls.function.arguments`

`string`

否

\-

函数调用的参数，由模型以 JSON 格式生成。 注意：模型生成的 JSON 可能无效，或者可能会生成不属于函数定义的参数。在调用函数前，请在代码中验证这些参数。

`tool_calls.function.name`

`string`

否

\-

要调用的函数名称。

`tool_calls.id`

`string`

否

\-

工具调用的唯一标识符。

`tool_calls.type`

`enum<string>`

否

\-

工具的类型。 目前仅支持 `function`  
。 可选值： - `function`  
: 表示这是一个函数调用。

`usage`

`object`

是

\-

Token 使用情况统计。

`usage.completion_tokens`

`integer`

是

\-

完成部分使用的 token 数量。

`usage.prompt_tokens`

`integer`

是

\-

提示部分使用的 token 数量。

`usage.total_tokens`

`integer`

是

\-

总共使用的 token 数量。

参数名

类型

是否必填

默认值

描述

`id`

`string`

是

\-

聊天补全的唯一标识符。每个分块具有相同的 ID。

`choices`

`object[]`

是

\-

模型生成的选择列表。

`choices.finish_reason`

`enum<string>`

否

\-

生成结束的原因。可选值：`stop`（自然结束）、`eos`（到达句子结束符）、`length`（达到最大 token 长度限制）、`tool_calls`（调用了工具，如函数）。

`choices.message`

`object`

是

\-

模型返回的消息对象。

`created`

`integer`

是

\-

响应生成的时间戳（Unix 时间戳，单位为秒）。

`model`

`string`

是

\-

使用的模型名称。

`object`

`enum<string>`

是

\-

响应类型。可选值：`chat.completion`（表示这是一个聊天完成响应）。

`tool_calls`

`object[]`

否

\-

模型生成的工具调用，例如函数调用。

`tool_calls.function`

`object`

否

\-

模型调用的函数。

`tool_calls.function.arguments`

`string`

否

\-

函数调用的参数，由模型以 JSON 格式生成。注意：模型生成的 JSON 可能无效，或者可能会生成不属于函数定义的参数。在调用函数前，请在代码中验证这些参数。

`tool_calls.function.name`

`string`

否

\-

要调用的函数名称。

`tool_calls.id`

`string`

否

\-

工具调用的唯一标识符。

`tool_calls.type`

`enum<string>`

否

\-

工具的类型。目前仅支持 `function`（表示这是一个函数调用）。

`usage`

`object`

是

\-

Token 使用情况统计。

`usage.completion_tokens`

`integer`

是

\-

完成部分使用的 token 数量。

`usage.prompt_tokens`

`integer`

是

\-

提示部分使用的 token 数量。

`usage.total_tokens`

`integer`

是

\-

总共使用的 token 数量（提示 + 完成）。

`delta`

`object`

否

\-

流式模型响应生成的聊天补全增量。

`choices.logprobs`

`object` 或 `null`

否

\-

该选项的对数概率信息。

`choices.logprobs.content`

`array` 或 `null`

否

\-

包含对数概率信息的消息内容标记列表。

`choices.logprobs.refusal`

`array` 或 `null`

否

\-

包含拒绝消息的标记列表及其对数概率信息。

`choices.logprobs.refusal.token`

`string`

否

\-

标记。

`choices.logprobs.refusal.logprob`

`number`

否

\-

如果该标记在最有可能的 20 个标记内，则为其对数概率；否则使用值 `-9999.0` 表示极不可能。

`choices.logprobs.refusal.bytes`

`array` 或 `null`

否

\-

表示标记的 UTF-8 字节表示的整数列表。用于需要组合多个标记字节表示的情况。

`choices.logprobs.refusal.top_logprobs`

`array`

否

\-

当前标记位置上最有可能的标记及其对数概率列表。在少数情况下，返回的数量可能少于请求的数量。

`finish_reason`

`string` 或 `null`

否

\-

模型停止生成标记的原因。

`index`

`integer`

否

\-

选项在选项列表中的索引。

`service_tier`

`string` 或 `null`

否

\-

用于处理请求的服务层级。

`system_fingerprint`

`string`

否

\-

表示模型运行时后端配置的指纹。可与请求参数 `seed` 结合使用，以了解可能影响确定性的后端更改。

* * *

### 响应信息[​](# "响应信息的直接链接")

*   非流式
*   流式

            {          "id": "<string>",          "choices": [            {              "message": {                "role": "assistant",                "content": "<string>",                "reasoning_content": "<string>"              },              "finish_reason": "stop"            }          ],          "tool_calls": [            {              "id": "<string>",              "type": "function",              "function": {                "name": "<string>",                "arguments": "<string>"              }            }          ],          "usage": {            "prompt_tokens": 123,            "completion_tokens": 123,            "total_tokens": 123          },          "created": 123,          "model": "<string>",          "object": "chat.completion"        }

        {"id":"<string>","object":"chat.completion.chunk","created":1694268190,"model":"<string>", "system_fingerprint": "fp_44709d6fcb", "choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}    {"id":"<string>","object":"chat.completion.chunk","created":1694268190,"model":"<string>", "system_fingerprint": "fp_44709d6fcb", "choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}    ....    {"id":"<string>","object":"chat.completion.chunk","created":1694268190,"model":"<string>", "system_fingerprint": "fp_44709d6fcb", "choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}]}

[

上一页

快速开始

](https://platform.iflow.cn/docs/)[

下一页

错误码

](https://platform.iflow.cn/docs/response-info)