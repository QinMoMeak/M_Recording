 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OutputViewModel : ViewModel() {
    val transcript: MutableLiveData<String> = MutableLiveData("")
    val summary: MutableLiveData<String> = MutableLiveData("")

    fun setTranscript(text: String) {
        transcript.value = text
    }

    fun setSummary(text: String) {
        summary.value = text
    }
}


