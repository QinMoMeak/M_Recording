本文为您介绍如何使用阿里云智能语音服务提供的Android SDK，包括SDK下载安装、关键接口及代码示例。

## 前提条件

-   使用SDK前，首先阅读接口说明，详情请参见[接口说明](https://help.aliyun.com/zh/isi/developer-reference/sdks-for-mobile-clients#topic-2619206)。
    
-   准备好项目Appkey，详情请参见[创建项目](https://help.aliyun.com/zh/isi/create-a-project#topic-2572188)。
    
-   已获取Access Token，详情请参见[获取Token概述](https://help.aliyun.com/zh/isi/overview-of-obtaining-an-access-token#587dee8029x7r)。
    

## 下载安装

1.  [移动端SDK选择与下载](https://help.aliyun.com/zh/isi/sdk-selection-and-download)。
    
2.  解压ZIP包，在`app/libs`目录下获取AAR格式的SDK包，将AAR包集成到您的工程项目中进行依赖。
    
3.  使用Android Studio打开此工程查看参考代码实现，其中示例代码为FileTranscriberActivity.java文件，替换Appkey和Token后可直接运行。
    

## SDK关键接口

-   **initialize**：初始化SDK。
    
    ```
    /**
     * 初始化SDK，SDK为单例，请先释放后再次进行初始化。请勿在UI线程调用，可能会引起阻塞
     * @param callback: 事件监听回调，参见下文回调说明
     * @param parameters: 初始化参数，参见接口说明
     * @param level: log打印级别，值越小打印越多
     * @return: 参见错误码
     */
    public synchronized int initialize(final INativeFileTransCallback callback,
                                       String parameters,
                                       final Constants.LogLevel level)
    ```
    
    其中，INativeFileTransCallback类型需要实现的回调是onFileTransEventCallback。
    
    **onFileTransEventCallback**：文件识别事件回调。
    
    ```
    /**
         * SDK主要事件回调
         * @param event: 回调事件，参见如下事件列表
         * @param resultCode: 参见错误码，在出现EVENT_ASR_ERROR事件时有效
         * @param arg2: 保留参数
         * @param asrResult: 语音识别结果
         * @param taskId: 转写任务ID
         */
        void onFileTransEventCallback(NuiEvent event, final int resultCode, final int arg2, AsrResult asrResult, String taskId);
    ```
    
    事件列表：
    
    | **名称** | **说明** |
    | --- | --- |
    | EVENT\\_FILE\\_TRANS\\_CONNECTED | 连接文件识别服务成功 |
    | EVENT\\_FILE\\_TRANS\\_UPLOADED | 上传文件成功 |
    | EVENT\\_FILE\\_TRANS\\_RESULT | 识别最终结果 |
    | EVENT\\_ASR\\_ERROR | 根据错误码信息判断出错原因 |
    
-   **setParams**：以JSON格式设置SDK参数。
    
    ```
    /**
     * 以JSON格式设置参数
     * @param params: 参见接口说明
     * @return: 参见错误码
     */
    public synchronized int setParams(String params)
    ```
    
-   **startFileTranscriber**：开始文件识别。
    
    ```
    /**
     * 开始识别
     * @param params: 识别参数，参见接口说明
     * @param taskId: 开始转写的任务ID，SDK生成随机字符串
     * @return: 参见错误码
     */
    public synchronized int startFileTranscriber(String params, byte[] taskId)
    ```
    
-   **stopFileTranscriber**：结束识别。
    
    ```
    /**
     * 结束识别
     * @return: 参见错误码
     */
    public synchronized int stopFileTranscriber(String taskId)
    ```
    
-   **release**：释放SDK。
    
    ```
    /**
     * 释放SDK资源
     * @return: 参见错误码
     */
    public synchronized int release()
    ```
    

## 调用步骤

1.  初始化SDK。
    
2.  根据业务需求设置参数。
    
3.  调用startFileTranscriber开始识别。
    
4.  在EVENT\_FILE\_TRANS\_RESULT事件中获取最终识别结果。
    
5.  结束调用，使用release接口释放SDK资源。
    

## Proguard配置

如果代码使用了混淆，请在proguard-rules.pro中配置：

```
-keep class com.alibaba.idst.nui.*{*;}
```

## 代码示例

**说明**

接口默认采用GetInstance获得单例，您如果有多例需求，也可以直接new对象进行使用。

**NUI SDK初始化**

```
//这里主动调用完成SDK配置文件的拷贝
CommonUtils.copyAssetsData(this);
//获取工作路径
String assets_path = CommonUtils.getModelPath(this);
Log.i(TAG, "use workspace " + assets_path);

int ret = nui_instance.initialize(this, genInitParams(assets_path, debug_path), Constants.LogLevel.LOG_LEVEL_VERBOSE);
```

其中，genInitParams生成为String JSON字符串，包含资源目录和用户信息。其中用户信息包含如下字段。

```
private String genInitParams(String workpath, String debugpath) {
    String str = "";
    try{
        //获取token方式：
        JSONObject object = new JSONObject();

        //账号和项目创建
        //  ak_id ak_secret app_key如何获得,请查看https://help.aliyun.com/document_detail/72138.html
        object.put("app_key", "<您申请创建的app_key>"); // 必填

        //方法1：（强烈推荐）
        //  首先ak_id ak_secret app_key如何获得,请查看https://help.aliyun.com/document_detail/72138.html
        //  然后请看 https://help.aliyun.com/document_detail/466615.html 使用其中方案一获取临时凭证
        //  此方案简介: 远端服务器生成具有有效时限的临时凭证, 下发给移动端进行使用, 保证账号信息ak_id和ak_secret不被泄露
        //  获得Token方法(运行在APP服务端): https://help.aliyun.com/document_detail/450255.html?spm=a2c4g.72153.0.0.79176297EyBj4k
        object.put("token", "<服务器生成的具有时效性的临时凭证>"); // 必填

        //方法2：
        //  STS获取临时凭证方法暂不支持

        //方法3：（强烈不推荐，存在阿里云账号泄露风险）
        //  参考Auth类的实现在端上访问阿里云Token服务获取SDK进行获取。请勿将ak/sk存在本地或端侧环境。
        //  此方法优点: 端侧获得Token, 无需搭建APP服务器。
        //  此方法缺点: 端侧获得ak/sk账号信息, 极易泄露。
//            JSONObject object = Auth.getAliYunTicket();

        object.put("url", "https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/FlashRecognizer"); // 必填

        object.put("device_id", Utils.getDeviceId()); // 必填, 推荐填入具有唯一性的id, 方便定位问题。也可用提供Utils.getDeviceId()
        //工作目录路径，SDK从该路径读取配置文件
        object.put("workspace", workpath); // 必填, 且需要有读写权限
        //debug目录。当初始化SDK时的save_log参数取值为true时，该目录用于保存中间音频文件
        object.put("debug_path", debugpath);

        // FullMix = 0   // 选用此模式开启本地功能并需要进行鉴权注册
        // FullCloud = 1
        // FullLocal = 2 // 选用此模式开启本地功能并需要进行鉴权注册
        // AsrMix = 3    // 选用此模式开启本地功能并需要进行鉴权注册
        // AsrCloud = 4
        // AsrLocal = 5  // 选用此模式开启本地功能并需要进行鉴权注册
        // 这里只能选择FullMix和FullCloud
        object.put("service_mode", Constants.ModeFullCloud); // 必填

        str = object.toString();
    } catch (JSONException e) {
        e.printStackTrace();
    }

    return str;
}
```

**开始识别**

调用startFileTranscriber方法开启识别。

```
byte[] task_id = new byte[32];
NativeNui.GetInstance().startFileTranscriber(genDialogParams(), taskId);

private String genDialogParams() {
    String params = "";
    try {
        JSONObject dialog_param = new JSONObject();
        //若想在运行时切换app_key
        //dialog_param.put("app_key", "");
        dialog_param.put("file_path", "/sdcard/test.wav");

        JSONObject nls_config = new JSONObject();
        nls_config.put("format", "wav");

        dialog_param.put("nls_config", nls_config);
        params = dialog_param.toString();
    } catch (JSONException e) {
        e.printStackTrace();
    }

    Log.i(TAG, "dialog params: " + params);
    return params;
}
```

**回调处理**

onFileTransEventCallback：NUI SDK事件回调，请勿在事件回调中调用SDK的接口，可能引起死锁。

```
public void onFileTransEventCallback(Constants.NuiEvent event, final int resultCode, final int arg2, AsrResult asrResult, String taskId) {
        Log.i(TAG, "event=" + event);
    	if (event == Constants.NuiEvent.EVENT_FILE_TRANS_RESULT) {
            showText(asrView, asrResult.asrResult);
        } else if (event == Constants.NuiEvent.EVENT_ASR_ERROR) {
            ;
        }
    }
```

## 常见问题

### Android SDK调用文件上传转写极速版，调用int startFileTranscriber(String params, byte\[\] task\_id)后无法收到回调，报错提示“EventE/iDST::NativeNui: no java instance, maybe already released”。

您需要核实：

-   资源文件是否复制成功。
    
-   `CommonUtils.copyAssetsData`函数是否已调用。
    

### Android SDK录音文件识别极速版，通过任务ID查询任务状态用哪个API？

不支持通过任务ID查询任务状态，任务中处理安卓端回调就是当前任务的状态。

### Android SDK是否可以上传OPUS音频数据，实现实时语音转文字？

-   录音文件极速版：
    
    支持OPUS格式的音频文件。
    
-   一句话识别和实时语音识别：
    
    仅支持用户输入PCM编码、16bit采样位数、单通道音频数据。支持PCM和OPUS两种音频传输格式（通过参数`sr_format`进行设置），且均为16bit采样位数、单通道数据。若设置为OPUS格式，SDK内部将会自动将用户传入的PCM数据进行编码压缩，以节约网络传输带宽资源。
    

### 调用Android SDK时，手机报错提示“audio recoder not init”如何解决？

您可以通过以下方式排查：

-   检查AudioRecord是否初始化正常。
    
-   检查语音播放器是否有问题。
    
-   系统的录音模块代码如下，也可单独编写AudioRecord录音代码，测试是否正常。![编写AudioRecord录音代码](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/6839880661/p478986.png)

SDK内封装了获取和刷新Token的过程，使用户无需手动处理复杂的认证逻辑和Token有效期管理，简化了开发流程，提升了开发效率，更加安全有效。本文介绍如何通过SDK方式获取Token。

## **背景信息**

| **通过SDK获取Token方式** | **说明** |
| --- | --- |
| 通过智能语音交互SDK获取Token | 适用于通过智能语音交互SDK直接获取Token的场景， 建议您集成此SDK。 |
| 通过阿里云公共SDK获取Token | 适用于当前智能语音交互暂未提供对应语言SDK获取Token的场景。 |

## 前提条件

-   已获取AccessKey ID和AccessKey Secret，具体操作，请参见[从这里开始](https://help.aliyun.com/zh/isi/getting-started/start-here)**。**
    
-   调用接口前，需配置环境变量，通过环境变量读取访问凭证。智能语音交互的AccessKey ID和AccessKey Secret的环境变量名：**ALIYUN\_AK\_ID**和**ALIYUN\_AK\_SECRET**。
    

## 通过智能语音交互SDK获取Token

### **调用示例（Java）**

从Maven服务器下载最新版本SDK，[下载demo源码ZIP包](https://gw.alipayobjects.com/os/bmw-prod/5d824f8b-cf20-4bb9-a22a-74463bff2b36.zip)。

```
<dependency>
    <groupId>com.alibaba.nls</groupId>
    <artifactId>nls-sdk-common</artifactId>
    <version>2.1.6</version>
</dependency>
```

Java代码获取访问令牌Token实现示例如下：

```
AccessToken accessToken = new AccessToken(System.getenv().get("ALIYUN_AK_ID"), System.getenv().get("ALIYUN_AK_SECRET"));
accessToken.apply();
String token = accessToken.getToken();
long expireTime = accessToken.getExpireTime();
```

-   `token`为服务端分配的Token，在Token过期失效前，可以一直使用，也支持在不同机器、进程或应用上同时使用该Token。
    
-   `expireTime`为此令牌的有效期时间戳，单位：秒。例如，1527592757换算为北京时间为2018/5/29 19:19:17，即Token在该时间之前有效，过期需要重新获取。
    

**说明**

当前获取Token需要获取AccessKey ID和AccessKey Secret，为了安全起见，一般不建议在端侧操作，比如在移动端等环境下保存AccessKey ID和AccessKey Secret，建议您在安全可靠的环境中使用AccessKey ID和AccessKey Secret。

如果您的使用场景是移动端APP，可以考虑自行在服务端搭建一个Token生成器的服务，将AccessKey ID和AccessKey Secret放在服务端，APP调用语音识别前，先向您的服务端请求下发Token，之后通过此Token向智能语音服务发起调用。

### **调用示例（C++）**

**说明**

-   Linux下安装工具要求如下：
    
    -   Glibc 2.5及以上
        
    -   Gcc4或Gcc5
        
-   Windows下：VS2013或VS2015，Windows平台需要您自己搭建工程。
    

1.  [下载 C++ Token SDK](https://gw.alipayobjects.com/os/bmw-prod/933cf60b-bdbb-4c62-a67d-f314c47e6952.zip)。
    
2.  编译示例。
    
    假设示例文件已解压至`path/to`路径下，在Linux终端依次执行如下命令编译运行程序。
    
    -   当您的开发环境支持通过CMake编译：
        
        1.  确认本地系统已安装Cmake2.4及以上版本。
            
        2.  切换目录：`cd path/to/sdk/lib`。
            
        3.  解压缩文件：`tar -zxvpf linux.tar.gz`。
            
        4.  切换目录：`cd path/to/sdk`。
            
        5.  执行编译脚本：`./build.sh`。
            
        6.  切换目录：`cd path/to/sdk/demo`。
            
        7.  执行获取Token示例：`./tokenDemo <yourAccessKeySecret> <yourAccessKeyId>`。
            
    -   当您的开发环境不支持通过CMake编译：
        
        1.  切换目录：`cd path/to/sdk/lib`。
            
        2.  解压缩文件：`tar -zxvpf linux.tar.gz`。
            
        3.  切换目录：`cd path/to/sdk/demo`。
            
        4.  使用g++编译命令编译示例程序：`g++ -o tokenDemo tokenDemo.cpp -I path/to/sdk/include/ -L path/to/sdk/lib/linux -ljsoncpp -lssl -lcrypto -lcurl -luuid -lnlsCommonSdk -D_GLIBCXX_USE_CXX11_ABI=0`。
            
        5.  指定库路径：`export LD_LIBRARY_PATH=path/to/sdk/lib/linux/`。
            
        6.  执行获取Token示例：`./tokenDemo <yourAccessKeySecret> <yourAccessKeyId>`。
            
3.  调用服务。
    
    C++获取访问令牌Token的示例代码如下：
    
    ```
    #include <iostream>
    #include "Token.h"
    
    using std::cout;
    using std::endl;
    using namespace AlibabaNlsCommon;
    
    //获取访问令牌TokenId
    int getTokenId(const char* keySecret, const char* keyId) {
        NlsToken nlsTokenRequest;
    
        /*设置阿里云账号KeySecret*/
        nlsTokenRequest.setKeySecret(getenv("ALIYUN_AK_SECRET"));
        /*设置阿里云账号KeyId*/
        nlsTokenRequest.setAccessKeyId(getenv("ALIYUN_AK_ID"));
    
        /*获取Token. 成功返回0, 失败返回-1*/
        if (-1 == nlsTokenRequest.applyNlsToken()) {
            cout << "Failed: " << nlsTokenRequest.getErrorMsg() << endl; /*获取失败原因*/
    
            return -1;
        } else {
            cout << "TokenId: " << nlsTokenRequest.getToken() << endl; /*获取TokenId*/
            cout << "TokenId expireTime: " << nlsTokenRequest.getExpireTime() << endl; /*获取Token有效期时间戳（秒）*/
    
            return 0;
        }
    }
    ```
    

## 通过阿里云公共SDK获取Token

使用阿里云公共SDK获取Token，建议采用RPC风格的API调用。发起一次RPC风格的CommonAPI请求，需要提供以下参数：

| **参数名** | **参数值** | **说明** |
| --- | --- | --- |
| domain | nls-meta.cn-shanghai.aliyuncs.com | 产品的通用访问域名，固定值。 |
| region\\_id | cn-shanghai | 服务的地域ID，固定值。 |
| action | CreateToken | API的名称，固定值。 |
| version | 2019-02-28 | API的版本号，固定值。 |

通过阿里云SDK获取Token时，如果成功调用，则会返回如下报文：

![通过阿里云公共SDK获取Token](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/8506283661/p494131.png)

-   `Id`为本次分配的访问令牌Token，在Token过期失效前，可以一直使用，也支持在不同机器、进程或应用上同时使用该Token。
    
-   `ExpireTime`为此令牌的有效期时间戳，单位：秒。如，1527592757换算为北京时间为2018/5/29 19:19:17，即Token在该时间之前有效，过期需要重新获取。
    

### **调用示例（Java）**

1.  添加Java依赖。
    
    添加阿里云Java SDK的核心库（版本为3.7.1）和fastjson库。
    
    ```
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>aliyun-java-sdk-core</artifactId>
        <version>3.7.1</version>
    </dependency>
    
    <!-- http://mvnrepository.com/artifact/com.alibaba/fastjson -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.83</version>
    </dependency>
    ```
    
2.  调用服务。
    
    获取访问令牌的示例代码如下：
    
    ```
    import com.alibaba.fastjson.JSON;
    import com.alibaba.fastjson.JSONObject;
    import com.aliyuncs.CommonRequest;
    import com.aliyuncs.CommonResponse;
    import com.aliyuncs.DefaultAcsClient;
    import com.aliyuncs.IAcsClient;
    import com.aliyuncs.exceptions.ClientException;
    import com.aliyuncs.http.MethodType;
    import com.aliyuncs.http.ProtocolType;
    import com.aliyuncs.profile.DefaultProfile;
    
    import java.text.SimpleDateFormat;
    import java.util.Date;
    
    public class CreateTokenDemo {
    
        // 地域ID
        private static final String REGIONID = "cn-shanghai";
        // 获取Token服务域名
        private static final String DOMAIN = "nls-meta.cn-shanghai.aliyuncs.com";
        // API版本
        private static final String API_VERSION = "2019-02-28";
        // API名称
        private static final String REQUEST_ACTION = "CreateToken";
    
        // 响应参数
        private static final String KEY_TOKEN = "Token";
        private static final String KEY_ID = "Id";
        private static final String KEY_EXPIRETIME = "ExpireTime";
    
    
        public static void main(String args[]) throws ClientException {
    
            String accessKeyId = System.getenv().get("ALIYUN_AK_ID");
            String accessKeySecret = System.getenv().get("ALIYUN_AK_SECRET");
    
            // 创建DefaultAcsClient实例并初始化
            DefaultProfile profile = DefaultProfile.getProfile(
                    REGIONID,
                    accessKeyId,
                    accessKeySecret);
    
            IAcsClient client = new DefaultAcsClient(profile);
            CommonRequest request = new CommonRequest();
            request.setDomain(DOMAIN);
            request.setVersion(API_VERSION);
            request.setAction(REQUEST_ACTION);
            request.setMethod(MethodType.POST);
            request.setProtocol(ProtocolType.HTTPS);
    
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            if (response.getHttpStatus() == 200) {
                JSONObject result = JSON.parseObject(response.getData());
                String token = result.getJSONObject(KEY_TOKEN).getString(KEY_ID);
                long expireTime = result.getJSONObject(KEY_TOKEN).getLongValue(KEY_EXPIRETIME);
                System.out.println("获取到的Token： " + token + "，有效期时间戳(单位：秒): " + expireTime);
                // 将10位数的时间戳转换为北京时间
                String expireDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(expireTime * 1000));
                System.out.println("Token有效期的北京时间：" + expireDate);
            }
            else {
                System.out.println("获取Token失败！");
            }
        }
    }
    ```
    

### **调用示例（Python）**

使用pip安装SDK。

1.  执行以下命令，通过pip安装Python SDK，版本为2.15.1。
    
    ```
    pip install aliyun-python-sdk-core==2.15.1 # 安装阿里云SDK核心库
    ```
    
2.  调用服务。
    
    示例代码如下：
    
    ```
    #! /usr/bin/env python
    # coding=utf-8
    import os
    import time
    import json
    from aliyunsdkcore.client import AcsClient
    from aliyunsdkcore.request import CommonRequest
    
    # 创建AcsClient实例
    client = AcsClient(
       os.getenv('ALIYUN_AK_ID'),
       os.getenv('ALIYUN_AK_SECRET'),
       "cn-shanghai"
    )
    
    # 创建request，并设置参数。
    request = CommonRequest()
    request.set_method('POST')
    request.set_domain('nls-meta.cn-shanghai.aliyuncs.com')
    request.set_version('2019-02-28')
    request.set_action_name('CreateToken')
    
    try: 
       response = client.do_action_with_exception(request)
       print(response)
    
       jss = json.loads(response)
       if 'Token' in jss and 'Id' in jss['Token']:
          token = jss['Token']['Id']
          expireTime = jss['Token']['ExpireTime']
          print("token = " + token)
          print("expireTime = " + str(expireTime))
    except Exception as e:
       print(e)
    ```
    

### **调用示例（GO）**

开发前请先安装[alibaba-cloud-sdk-go](https://github.com/aliyun/alibaba-cloud-sdk-go)，具体操作请参见[安装操作](https://github.com/aliyun/alibaba-cloud-sdk-go/blob/master/README-CN.md)。

示例代码如下：

```
package main
import (
    "fmt"
    "encoding/json"
    "github.com/aliyun/alibaba-cloud-sdk-go/sdk"
    "github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
)

type TokenResult struct {
    ErrMsg string
    Token struct {
        UserId      string
        Id          string
        ExpireTime  int64
    }
  }

func main() {
    config := sdk.NewConfig()
    credential := credentials.NewAccessKeyCredential(os.Getenv("ALIYUN_AK_ID"), os.Getenv("ALIYUN_AK_SECRET"))
    client, err := sdk.NewClientWithOptions("cn-shanghai", config, credential)
    if err != nil {
        panic(err)
    }
    request := requests.NewCommonRequest()
    request.Method = "POST"
    request.Domain = "nls-meta.cn-shanghai.aliyuncs.com"
    request.ApiName = "CreateToken"
    request.Version = "2019-02-28"
    response, err := client.ProcessCommonRequest(request)
    if err != nil {
        panic(err)
    }
    fmt.Print(response.GetHttpStatus())
    fmt.Print(response.GetHttpContentString())

    var tr TokenResult
    err = json.Unmarshal([]byte(response.GetHttpContentString()), &tr)
    if err == nil {
        fmt.Println(tr.Token.Id)
        fmt.Println(tr.Token.ExpireTime)
    } else {
        fmt.Println(err)
    }
}
```

### **调用示例（PHP）**

安装前请确保环境使用的是PHP 7.2及以上版本，PHP SDK安装方式请参见[安装Alibaba Cloud SDK for PHP](https://github.com/aliyun/openapi-sdk-php/blob/master/docs/zh-CN/1-Installation.md) 。

1.  创建一个全局客户端。
    
2.  创建API请求，设置参数。
    
3.  发起请求，处理应答或异常。
    

PHP代码获取访问令牌Token代码示例如下：

```
<?php
require __DIR__ . '/vendor/autoload.php';
use AlibabaCloud\Client\AlibabaCloud;
use AlibabaCloud\Client\Exception\ClientException;
use AlibabaCloud\Client\Exception\ServerException;
/**
 * 第一步：设置一个全局客户端
 * 使用阿里云RAM账号的AccessKey ID和AccessKey Secret进行鉴权。
 */
AlibabaCloud::accessKeyClient(
            getenv('ALIYUN_AK_ID'),
            getenv('ALIYUN_AK_SECRET'))
            ->regionId("cn-shanghai")
            ->asDefaultClient();
try {
    $response = AlibabaCloud::nlsCloudMeta()
                            ->v20180518()
                            ->createToken()
                            ->request();
    print $response . "\n";
    $token = $response["Token"];
    if ($token != NULL) {
        print "Token 获取成功：\n";
        print_r($token);
        print "获取到的token是：" . $token["Id"] ."\n";
        date_default_timezone_set('Asia/Shanghai');
        print "获取到的token过期时间是：" . $token["ExpireTime"] . "\n";
    }
    else {
        print "token 获取失败\n";
    }
} catch (ClientException $exception) {
    // 获取错误消息
    print_r($exception->getErrorMessage());
} catch (ServerException $exception) {
    // 获取错误消息
    print_r($exception->getErrorMessage());
}
```

**说明**

上述示例代码是基于阿里云新版PHP SDK进行示例，请参见[Alibaba Cloud SDK for PHP](https://github.com/aliyun/openapi-sdk-php)。如果您已接入阿里云旧版PHP SDK，参见[aliyun-openapi-php-sdk](https://github.com/aliyun/aliyun-openapi-php-sdk.git)，仍然可以继续使用或者更新到新版SDK。

### **调用示例（Node.js）**

1.  安装Node.js SDK。
    
    建议使用npm完成Node.js依赖模块的安装，所有阿里官方的Node.js SDK都位于@alicloud下。
    
    假设Node.js SDK下载后的路径为`/path/to/aliyun-openapi-Node.js-sdk`。当基于SDK核心库进行开发时，请执行以下命令安装@alicloud/pop-core模块。
    
    命令中的`--save`会将模块写入应用的package.json文件中，作为依赖模块。
    
    ```
     npm install @alicloud/pop-core --save
    ```
    
    **说明**
    
    您也可以从GitHub上下载SDK，请参见[GitHub下载SDK](https://github.com/aliyun/aliyun-openapi-nodejs-sdk)。
    
2.  调用服务。
    
    示例代码如下：
    
    ```
    var RPCClient = require('@alicloud/pop-core').RPCClient;
    
    var client = new RPCClient({
      accessKeyId: process.env.ALIYUN_AK_ID,
      accessKeySecret: process.env.ALIYUN_AK_SECRET,
      endpoint: 'http://nls-meta.cn-shanghai.aliyuncs.com',
      apiVersion: '2019-02-28'
    });
    
    // => returns Promise
    // => request(Action, params, options)
    client.request('CreateToken').then((result) => {
        console.log(result.Token)
        console.log("token = " + result.Token.Id)
        console.log("expireTime = " + result.Token.ExpireTime)
    });
    ```
    

## **常见问题**

在获取Token过程中，可能会出现各种异常问题，整体的排查思路如下：

1.  您可以根据错误码以及错误信息在阿里云官方的[错误代码表](https://help.aliyun.com/zh/api-gateway/error-codes#topic20867)中找到对应错误含义及解决方案。例如，使用一个不存在的AccessKeyId调用服务获取Token时，可能返回如下结果：
    
    ```
    {
      "RequestId":"F5805076-38C5-5093-B5BD-9176ECD9CBBD",
      "Message":"Specified access key is not found.",
      "Recommend":"https://next.api.aliyun.com/troubleshoot?q=InvalidAccessKeyId.NotFound&amp;product=nls-cloud-meta",
      "HostId":"nls-meta.cn-shanghai.aliyuncs.com",
      "Code":"InvalidAccessKeyId.NotFound"
    }
    ```
    
    在[错误代码表](https://help.aliyun.com/zh/api-gateway/error-codes#topic20867)搜索该段代码中错误信息的关键字，例如，**InvalidAccessKeyId.NotFound**和**Specified access key is not found**，可以获取相关错误码详情。![错误码](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/8506283661/p494241.png)
    
2.  您也可以在[阿里云OpenAPI诊断中心](https://next.api.aliyun.com/troubleshoot)页面输入错误信息，查找解决方案。
    
    ![api诊断](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/8506283661/p494238.png)
    

### **获取Token时出现“Not supported proxy scheme”报错如何处理？**

如果出现“Not supported proxy scheme”报错，建议您检查代理配置是否正确，例如可尝试修改或删除http\_proxy和https\_proxy环境变量并重试。如果您在其他地方设置了代理，请确保代理地址和端口正确，并且代理服务器可用。

## **相关文档**

除了SDK方式获取Token，您还可以通过以下方式获取Token：

-   [通过控制台获取Token](https://help.aliyun.com/zh/isi/getting-started/obtain-an-access-token-in-the-console)
    
-   [通过OpenAPI获取Token](https://help.aliyun.com/zh/isi/getting-started/use-http-or-https-to-obtain-an-access-token)
本文为您介绍传入录音文件，完成音频文件识别并返回结果的流程说明。

## 使用须知

-   输入格式：WAV/MP3/AAC。
    
-   时长限制：识别语音文件大小不能超过100 MB。
    
-   设置多语言识别：在管控台编辑项目中进行模型选择，详情请参见[管理项目](https://help.aliyun.com/zh/isi/getting-started/manage-projects#topic-2572199)。
    

## 服务地址

| 访问类型 | 说明  | URL | Host |
| 外网访问 | 所有服务器均可使用外网访问URL。 | - 上海：`https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/FlashRecognizer` - 北京：`https://nls-gateway-cn-beijing.aliyuncs.com/stream/v1/FlashRecognizer` - 深圳：`https://nls-gateway-cn-shenzhen.aliyuncs.com/stream/v1/FlashRecognizer` | - 上海：`nls-gateway-cn-shanghai.aliyuncs.com` - 北京：`nls-gateway-cn-beijing.aliyuncs.com` - 深圳：`nls-gateway-cn-shenzhen.aliyuncs.com` |
| 阿里云上海ECS内网访问 | 使用阿里云上海、北京、深圳ECS（即ECS地域为华东2（上海）、华北2（北京）、华南1（深圳）），可使用内网访问URL。 ECS的经典网络不能访问AnyTunnel，即不能在内网访问语音服务；如果希望使用AnyTunnel，需要创建专有网络在其内部访问。 **说明** - 使用内网访问方式，将不会产生ECS实例的公网流量费用。 - 关于ECS的网络类型请参见[网络类型](https://help.aliyun.com/zh/ecs/user-guide/network-types#concept-nfj-dz2-5db)。 | - 上海：`http://nls-gateway-cn-shanghai-internal.aliyuncs.com/stream/v1/FlashRecognizer` - 北京：`http://nls-gateway-cn-beijing-internal.aliyuncs.com/stream/v1/FlashRecognizer` - 深圳：`http://nls-gateway-cn-shenzhen-internal.aliyuncs.com/stream/v1/FlashRecognizer` | - 上海：`nls-gateway-cn-shanghai-internal.aliyuncs.com` - 北京：`nls-gateway-cn-beijing-internal.aliyuncs.com` - 深圳：`nls-gateway-cn-shenzhen-internal.aliyuncs.com` |

**说明**

以下将以使用外网访问URL的方式进行介绍。如果您使用阿里云上海ECS，并需要通过内网访问URL，则使用HTTP协议，并替换外网访问的URL和Host。

## 交互流程

下图展示iOS SDK和Android SDK的交互流程。

![image](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/0867463071/5954d13065v7u.svg)

1.  鉴权和初始化
    
    客户端与服务端建立连接时，使用Access Token进行鉴权。关于Token获取请参见[获取Token概述](https://help.aliyun.com/zh/isi/overview-of-obtaining-an-access-token#587dee8029x7r)。
    
    初始化参数表
    
    | 参数  | 类型  | 是否必选 | 说明  |
    | --- | --- | --- | --- |
    | url | String | 是   | 语音服务URL地址。 |
    | app\\_key | String | 是   | [管控台](https://nls-portal.console.aliyun.com/applist)创建项目的appkey。 |
    | token | String | 是   | 请确保该Token可以使用并在有效期内。 **说明** Token可以在初始化时设置，也可通过参数设置进行更新。 |
    | device\\_id | String | 是   | 设备标识，唯一表示一台设备（如Mac地址/SN/UniquePsuedoID）。 |
    
2.  开始文件识别
    
    客户端发起文件识别请求时，需要通过接口传入JSON格式的params参数。
    
    | 参数  | 类型  | 是否必选 | 说明  |
    | --- | --- | --- | --- |
    | file\\_path | String | 是   | 需要进行识别的文件绝对路径。 |
    | direct\\_ip | String | 否   | 您可自行完成服务地址DNS解析，传入IP地址进行访问。 |
    | nls\\_config | JsonObject | 否   | 服务细节参数配置。 |
    
    其中，`nls_config`字段用于设置服务的具体配置参数，多与识别效果相关。
    
    | 参数  | 类型  | 是否必选 | 说明  |
    | --- | --- | --- | --- |
    | format | String | 是   | 音频编码格式，支持WAV/MP3/AAC格式。 |
    | sample\\_rate | Integer | 否   | 识别模型使用的采样率，一般不设置，在管控台进行配置。 |
    | enable\\_inverse\\_text\\_normalization | Boolean | 否   | ITN（逆文本inverse text normalization）中文数字转换阿拉伯数字。设置为True时，中文数字将转为阿拉伯数字输出，默认值：False。 |
    | max\\_end\\_silence | Integer | 否   | 允许的最大结束静音，默认值450，单位是毫秒。 |
    | customization\\_id | String | 否   | 通过POP API创建的定制模型id，默认不添加。 |
    | vocabulary\\_id | String | 否   | 创建的泛热词表ID，默认不添加。 |
    | enable\\_word\\_level\\_result | Boolean | 否   | 是否开启词信息返回，默认false。 |
    | first\\_channel\\_only | Boolean | 否   | 是否开启只处理第一个声道，默认false。 |
    
3.  获取识别结果
    
    启动任务后一段时间（根据文件长度和网络情况而定）将收到EVENT\_FILE\_TRANS\_RESULT事件，即完整转写结果，示例如下。
    
    ```
    {
        "task_id":"e76f979b33d9443eb6fbf770315a****",
        "status":20000000,
        "message":"SUCCESS",
        "flash_result":{
            "duration":299,  //音频时长
            "completed":true, 
            "sentences":[
                {
                    "text":"啊，",  //句子级别的识别结果
                    "begin_time":3700,  //句子的开始时间，单位：毫秒
                    "end_time":3940,  //句子的结束时间，单位：毫秒
                    "channel_id":0,  //多个声道的音频文件会区分返回识别结果，声道id从0计数
                    "words":[
                        {
                            "text":"啊",  //当前句子包含的词信息
                            "begin_time":3700,  //当前词开始时间，单位：毫秒
                            "end_time":3940,  //当前词结束时间，单位：毫秒
                            "punc":"，"  //当前词尾的标点信息，没有标点则为空
                        }
                    ]
                },
                {
                    "text":"我在哪呀了。",
                    "begin_time":3940,
                    "end_time":4720,
                    "channel_id":0,
                    "words":[
                        {
                            "text":"我",
                            "begin_time":3940, 
                            "end_time":4040,
                            "punc":"，"
                        },
                        {
                            "text":"在哪",
                            "begin_time":4040,
                            "end_time":4340,
                            "punc":"，"
                        },
                        {
                            "text":"呀了",
                            "begin_time":4340,
                            "end_time":4720,
                            "punc":"，"
                        }
                    ]
                }
            ]
        }
    }
    ```
    
    响应消息为JSON格式，包含如下字段。
    
    | 字段  | 说明  |
    | --- | --- |
    | task\\_id | 任务ID，任务唯一标识。 |
    | status | 状态码 |
    | message | 状态消息 |
    | flash\\_result | 识别结果对象 |
    
    其中，`flash_result`对象包括如下字段。
    
    | 字段  | 说明  |
    | --- | --- |
    | sentences | 句子级别的识别结果，是一个对象数组。 |
    
    其中，`sentences`数组对象包含如下字段。
    
    | 字段  | 说明  |
    | --- | --- |
    | text | 识别结果。 |
    | begin\\_time | 识别结果的语音起点在音频流的时间偏移，单位毫秒。 |
    | end\\_time | 识别结果的语音尾点在音频流的时间偏移，单位毫秒。 |
    

## 错误码

### **通用错误码**

| **状态码** | **状态消息** | **原因** | **解决方案** |
| --- | --- | --- | --- |
| 40000000 | 默认的客户端错误码，对应了多个错误消息。 | 用户使用了不合理的参数或者调用逻辑。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000001 | The token 'xxx' has expired； The token 'xxx' is invalid | 用户使用了不合理的参数或者调用逻辑。通用客户端错误码，通常是涉及Token相关的不正确使用，例如Token过期或者非法。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000002 | Gateway:MESSAGE\\_INVALID:Can't process message in state'FAILED'! | 无效或者错误的报文消息。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000003 | PARAMETER\\_INVALID; Failed to decode url params | 用户传递的参数有误，一般常见于RESTful接口调用。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000005 | Gateway:TOO\\_MANY\\_REQUESTS:Too many requests! | 并发请求过多。 | 如果是试用版调用，建议您升级为商用版本以增大并发。 如果已是商用版，可购买并发资源包，扩充您的并发额度。 |
| 40000009 | Invalid wav header! | 错误的消息头。 | 如果您发送的是WAV语音文件，且设置`format`为`wav`，请注意检查该语音文件的WAV头是否正确，否则可能会被服务端拒绝。 |
| 40000009 | Too large wav header! | 传输的语音WAV头不合法。 | 建议使用PCM、OPUS等格式发送音频流，如果是WAV，建议关注语音文件的WAV头信息是否为正确的数据长度大小。 |
| 40000010 | Gateway:FREE\\_TRIAL\\_EXPIRED:The free trial has expired! | 试用期已结束，并且未开通商用版、或账号欠费。 | 请登录控制台确认服务开通状态以及账户余额。 |
| 40010001 | Gateway:NAMESPACE\\_NOT\\_FOUND:RESTful url path illegal | 不支持的接口或参数。 | 请检查调用时传递的参数内容是否和官网文档要求的一致，并结合错误信息对比排查，设置为正确的参数。 比如您是否通过curl命令执行RESTful接口请求， 拼接的URL是否合法。 |
| 40010003 | Gateway:DIRECTIVE\\_INVALID:\\[xxx\\] | 客户端侧通用错误码。 | 表示客户端传递了不正确的参数或指令，在不同的接口上有对应的详细报错信息，请参考对应文档进行正确设置。 |
| 40010004 | Gateway:CLIENT\\_DISCONNECT:Client disconnected before task finished! | 在请求处理完成前客户端主动结束。 | 无，或者请在服务端响应完成后再关闭链接。 |
| 40010005 | Gateway:TASK\\_STATE\\_ERROR:Got stop directive while task is stopping! | 客户端发送了当前不支持的消息指令。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40020105 | Meta:APPKEY\\_NOT\\_EXIST:Appkey not exist! | 使用了不存在的Appkey。 | 请确认是否使用了不存在的Appkey，Appkey可以通过登录控制台后查看项目配置。 |
| 40020106 | Meta:APPKEY\\_UID\\_MISMATCH:Appkey and user mismatch! | 调用时传递的Appkey和Token并非同一个账号UID所创建，导致不匹配。 | 请检查是否存在两个账号混用的情况，避免使用账号A名下的Appkey和账号B名下生成的Token搭配使用。 |
| 403 | Forbidden | 使用的Token无效，例如Token不存在或者已过期。 | 请设置正确的Token。Token存在有效期限制，请及时在过期前获取新的Token。 |
| 41000003 | MetaInfo doesn't have end point info | 无法获取该Appkey的路由信息。 | 请检查是否存在两个账号混用的情况，避免使用账号A名下的Appkey和账号B名下生成的Token搭配使用。 |
| 41010101 | UNSUPPORTED\\_SAMPLE\\_RATE | 不支持的采样率格式。 | 当前实时语音识别只支持8000 Hz和16000 Hz两种采样率格式的音频。 |
| 41040201 | Realtime:GET\\_CLIENT\\_DATA\\_TIMEOUT:Client data does not send continuously! | 获取客户端发送的数据超时失败。 | 客户端在调用实时语音识别时请保持实时速率发送，发送完成后及时关闭链接。 |
| 50000000 | GRPC\\_ERROR:Grpc error! | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复。 |
| 50000001 | GRPC\\_ERROR:Grpc error! | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复。 |
| 52010001 | GRPC\\_ERROR:Grpc error! | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复。 |

### **一句话识别错误码**

| **状态码** | **状态消息** | **原因** | **解决方案** |
| --- | --- | --- | --- |
| 40000000 | Gateway:CLIENT\\_ERROR:Empty audio data! | 没有音频数据。 | 建议参考公共云示例代码，请求时发送音频数据。 |
| 40000004 | Gateway:IDLE\\_TIMEOUT:Websocket session is idle for too long time | 请求建立链接后，长时间没有发送任何数据，超过10s后服务端会返回此错误信息。 | 请在建立链接后和服务端保持交互，比如持续发送语音流，您可以在采集音频的同时进行发送， 发送结束后及时关闭链接。 |
| 40010002 | Gateway:DIRECTIVE\\_NOT\\_SUPPORTED:Directive'SpeechRecognizer.EnhanceRecognition'isnotsupported! | 发送了服务端不支持的消息指令。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40010003 | Gateway:DIRECTIVE\\_INVALID:Too many items for ‘vocabulary'!(173) | 热词数量设置过多。 | 请参考API进行正确设置。 |
| 41010104 | TOO\\_LONG\\_SPEECH | 发送的语音时长超过限制，仅在一句话识别接口上出现。 | 一句话语音识别支持60s以内的音频，如果超过60s，建议调用实时语音识别接口。 |
| 41010105 | SILENT\\_SPEECH | 纯静音数据或噪音数据，导致无法检测出任何有效语音。 | 无。  |

### 一句话识别/实时语音识别/录音文件识别极速版

-   配置或参数错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240999 | DEFAULT\\_ERROR | 内部默认错误。 | 内部未明确错误。 |
    | 240001 | NUI\\_CONFIG\\_INVALID | 配置文件错误。 | 配置文件错误，请确认传入的资源路径内是否有资源文件。如果是Android平台，请参考代码样例主动使用copyAssets接口。 |
    | 240002 | ILLEGAL\\_PARAM | 非法参数。 | 请确认传入的格式是否正确，包括字段类型、值范围限制。 |
    | 240003 | ILLEGAL\\_INIT\\_PARAM | 初始化参数非法。 | 请确认初始化参数格式是否错误或缺少必须字段。 |
    | 240004 | NECESSARY\\_PARAM\\_LACK | 缺少必须参数。 | 请确认接口调用时的必须参数。 |
    | 240005 | NULL\\_PARAM\\_ERROR | 参数为空。 | 确认参数是否为空。 |
    | 240006 | NULL\\_LISTENER\\_ERROR | 未定义事件回调。 | 确认回调事件是否正确赋值。 |
    | 240007 | NULL\\_DIALOG\\_ERROR | 无有效对话实例，一般在内部状态错误时发生。 | 请确认接口调用前是否为正确状态，可使用cancel接口恢复idle状态。 |
    | 240008 | NULL\\_ENGINE\\_ERROR | 无有效引擎实例，请检查是否初始化成功。 | 请确认是否初始化成功。 |
    | 240009 | ILLEGAL\\_DATA | 传入音频数据地址或长度非法。 | 请确认传入的数据长度值。 |
    
-   SDK状态错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240010 | ILLEGAL\\_REENTRANT | 退出后调用SDK接口。 | 不影响功能时可忽略。 |
    | 240011 | SDK\\_NOT\\_INIT | SDK未正确初始化。 | 确认初始化返回值正确再进行其他接口使用。 |
    | 240012 | SDK\\_ALREADY\\_INIT | 重复调用SDK初始化接口。 | 确认初始化调用逻辑。 |
    | 240013 | DIALOG\\_INVALID\\_STATE | 内部对话状态错误。 | 请阅读SDK流程图，确认是否在错误状态下调用接口。 |
    | 240014 | STATE\\_INVALID | SDK内部状态错误。 | 请阅读SDK流程图，确认是否在错误状态下调用接口。 |
    | 240015 | ILLEGAL\\_FUNC\\_CALL | 该模式无法调用接口。 | 请确认接口调用是否合理。 |
    
-   系统调用错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240020 | MEM\\_ALLOC\\_ERROR | 内存分配错误。 | 检查内存是否不足。 |
    | 240021 | FILE\\_ACCESS\\_FAIL | 文件访问错误。 | 检查文件是否提供读写权限。 |
    | 240022 | CREATE\\_DIR\\_ERROR | 创建目录错误。 | 检查是否有写权限。 |
    
-   SDK内部调用错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240030 | CREATE\\_NUI\\_ERROR | 引擎创建失败。 | 创建实例失败，一般为系统资源不足。 |
    | 240031 | TEXT\\_DIALOG\\_START\\_FAIL | 发起文本理解失败。 | 文本转语义理解失败，检查网络连接或URL以及Token等信息是否有效。 |
    | 240032 | TEXT\\_CANCEL\\_START\\_FAIL | 取消文本理解失败。 | 可忽略。 |
    | 240033 | WUW\\_DUPLICATE | 动态唤醒词重复。 | 可忽略。 |
    
-   本地引擎调用错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240040 | CEI\\_INIT\\_FAIL | 本地引擎初始化失败。 | 请确认本地引擎的模型是否有效、目录是否可读写。 |
    
-   音频错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240051 | UPDATE\\_AUDIO\\_ERROR | 推送音频错误，一般为输入音频长度大于所需音频。 | 确认推送的音频长度是否非法。 |
    | 240052 | MIC\\_ERROR | 连续2s未获取到音频。 | 请确认在音频数据回调中是否正确提供所需长度的音频。 |
    
-   网络错误
    
    | **状态码** | **状态消息** | **原因** | **解决方案** |
    | --- | --- | --- | --- |
    | 240060 | CREATE\\_DA\\_REQUEST\\_ERROR | 创建对话助手实例失败 | 可忽略。 |
    | 240061 | START\\_DA\\_REQUEST\\_ERROR | 发起对话助手请求失败 | 可忽略。 |
    | 240062 | DEFAULT\\_NLS\\_ERROR | 服务端发生错误。 **说明** 该错误同时包含服务端返回错误内容。 | 请参考[服务端错误码](https://help.aliyun.com/zh/isi/support/error-codes)进一步定位。 |
    | 240063 | SSL\\_ERROR | 创建SSL实例错误。 | 偶现请忽略。 |
    | 240064 | SSL\\_CONNECT\\_FAILED | SSL连接失败。 | 连接异常，请检查服务URL或者本地网络连接是否正常。 |
    | 240065 | HTTP\\_CONNECT\\_FAILED | HTTP连接失败。 | 服务连接错误，可通过日志文件查看HTTP返回值确认原因。 |
    | 240066 | DNS\\_FAILED | DNS解析失败。 | 请检查本地网络是否正常、DNS服务是否正常。 |
    | 240067 | CONNECT\\_FAILED | Socket连接失败。 | 检查网络连接。 |
    | 240068 | SERVER\\_NOT\\_ACCESS | 服务端无法访问。 | 请检查Token是否过期或者URL是否正确。 |
    | 240069 | SOCKET\\_CLOSED | Socket已关闭。 | 偶现请忽略。 |
    | 240070 | AUTH\\_FAILED | 鉴权失败。 | 请检查是否提供正确的ak\\_secret，ak\\_id，app\\_key，sdk\\_code和device\\_id等信息，以及确认是否开通足够配额。 |
    | 240071 | HTTPDNS\\_FAILED | 使用客户端传入的IP连接失败。 | 如果使用直接传入IP进行访问，请确认IP是否可访问。 |
    | 240072 | HTTP\\_SEND\\_FAILED | 文件转写HTTP发送失败。 | 确认网络连接是否正常。 |
    | 240073 | HTTP\\_RECEIVE\\_FAILED | 文件转写HTTP接收失败。 | 确认网络连接是否正常。 |
    | 240074 | HTTP\\_RESPONSE\\_ERROR | 文件转写接收内容解析失败 | 服务端返回内容错误。 |
    | 240075 | HTTP\\_SERVER\\_ERROR | 文件转写服务错误。 | 请参考[服务端错误码](https://help.aliyun.com/zh/isi/support/error-codes)进一步定位。 |

    录音文件识别是针对已经录制完成的录音文件，进行离线识别的服务。录音文件识别是非实时的，识别的文件需要提交基于HTTP可访问的URL地址，不支持提交本地文件。

## **计费和并发限制**

-   录音文件识别提供试用版和商用版两种计费模式，详情请参见[试用版和商用版](https://help.aliyun.com/zh/isi/product-overview/pricing#40ba8a7127hw1)。如果您需要将试用版升级为商用版，请参见[试用版升级为商用版](https://help.aliyun.com/zh/isi/product-overview/billing-10#41bb3ea20c3v4)。
    
-   计费方式详情请参见[计费方式](https://help.aliyun.com/zh/isi/product-overview/billing-10)。
    
-   并发限制请参见[并发和QPS说明](https://help.aliyun.com/zh/isi/product-overview/faq-about-concurrency-and-monitoring)。
    

## 使用限制

请在编码时严格遵循以下要求，否则可能导致识别失败（识别结果为空）。

-   支持单轨和双轨的WAV、MP3、MP4、M4A、WMA、AAC、OGG、AMR、FLAC格式录音文件识别。
    
-   音频文件大小不超过512 MB，视频文件大小不超过2 GB，文件总时长不超过12小时。
    
-   需要识别的录音文件必须存放在某服务上，可以通过URL访问。
    
    -   推荐使用阿里云OSS：如果OSS中文件访问权限为公开，可参见[公共读Object](https://help.aliyun.com/zh/oss/user-guide/how-to-obtain-the-url-of-a-single-object-or-the-urls-of-multiple-objects#concept-39607-zh/section-st4-efq-v5j)，获取文件访问链接；如果OSS中文件访问权限为私有，可参见[私有Object](https://help.aliyun.com/zh/oss/user-guide/how-to-obtain-the-url-of-a-single-object-or-the-urls-of-multiple-objects#concept-39607-zh/section-wnz-0he-q20)，通过SDK生成有有效时间的访问链接。
        
    -   您也可以把录音文件存放在自行搭建的文件服务器，提供文件下载。请保证HTTP的响应头（Header）中Content-Length的长度值和Body中数据的真实长度一致，否则会导致下载失败。
        
-   上传的录音文件URL的访问权限需要设置为公开，URL中只能使用域名不能使用IP地址、不可包含空格，请尽量避免使用中文。
    
    | 可用URL | 不可用URL |
    | https://gw.alipayobjects.com/os/bmw-prod/0574ee2e-f494-45a5-820f-63aee583045a.wav | - http://127.0.0.1/sample.wav - D:\\\\files\\\\sample.wav |
    
-   录音文件识别属于离线识别服务，对于并发数没有限制，对于QPS（Queries Per Second）的限制如下：
    
    -   POST方式的录音文件识别请求调用接口，用户级别QPS限制为200。
        
    -   GET方式的录音文件识别请求调用接口，用户级别QPS限制为500。
        
    -   录音文件识别结果查询接口，同一Taskid QPS限制为1。
        
-   新用户试用期3个月内，每隔24小时可免费识别2小时时长的文件转写服务。免费额度用完后，间隔24小时后可继续试用。
    
-   提交录音文件识别请求后，免费用户的识别任务在24小时内完成并返回识别文本。 付费用户的识别任务在3小时内完成并返回识别文本。识别结果在服务端可保存72小时。
    
    **重要**
    
    一次性上传大规模数据（半小时内上传超过500小时时长的录音）的除外。有大规模数据识别需求的用户，请联系售前专家。
    
-   支持调用方式：轮询方式和回调方式。
    
-   支持语言模型定制。更多信息请参见[语言模型定制](https://help.aliyun.com/zh/isi/developer-reference/overview-2#topic-2644771)。
    
-   支持热词。更多信息请参见[热词](https://help.aliyun.com/zh/isi/developer-reference/overview-1#topic-2572262)。
    
-   支持汉语普通话、方言、欧美英语等多种模型识别。语种和方言模型无法在编码时指定，需要在智能语音交互控制台的[全部项目](https://nls-portal.console.aliyun.com/applist)中对相关项目执行**项目功能配置**操作，选择对应的模型。详情请参见[管理项目](https://help.aliyun.com/zh/isi/getting-started/manage-projects#topic-2572199)。
    
    目前支持的语种和方言模型如下：
    
    -   语种
        
        | 语言  | 模型名称 | 采样率 | 标点  | ITN | 顺滑  | 语义断句 | 声音和文本对齐 |
        | --- | --- | --- | --- | --- | --- | --- | --- |
        | 英语  | 通用-英文，教育直播-英文，教育内容分析-英文 | 16k | 支持  | 支持  | 支持  | 不支持 | 支持  |
        | 电话客服（通用） | 8k  | 支持  | 支持  | 支持  | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 日语  | 通用-日语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 支持  |
        | 西班牙语 | 通用-西班牙语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 通用-西班牙客服通用 | 8k  | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 阿拉伯语 | 通用-阿拉伯语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 哈萨克语 | 通用-哈萨克语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 韩语  | 通用-韩语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 泰语  | 通用-泰语 | 16k | 不支持 | 不支持 | 不支持 | 不支持 | 不支持 |
        | 通用-泰语客服通用 | 8k  | 不支持 | 不支持 | 不支持 | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 印尼语 | 通用-印尼语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 电话客服（通用） | 8k  | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 俄语  | 通用-俄语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 越南语 | 通用-越南语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 通用-越南语客服通用 | 8k  | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 法语  | 通用-法语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 德语  | 通用-德语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 意大利语 | 通用-意大利语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 印地语 | 通用-印地语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 马来语 | 通用-马来语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 通用-马来语客服通用 | 8k  | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 菲律宾语 | 通用-菲律宾语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 电话客服（通用） | 8k  | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 泰米尔语 | 通用-泰米尔语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 葡萄牙语 | 通用-葡萄牙语 | 16k | 支持  | 支持  | 不支持 | 不支持 | 不支持 |
        | 土耳其语 | 通用-土耳其语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 波兰语 | 通用-波兰语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 乌克兰语 | 通用-乌克兰语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 罗马尼亚语 | 通用-罗马尼亚语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 荷兰语 | 通用-荷兰语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 希腊语 | 通用-希腊语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 匈牙利语 | 通用-匈牙利语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 爪哇语 | 通用-爪哇语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 孟加拉语 | 通用-孟加拉语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 缅甸语 | 通用-缅甸语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 老挝语 | 通用-老挝语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 斯瓦希里语 | 通用-斯瓦希里语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 阿塞拜疆语 | 通用-阿塞拜疆语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 波斯语 | 通用-波斯语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 僧伽罗语 | 通用-僧伽罗语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 加泰罗尼亚语 | 通用-加泰罗尼亚语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 高棉语 | 通用-高棉语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 希伯来语 | 通用-希伯来语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 克罗地亚语 | 通用-克罗地亚语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 豪萨语 | 通用-豪萨语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 马拉地语 | 通用-马拉地语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 泰卢固语 | 通用-泰卢固语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 旁遮普语 | 通用-旁遮普语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 瑞典语 | 通用-瑞典语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 保加利亚语 | 通用-保加利亚语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 丹麦语 | 通用-丹麦语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 挪威语 | 通用-挪威语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 坎纳达语 | 通用-坎纳达语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 马拉雅拉姆语 | 通用-马拉雅拉姆语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 捷克语 | 通用-捷克语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 乌尔都语 | 通用-乌尔都语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 尼泊尔语 | 通用-尼泊尔语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 蒙古语（外蒙） | 通用-蒙古语（外蒙） | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 乌兹别克语 | 通用-乌兹别克语 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        
    -   方言
        
        | 语言  | 模型名称 | 采样率 | 标点  | ITN | 顺滑  | 语义断句 | 声音和文本对齐 |
        | --- | --- | --- | --- | --- | --- | --- | --- |
        | 粤语  | 通用-粤语 | 16k | 支持  | 支持  | 支持  | 不支持 | 支持  |
        | 电话客服（通用） | 8k  | 支持  | 支持  | 支持  | 不支持 | 支持  |
        | 粤中自由说 | 8k  | 支持  | 支持  | 支持  | 不支持 | 不支持 |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 粤语（繁体） | 通用-粤语（繁体） | 8k  | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 通用-粤语（繁体） | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        | 四川话 | 通用-四川话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 电话客服（通用） | 8k  | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 湖北话 | 通用-湖北话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 通用-湖北话 | 8k  | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 上海话 | 通用-上海话 | 16k | 支持  | 支持  | 支持  | 支持  | 不支持 |
        | 湖南话 | 通用-湖南话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 河南话 | 通用-河南话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 通用-河南话 | 8k  | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 浙江话 | 通用-浙江话 | 16k | 支持  | 支持  | 支持  | 支持  | 不支持 |
        | 东北话 | 通用-东北话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 山东话 | 通用-山东话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 天津话 | 通用-天津话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 陕西话 | 通用-陕西话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 山西话 | 通用-山西话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 贵州话 | 通用-贵州话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 云南话 | 通用-云南话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 甘肃话 | 通用-甘肃话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 苏州话 | 通用-苏州话 | 16k | 支持  | 支持  | 支持  | 支持  | 不支持 |
        | 闽南语 | 通用-闽南语 | 16k | 支持  | 支持  | 支持  | 支持  | 不支持 |
        | 江西话 | 通用-江西话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 宁夏话 | 通用-宁夏话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 广西话 | 通用-广西话 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 通用-广西话 | 8k  | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 中文普通话 | 识音石 V1 - 端到端模型，教育内容分析，医疗内容分析，新闻媒体内容分析，娱乐视频内容分析，音视频离线转写（升级版），新零售领域识别模型，出行领域识别模型，汽车领域 | 16k | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 中英自由说 | 16k | 支持  | 支持  | 支持  | 支持  | 不支持 |
        | 识音石 V1 - 端到端模型 | 8k  | 支持  | 支持  | 支持  | 支持  | 支持  |
        | 东南亚多语言 | 16k | 支持  | 不支持 | 不支持 | 不支持 | 不支持 |
        

## 使用步骤

1.  了解您的录音文件格式和采样率，根据业务场景在管控台选择合适的场景模型。
    
2.  上传录音文件至OSS。
    
    如果OSS中文件访问权限为公开，可参见[公共读Object](https://help.aliyun.com/zh/oss/user-guide/how-to-obtain-the-url-of-a-single-object-or-the-urls-of-multiple-objects#concept-39607-zh/section-st4-efq-v5j)，获取文件访问链接；如果OSS中文件访问权限为私有，可参见[私有Object](https://help.aliyun.com/zh/oss/user-guide/how-to-obtain-the-url-of-a-single-object-or-the-urls-of-multiple-objects#concept-39607-zh/section-wnz-0he-q20)，通过SDK生成有有效时间的访问链接。
    
    **重要**
    
    您也可以把录音文件存放在自行搭建的文件服务器，提供文件下载。请保证HTTP的响应头（Header）中`Content-Length`的长度值和Body中数据的真实长度一致，否则会导致下载失败。
    
3.  客户端提交录音文件识别请求。
    
    正常情况下，服务端返回该请求任务的ID，用以查询识别结果。
    
4.  客户端发送识别结果查询请求。
    
    通过步骤3获取的请求任务ID查询录音文件识别的结果，目前识别的结果在服务端可保存72小时。
    

## 交互流程

客户端与服务端的交互流程如图所示。

![image](https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/5789150671/CAEQShiBgMDs5IiK2RgiIGQ0Nzg1N2MzNDRlNTRmMDA5N2VjMjJhODBiMmVhNTc44024928_20231008170233.737.svg)

**说明**

所有服务端的响应都会在返回信息的header包含表示本次识别任务的TaskId参数。

## 各地域POP调用参数

| 地域  | 调用参数 |
| 华东2（上海） | - regionId="cn-shanghai" - endpointName="cn-shanghai" - domain="filetrans.cn-shanghai.aliyuncs.com" |
| 华北2（北京） | - regionId="cn-beijing" - endpointName="cn-beijing" - domain="filetrans.cn-beijing.aliyuncs.com" |
| 华南1（深圳） | - regionId="cn-shenzhen" - endpointName="cn-shenzhen" - domain="filetrans.cn-shenzhen.aliyuncs.com" |

## 接口调用方式

录音文件识别服务是以RPC风格的POP API方式提供录音文件识别接口，将参数封装到每一个请求中，每个请求即对应一个方法，执行的结果放在response中。需要识别的录音文件必须存放在某服务上（推荐[阿里云OSS](https://www.aliyun.com/product/oss)），可以通过URL访问。使用阿里云OSS，同一地域可以通过内网访问，不计外网流量费用，具体方法请参见[使用录音文件识别时如何设置OSS内网地址](https://help.aliyun.com/zh/isi/developer-reference/how-to-set-the-internal-endpoint-of-oss-when-using-recording-file-recognition#topic-2199516)。

录音文件识别POP API包括两部分：POST方式的“录音文件识别请求调用接口”（用户级别QPS（queries per second）限制为200）、GET方式的“录音文件识别结果查询接口”（用户级别QPS限制为500）。

-   识别请求调用接口：
    
    -   当采用轮询方式时，提交录音文件识别任务，获取任务ID，供后续轮询使用。
        
    -   当采用回调方式时，提交录音文件识别任务和回调URL，任务完成后会把识别结果POST到回调地址，要求回调地址可接收POST请求。
        
        **说明**
        
        由于历史原因，早期发布的录音文件识别服务（默认为2.0版本）的回调方式和轮询方式的识别结果在JSON字符串的风格和字段上均有不同，下文将作说明。录音文件识别服务在4.0版本对回调方式做了优化，使得回调方式的识别结果与轮询方式的识别结果保持一致，均为驼峰风格的JSON格式字符串。
        
        如果您已接入录音文件识别服务，即没有设置录音文件识别服务的版本，默认为2.0版，可以继续使用；如果您新接入录音文件识别服务，请设置服务版本为4.0。
        
        输入参数及说明：
        
        提交录音文件识别请求时，需要设置输入参数，以JSON格式的字符串传入请求对象的Body，JSON格式如下：
        
        ```
        {
            "appkey": "your-appkey",
            "file_link": "https://gw.alipayobjects.com/os/bmw-prod/0574ee2e-f494-45a5-820f-63aee583045a.wav",
            "auto_split":false,
            "version": "4.0",
            "enable_words": false,
            "enable_sample_rate_adaptive": true,
            // valid_times：获取语音指定时间段的识别内容，若不需要，则无需填写。
            "valid_times": [
                {
                    "begin_time": 200,
                    "end_time":2000,
                    "channel_id": 0
                }
            ]
        }
        ```
        
        | 参数  | 值类型 | 是否必选 | 说明  |
        | --- | --- | --- | --- |
        | appkey | String | 是   | [管控台](https://nls-portal.console.aliyun.com/applist)创建的项目Appkey。 |
        | file\\_link | String | 是   | 存放录音文件的地址，需要在管控台中将对应项目的模型设置为支持该音频场景的模型。 |
        | version | String | 否   | 设置录音文件识别服务的版本，默认为**4.0**。 |
        | enable\\_words | Boolean | 否   | 是否开启返回词信息，默认为false，开启时需要设置version为**4.0**。 |
        | enable\\_sample\\_rate\\_adaptive | Boolean | 否   | 大于16 kHz采样率的音频是否进行自动降采样（降为16 kHz），默认为false，开启时需要设置version为**4.0**。 |
        | enable\\_callback | Boolean | 否   | 是否启用回调功能，默认值为false。 |
        | callback\\_url | String | 否   | 回调服务的地址，enable\\_callback取值为true时，本字段必选。URL支持HTTP和HTTPS协议，host不可使用IP地址。 |
        | auto\\_split | Boolean | 否   | 是否开启智能分轨（开启智能分轨，即可在两方对话的语音情景下，依据每句话识别结果中的ChannelId，判断该句话的发言人为哪一方。通常先发言一方ChannelId为0，8k双声道开启分轨后默认为2个人，声道channel0和channel1就是音轨编号）。 **说明** 8000\\\\16000 Hz采样率均支持，16k默认只分离首个声道。 |
        | supervise\\_type | Integer | 否   | 说话人分离的确定人数方式，需要和**auto\\_split**、**speaker\\_num**这两个参数搭配使用。 - 默认为空：8k由用户指定，16k由算法决定。 - 1：用户指定人数，具体人数由参数**speaker\\_num**确认。 - 2：算法决定人数。 |
        | speaker\\_num | Integer | 否   | 用于辅助指定声纹人数，取值范围为2至100的整数。8k音频默认为2，16k音频默认为100。 此参数只能辅助算法尽量输出指定人数，无法保证一定会输出此人数。需要和**auto\\_split**、**supervise\\_type**这两个参数搭配使用。 |
        | enable\\_inverse\\_text\\_normalization | Boolean | 否   | ITN（逆文本inverse text normalization）中文数字转换阿拉伯数字。设置为True时，中文数字将转为阿拉伯数字输出，默认值：False。 |
        | enable\\_disfluency | Boolean | 否   | 过滤语气词，即声音顺滑，默认值false（关闭），开启时需要设置version为4.0。 |
        | enable\\_punctuation\\_prediction | Boolean | 否   | 是否给句子加标点。默认值true（加标点）。 |
        | valid\\_times | List< ValidTime > | 否   | 有效时间段信息，用来排除一些不需要的时间段。 |
        | max\\_end\\_silence | Integer | 否   | 允许的最大结束静音，取值范围：200~6000，默认值800，单位为毫秒。 开启语义断句**enable\\_semantic\\_sentence\\_detection**后，此参数无效。 |
        | max\\_single\\_segment\\_time | Integer | 否   | 允许单句话最大结束时间，最小值5000，默认值60000。单位为毫秒。 开启语义断句**enable\\_semantic\\_sentence\\_detection**后，此参数无效。 |
        | customization\\_id | String | 否   | 通过POP API创建的定制模型ID，默认不添加。 |
        | class\\_vocabulary\\_id | String | 否   | 创建的类热词表ID，默认不添加。 |
        | vocabulary\\_id | String | 否   | 创建的泛热词表ID，默认不添加。 |
        | enable\\_semantic\\_sentence\\_detection | Boolean | 否   | 是否启⽤语义断句，取值：true/false，默认值false。 |
        | enable\\_timestamp\\_alignment | Boolean | 否   | 是否启用时间戳校准功能，取值：true/false，默认值false。 |
        | first\\_channel\\_only | Boolean | 否   | 是否只识别首个声道，取值：true/false。（如果录音识别结果重复，您可以开启此参数。） - 默认为空：8k处理双声道，16k处理单声道。 - false：8k处理双声道，16k处理双声道。 - true：8k处理单声道，16k处理单声道。 **重要** 计费模式： - 8k处理双声道，按单声道计费，即**音频时长**进行计费。 - 16k处理双声道，按双声道计费，即**声道数×音频时长**进行计费。 |
        | special\\_word\\_filter | String | 否   | 敏感词过滤功能，支持开启或关闭，支持自定义敏感词。该参数可实现： **不处理**（默认，即展示原文）、**过滤**、**替换为\\*。** 具体调用说明请见下文的自定义过滤词调用示例。 **说明** 开启但未配置敏感词，则会过滤默认词表：[敏感词表](https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20230602/wucl/敏感词.txt)。 |
        | punctuation\\_mark | String | 否   | 自定义标点断句。 不填默认使用句号、问号、叹号断句。如果用户填写此值，则会增加使用用户指定的标点符号断句。 示例： 按英文逗号断句填写"," 按中文和英文逗号断句填写"，," **说明** - 如果用户填写的不是标点符号则不生效。 - 此字段可填多个标点，且会区分中英文标点，标点间无空格。 |
        | sentence\\_max\\_length | Integer | 否   | 每句最多展示字数，取值范围：\\[4，50\\]。默认为不启用该功能。启用后如不填写字数，则按照长句断句。该参数可用于字幕生成场景，控制单行字幕最大字数。 |
        
        自定义过滤词调用示例如下：
        
        ```
                    // 以实时转写为例，
                    JSONObject root = new JSONObject();
                    root.put("system_reserved_filter", true);
        
                    // 将以下词语替换成空
                    JSONObject root1 = new JSONObject();
                    JSONArray array1 = new JSONArray();
                    array1.add("开始");
                    array1.add("发生");
                    root1.put("word_list", array1);
        
                    // 将以下词语替换成*
                    JSONObject root2 = new JSONObject();
                    JSONArray array2 = new JSONArray();
                    array2.add("测试");
                    root2.put("word_list", array2);
        
        						// 可以全部设置，也可以部分设置
                    root.put("filter_with_empty", root1);
                    root.put("filter_with_signed", root2);
        
                    transcriber.addCustomedParam("special_word_filter", root);
        ```
        
        其中，ValidTime对象参数说明如下表所示。
        
        | 参数  | 值类型 | 是否必选 | 说明  |
        | --- | --- | --- | --- |
        | begin\\_time | Int | 是   | 有效时间段的起始点时间偏移，单位为毫秒。 |
        | end\\_time | Int | 是   | 有效时间段的结束点时间偏移，单位为毫秒。 |
        | channel\\_id | Int | 是   | 有效时间段的作用音轨序号（从0开始）。 |
        
        输出参数及说明：
        
        服务端返回录音文件识别请求的响应，响应的输出参数为JSON格式的字符串：
        
        ```
        {
                "TaskId": "4b56f0c4b7e611e88f34c33c2a60****",
                "RequestId": "E4B183CC-6CFE-411E-A547-D877F7BD****",
                "StatusText": "SUCCESS",
                "StatusCode": 21050000
        }
        ```
        
        返回HTTP状态：200表示成功，更多状态码请查阅HTTP状态码。
        
        | 属性  | 值类型 | 是否必选 | 说明  |
        | --- | --- | --- | --- |
        | Taskid | String | 是   | 识别任务ID。 |
        | RequestId | String | 是   | 请求ID，仅用于联调。 |
        | StatusCode | Int | 是   | 状态码。 |
        | StatusText | String | 是   | 状态说明。 |
        
-   识别结果查询接口：
    
    提交完录音文件识别请求后，按照如下参数设置轮询识别结果。
    
    输入参数：
    
    通过提交录音文件识别请求获得的任务ID作为识别结果查询接口参数，获取识别结果。在接口调用过程中，需要设置一定的查询时间间隔。
    
    **重要**
    
    查询接口有QPS限制（500QPS），超过QPS之后则可能报错：`Throttling.User : Request was denied due to user flow control.`。建议查询接口的轮询间隔时长适当延长，不宜过短。
    
    | 属性  | 值类型 | 是否必选 | 说明  |
    | --- | --- | --- | --- |
    | Taskid | String | 是   | 识别任务ID。 |
    
    输出参数及说明：
    
    服务端返回识别结果查询请求的响应，响应的输出参数为JSON格式的字符串。
    
    -   正常返回：以录音文件[nls-sample-16k.wav](https://gw.alipayobjects.com/os/bmw-prod/0574ee2e-f494-45a5-820f-63aee583045a.wav)（文件为单轨）识别结果为例。
        
        ```
        {
                "TaskId": "d429dd7dd75711e89305ab6170fe****",
                "RequestId": "9240D669-6485-4DCC-896A-F8B31F94****",
                "StatusText": "SUCCESS",
                "BizDuration": 2956,
                "SolveTime": 1540363288472,
                "StatusCode": 21050000,
                "Result": {
                        "Sentences": [{
                                "EndTime": 2365,
                                "SilenceDuration": 0,
                                "BeginTime": 340,
                                "Text": "北京的天气。",
                                "ChannelId": 0,
                                "SpeechRate": 177,
                                "EmotionValue": 5.0
                        }]
                }
        }
        ```
        
        如果开启enable\_callback/callback\_url，且设置服务版本为4.0，回调识别结果为：
        
        ```
        {
                "Result": {
                        "Sentences": [{
                                "EndTime": 2365,
                                "SilenceDuration": 0,
                                "BeginTime": 340,
                                "Text": "北京的天气。",
                                "ChannelId": 0,
                                "SpeechRate": 177,
                                "EmotionValue": 5.0
                        }]
                },
                "TaskId": "36d01b244ad811e9952db7bb7ed2****",
                "StatusCode": 21050000,
                "StatusText": "SUCCESS",
                "RequestTime": 1553062810452,
                "SolveTime": 1553062810831,
                "BizDuration": 2956
        }
        ```
        
        **重要**
        
        -   RequestTime为时间戳（单位为毫秒，例如1553062810452转换为北京时间为2019/3/20 14:20:10），表示录音文件识别提交请求的时间。
            
        -   SolveTime为时间戳（单位为毫秒），表示录音文件识别完成的时间。
            
        
    -   排队中：
        
        ```
        {
                "TaskId": "c7274235b7e611e88f34c33c2a60****",
                "RequestId": "981AD922-0655-46B0-8C6A-5C836822****",
                "StatusText": "QUEUEING",
                "StatusCode": 21050002
        }
        ```
        
    -   识别中：
        
        ```
        {
                "TaskId": "c7274235b7e611e88f34c33c2a60****",
                "RequestId": "8E908ED2-867F-457E-82BF-4756194A****",
                "StatusText": "RUNNING",
                "BizDuration": 0,
                "StatusCode": 21050001
        }
        ```
        
    -   异常返回：以文件下载失败为例。
        
        ```
        {
                "TaskId": "4cf25b7eb7e711e88f34c33c2a60****",
                "RequestId": "098BF27C-4CBA-45FF-BD11-3F532F26****",
                "StatusText": "FILE_DOWNLOAD_FAILED",
                "BizDuration": 0,
                "SolveTime": 1536906469146,
                "StatusCode": 41050002
        }
        ```
        
        **说明**
        
        更多异常情况请查看下面的服务状态码的错误状态码及解决方案。
        
        返回HTTP状态：200表示成功，更多状态码请查阅HTTP状态码。
        
        | 属性  | 值类型 | 是否必选 | 说明  |
        | --- | --- | --- | --- |
        | TaskId | String | 是   | 识别任务ID。 |
        | StatusCode | Int | 是   | 状态码。 |
        | StatusText | String | 是   | 状态说明。 |
        | RequestId | String | 是   | 请求ID，用于调试。 |
        | Result | Object | 是   | 识别结果对象。 |
        | Sentences | List< SentenceResult > | 是   | 识别的结果数据。当StatusText为SUCCEED时存在。 |
        | Words | List< WordResult > | 否   | 词信息，获取时需设置enable\\_words为true，且设置服务version为”4.0”。 |
        | BizDuration | Long | 是   | 识别的音频文件总时长，单位为毫秒。 |
        | SolveTime | Long | 是   | 时间戳，单位为毫秒，录音文件识别完成的时间。 |
        
        其中，单句结果SentenceResult参数如下。
        
        | 属性  | 值类型 | 是否必选 | 说明  |
        | --- | --- | --- | --- |
        | ChannelId | Int | 是   | 该句所属音轨ID。 |
        | BeginTime | Int | 是   | 该句的起始时间偏移，单位为毫秒。 |
        | EndTime | Int | 是   | 该句的结束时间偏移，单位为毫秒。 |
        | Text | String | 是   | 该句的识别文本结果。 |
        | EmotionValue | Float | 是   | 情绪能量值，取值为音量分贝值/10。取值范围：\\[1,10\\]。值越高情绪越强烈。 |
        | SilenceDuration | Int | 是   | 本句与上一句之间的静音时长，单位为秒。 |
        | SpeechRate | Int | 是   | 本句的平均语速。 - 若识别语言为中文，则单位为：字数/分钟。 - 若识别语言为英文，则单位为：单词数/分钟。 |
        
    -   开启返回词信息：
        
        如果enable\_words设置为true，且设置服务version为"4.0"，服务端的识别结果将包含词信息。使用轮询方式和回调方式获得的词信息相同，以轮询方式的识别结果为例：
        
        ```
        {
                "StatusCode": 21050000,
                "Result": {
                        "Sentences": [{
                                "SilenceDuration": 0,
                                "EmotionValue": 5.0,
                                "ChannelId": 0,
                                "Text": "北京的天气。",
                                "BeginTime": 340,
                                "EndTime": 2365,
                                "SpeechRate": 177
                        }],
                        "Words": [{
                                "ChannelId": 0,
                                "Word": "北京",
                                "BeginTime": 640,
                                "EndTime": 940
                        }, {
                                "ChannelId": 0,
                                "Word": "的",
                                "BeginTime": 940,
                                "EndTime": 1120
                        }, {
                                "ChannelId": 0,
                                "Word": "天气",
                                "BeginTime": 1120,
                                "EndTime": 2020
                        }]
                },
                "SolveTime": 1553236968873,
                "StatusText": "SUCCESS",
                "RequestId": "027B126B-4AC8-4C98-9FEC-A031158F****",
                "TaskId": "b505e78c4c6d11e9a213e11db149****",
                "BizDuration": 2956
        }
        ```
        
        Words对象说明：
        
        | 属性  | 值类型 | 是否必选 | 说明  |
        | --- | --- | --- | --- |
        | BeginTime | Int | 是   | 词开始时间，单位为毫秒。 |
        | EndTime | Int | 是   | 词结束时间，单位为毫秒。 |
        | ChannelId | Int | 是   | 该词所属音轨ID。 |
        | Word | String | 是   | 词信息。 |
        

## 服务状态码

### **通用错误码**

| **状态码** | **状态消息** | **原因** | **解决方案** |
| --- | --- | --- | --- |
| 40000000 | 默认的客户端错误码，对应了多个错误消息。 | 用户使用了不合理的参数或者调用逻辑。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000001 | The token 'xxx' has expired； The token 'xxx' is invalid | 用户使用了不合理的参数或者调用逻辑。通用客户端错误码，通常是涉及Token相关的不正确使用，例如Token过期或者非法。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000002 | Gateway:MESSAGE\\_INVALID:Can't process message in state'FAILED'! | 无效或者错误的报文消息。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000003 | PARAMETER\\_INVALID; Failed to decode url params | 用户传递的参数有误，一般常见于RESTful接口调用。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40000005 | Gateway:TOO\\_MANY\\_REQUESTS:Too many requests! | 并发请求过多。 | 如果是试用版调用，建议您升级为商用版本以增大并发。 如果已是商用版，可购买并发资源包，扩充您的并发额度。 |
| 40000009 | Invalid wav header! | 错误的消息头。 | 如果您发送的是WAV语音文件，且设置`format`为`wav`，请注意检查该语音文件的WAV头是否正确，否则可能会被服务端拒绝。 |
| 40000009 | Too large wav header! | 传输的语音WAV头不合法。 | 建议使用PCM、OPUS等格式发送音频流，如果是WAV，建议关注语音文件的WAV头信息是否为正确的数据长度大小。 |
| 40000010 | Gateway:FREE\\_TRIAL\\_EXPIRED:The free trial has expired! | 试用期已结束，并且未开通商用版、或账号欠费。 | 请登录控制台确认服务开通状态以及账户余额。 |
| 40010001 | Gateway:NAMESPACE\\_NOT\\_FOUND:RESTful url path illegal | 不支持的接口或参数。 | 请检查调用时传递的参数内容是否和官网文档要求的一致，并结合错误信息对比排查，设置为正确的参数。 比如您是否通过curl命令执行RESTful接口请求， 拼接的URL是否合法。 |
| 40010003 | Gateway:DIRECTIVE\\_INVALID:\\[xxx\\] | 客户端侧通用错误码。 | 表示客户端传递了不正确的参数或指令，在不同的接口上有对应的详细报错信息，请参考对应文档进行正确设置。 |
| 40010004 | Gateway:CLIENT\\_DISCONNECT:Client disconnected before task finished! | 在请求处理完成前客户端主动结束。 | 无，或者请在服务端响应完成后再关闭链接。 |
| 40010005 | Gateway:TASK\\_STATE\\_ERROR:Got stop directive while task is stopping! | 客户端发送了当前不支持的消息指令。 | 请参考官网文档示例代码进行对比测试验证。 |
| 40020105 | Meta:APPKEY\\_NOT\\_EXIST:Appkey not exist! | 使用了不存在的Appkey。 | 请确认是否使用了不存在的Appkey，Appkey可以通过登录控制台后查看项目配置。 |
| 40020106 | Meta:APPKEY\\_UID\\_MISMATCH:Appkey and user mismatch! | 调用时传递的Appkey和Token并非同一个账号UID所创建，导致不匹配。 | 请检查是否存在两个账号混用的情况，避免使用账号A名下的Appkey和账号B名下生成的Token搭配使用。 |
| 403 | Forbidden | 使用的Token无效，例如Token不存在或者已过期。 | 请设置正确的Token。Token存在有效期限制，请及时在过期前获取新的Token。 |
| 41000003 | MetaInfo doesn't have end point info | 无法获取该Appkey的路由信息。 | 请检查是否存在两个账号混用的情况，避免使用账号A名下的Appkey和账号B名下生成的Token搭配使用。 |
| 41010101 | UNSUPPORTED\\_SAMPLE\\_RATE | 不支持的采样率格式。 | 当前实时语音识别只支持8000 Hz和16000 Hz两种采样率格式的音频。 |
| 41040201 | Realtime:GET\\_CLIENT\\_DATA\\_TIMEOUT:Client data does not send continuously! | 获取客户端发送的数据超时失败。 | 客户端在调用实时语音识别时请保持实时速率发送，发送完成后及时关闭链接。 |
| 50000000 | GRPC\\_ERROR:Grpc error! | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复。 |
| 50000001 | GRPC\\_ERROR:Grpc error! | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复。 |
| 52010001 | GRPC\\_ERROR:Grpc error! | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复。 |

### 录音文件识别/录音文件识别闲时版错误码

| **状态码** | **状态消息** | **原因** | **解决方案** |
| --- | --- | --- | --- |
| 21050000 | SUCCESS | 成功。 | 无。  |
| 21050001 | RUNNING | 录音文件识别任务运行中。 | 请稍后再发送GET方式的识别结果查询请求。 |
| 21050002 | QUEUEING | 录音文件识别任务排队中。 | 请稍后再发送GET方式的识别结果查询请求。 |
| 21050003 | SUCCESS\\_WITH\\_NO\\_VALID\\_FRAGMENT | 识别结果查询接口调用成功，但是VAD模块未检测到有效语音。 | 此种情况下可检查： 录音文件是否包含有效语音，如果都是无效语音，例如纯静音。上述情况下没有识别结果是正常现象。 |
| ASR\\_RESPONSE\\_HAVE\\_NO\\_WORDS | 识别结果查询接口调用成功，但是最终识别结果为空。 | 此种情况下可检查： 录音文件是否包含有效语音，或有效语音是否都是语气词且开启了顺滑参数**enable\\_disfluency**，导致语气词被过滤。 上述情况下没有识别结果是正常现象。 |
| 41050001 | USER\\_BIZDURATION\\_QUOTA\\_EXCEED | 单日时间超限（免费用户每日可识别不超过2小时时长的录音文件）。 | 建议从免费版升级到商用版。如业务量较大，请联系商务洽谈，邮件地址：**nls\\_support@service.aliyun.com**。 |
| 41050002 | FILE\\_DOWNLOAD\\_FAILED | 文件下载失败。 | 检查录音文件路径是否正确，以及是否可以通过外网访问和下载。 |
| 41050003 | FILE\\_CHECK\\_FAILED | 文件格式错误。 | 检查录音文件是否是单轨/双轨的WAV格式或MP3格式。 |
| 41050004 | FILE\\_TOO\\_LARGE | 文件过大。 | 检查录音文件大小是否超过512 MB，超过则需您对录音文件分段。 |
| 41050005 | FILE\\_NORMALIZE\\_FAILED | 文件归一化失败。 | 检查录音文件是否有损坏，是否可以正常播放。 |
| 41050006 | FILE\\_PARSE\\_FAILED | 文件解析失败。 | 检查录音文件是否有损坏，是否可以正常播放。 |
| 41050007 | MKV\\_PARSE\\_FAILED | MKV解析失败。 | 检查录音文件是否损坏，是否可以正常播放。 |
| 41050008 | UNSUPPORTED\\_SAMPLE\\_RATE | 采样率不匹配。 | 检查实际语音的采样率和控制台上Appkey绑定的ASR模型采样率是否一致，或者将本篇文档中自动降采样的参数enable\\_sample\\_rate\\_adaptive设置为true。 |
| 41050010 | FILE\\_TRANS\\_TASK\\_EXPIRED | 录音文件识别任务过期。 | TaskId不存在，或者已过期。 |
| 41050011 | REQUEST\\_INVALID\\_FILE\\_URL\\_VALUE | 请求file\\_link参数非法。 | 确认file\\_link参数格式是否正确。 |
| 41050012 | REQUEST\\_INVALID\\_CALLBACK\\_VALUE | 请求callback\\_url参数非法。 | 确认callback\\_url参数格式是否正确，是否为空。 |
| 41050013 | REQUEST\\_PARAMETER\\_INVALID | 请求参数无效。 | 确认请求task值为有效的JSON格式字符串。 |
| 41050014 | REQUEST\\_EMPTY\\_APPKEY\\_VALUE | 请求参数appkey值为空。 | 确认是否设置了appkey参数值。 |
| 41050015 | REQUEST\\_APPKEY\\_UNREGISTERED | 请求参数appkey未注册。 | 确认请求参数appkey值是否设置正确，或者是否与阿里云账号的AccessKey ID同一个账号。 |
| 41050021 | RAM\\_CHECK\\_FAILED | RAM检查失败。 | 检查您的RAM用户是否已经授权调用语音服务的API，具体操作，请参见[RAM用户权限配置](https://help.aliyun.com/zh/isi/getting-started/start-here)。 |
| 41050023 | CONTENT\\_LENGTH\\_CHECK\\_FAILED | content-length 检查失败。 | 检查下载文件时，HTTP response中的content-length与文件实际大小是否一致。 |
| 41050024 | FILE\\_404\\_NOT\\_FOUND | 需要下载的文件不存在。 | 检查需要下载的文件是否存在。 |
| 41050025 | FILE\\_403\\_FORBIDDEN | 没有权限下载需要的文件。 | 检查是否有权限下载录音文件。 |
| 41050026 | FILE\\_SERVER\\_ERROR | 请求的文件所在的服务不可用。 | 检查请求的文件所在的服务是否可用。 |
| 41050103 | AUDIO\\_DURATION\\_TOO\\_LONG | 请求的文件时长超过12小时。 | 建议将音频进行切分，分多次提交识别任务，[切分命令参考](https://help.aliyun.com/zh/isi/support/faq-about-input-formats-for-speech-recognition#33739064beavy)。 |
| 40270003 | DECODER\\_ERROR | 检测音频文件信息失败。 | 确认文件下载链接中文件为支持的音频格式。 |
| 51050000 | INTERNAL\\_ERROR | 受机器负载、网络等因素导致的异常，通常为偶发出现。 | 一般重试调用即可恢复，如无法恢复，请联系技术支持人员。 |

## 历史版本说明

如果您已接入录音文件识别，即没有设置服务版本为4.0，默认会使用录音文件识别4.0版本。回调方式的识别结果与轮询方式的识别结果在JSON字符串的风格和字段上有所不同， 如果开启enable\_callback/callback\_url，回调识别结果为：

```
{
        "result": [{
                "begin_time": 340,
                "channel_id": 0,
                "emotion_value": 5.0,
                "end_time": 2365,
                "silence_duration": 0,
                "speech_rate": 177,
                "text": "北京的天气。"
        }],
        "task_id": "3f5d4c0c399511e98dc025f34473****",
        "status_code": 21050000,
        "status_text": "SUCCESS",
        "request_time": 1551164878830,
        "solve_time": 1551164879230,
        "biz_duration": 2956
}
```