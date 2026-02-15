 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MediaLibraryViewModelFactory(private val fileManager: FileManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaLibraryViewModel(fileManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


