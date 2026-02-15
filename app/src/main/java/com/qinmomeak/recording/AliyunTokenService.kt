package com.qinmomeak.recording

class AliyunTokenService {
    data class TokenResult(val token: String?, val error: String? = null)

    fun getToken(): TokenResult {
        if (BuildConfig.ALIYUN_TOKEN.isNotBlank()) return TokenResult(BuildConfig.ALIYUN_TOKEN)
        return TokenResult(null, "ALIYUN_TOKEN not configured")
    }
}
