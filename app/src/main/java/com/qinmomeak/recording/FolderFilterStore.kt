package com.qinmomeak.recording

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

class FolderFilterStore(context: Context) {
    private val prefs = context.getSharedPreferences("folder_filter", Context.MODE_PRIVATE)

    fun addTree(treeUri: Uri): Boolean {
        val prefix = toRelativePrefix(treeUri) ?: return false
        val treeSet = prefs.getStringSet(KEY_TREES, emptySet()).orEmpty().toMutableSet()
        val prefixSet = prefs.getStringSet(KEY_PREFIXES, emptySet()).orEmpty().toMutableSet()
        val changed = treeSet.add(treeUri.toString()) or prefixSet.add(prefix)
        if (changed) {
            prefs.edit()
                .putStringSet(KEY_TREES, treeSet)
                .putStringSet(KEY_PREFIXES, prefixSet)
                .apply()
        }
        return changed
    }

    fun clearAll() {
        prefs.edit().remove(KEY_TREES).remove(KEY_PREFIXES).apply()
    }

    fun getPrefixes(): Set<String> {
        return prefs.getStringSet(KEY_PREFIXES, emptySet()).orEmpty()
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun treeCount(): Int = prefs.getStringSet(KEY_TREES, emptySet())?.size ?: 0

    private fun toRelativePrefix(treeUri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val rawPath = docId.substringAfter(':', "").trim()
            if (rawPath.isBlank()) return null
            normalize(rawPath)
        } catch (_: Exception) {
            null
        }
    }

    private fun normalize(path: String): String {
        val normalized = path.replace('\\', '/').trim('/').trim()
        return if (normalized.isBlank()) "" else "$normalized/"
    }

    companion object {
        private const val KEY_TREES = "selected_trees"
        private const val KEY_PREFIXES = "selected_prefixes"
    }
}
