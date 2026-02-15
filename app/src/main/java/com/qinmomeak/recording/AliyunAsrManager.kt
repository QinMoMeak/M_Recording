package com.qinmomeak.recording

import android.content.Context
import android.net.Uri

class AliyunAsrManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onAsrProgress(progress: Int)
        fun onAsrCompleted(text: String)
        fun onAsrError(message: String)
    }

    fun release() {
        // FileTrans API path is used in MainActivity; keep this for compatibility.
    }
}
