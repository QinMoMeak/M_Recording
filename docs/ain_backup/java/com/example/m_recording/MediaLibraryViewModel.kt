 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinmomeak.recording.data.FileRecord
import kotlinx.coroutines.launch

class MediaLibraryViewModel(private val fileManager: FileManager) : ViewModel() {
    private val _records = MutableLiveData<List<FileRecord>>(emptyList())
    val records: LiveData<List<FileRecord>> = _records

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    var sortBy: SortBy = SortBy.TIME
    var ascending: Boolean = false
    var currentScope: MediaScope = MediaScope.VISIBLE

    fun syncAndLoad() {
        viewModelScope.launch {
            _loading.value = true
            fileManager.syncMediaStore()
            load()
            _loading.value = false
        }
    }

    fun load() {
        viewModelScope.launch {
            _records.value = fileManager.getRecords(currentScope, sortBy, ascending)
        }
    }

    fun toggleSort(newSort: SortBy) {
        if (sortBy == newSort) {
            ascending = !ascending
        } else {
            sortBy = newSort
            ascending = false
        }
        load()
    }

    fun changeScope(newScope: MediaScope) {
        currentScope = newScope
        load()
    }

    fun hide(paths: List<String>) {
        viewModelScope.launch {
            fileManager.hide(paths)
            load()
        }
    }

    fun unhide(paths: List<String>) {
        viewModelScope.launch {
            fileManager.unhide(paths)
            load()
        }
    }
}


