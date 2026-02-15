package com.qinmomeak.recording

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.qinmomeak.recording.data.FileRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaLibraryAdapter(
    private val onItemClick: (FileRecord) -> Unit,
    private val onItemLongClick: (FileRecord) -> Unit
) : RecyclerView.Adapter<MediaLibraryAdapter.Holder>() {

    private val items = mutableListOf<FileRecord>()
    private val selected = mutableSetOf<String>()
    var selectionEnabled: Boolean = false
        private set

    fun submit(list: List<FileRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setSelectionEnabled(enabled: Boolean) {
        selectionEnabled = enabled
        if (!enabled) {
            selected.clear()
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(record: FileRecord) {
        if (selected.contains(record.filePath)) selected.remove(record.filePath) else selected.add(record.filePath)
        notifyDataSetChanged()
    }

    fun getSelectedPaths(): List<String> = selected.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media_record, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.bind(item, selectionEnabled, selected.contains(item.filePath))
        holder.itemView.setOnClickListener {
            if (selectionEnabled) {
                toggleSelection(item)
            } else {
                onItemClick(item)
            }
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.mediaTitle)
        private val sub = itemView.findViewById<TextView>(R.id.mediaSub)
        private val badge = itemView.findViewById<TextView>(R.id.mediaBadge)
        private val check = itemView.findViewById<View>(R.id.mediaSelected)

        fun bind(record: FileRecord, selectionEnabled: Boolean, selected: Boolean) {
            title.text = record.fileName
            val sizeMb = if (record.sizeBytes > 0) String.format(Locale.getDefault(), "%.2fMB", record.sizeBytes / 1024.0 / 1024.0) else "--"
            val duration = if (record.durationMs > 0) formatDuration(record.durationMs) else "--"
            val date = if (record.addedTimeSec > 0) {
                val ts = record.addedTimeSec * 1000
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts))
            } else {
                "--"
            }
            sub.text = "$duration  |  $sizeMb  |  $date"
            badge.isVisible = record.isProcessed
            check.isVisible = selectionEnabled
            check.isSelected = selected
            itemView.isSelected = selected
        }

        private fun formatDuration(ms: Long): String {
            val totalSec = ms / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        }
    }
}

