 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(private val dp: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val scale = view.resources.displayMetrics.density
        val px = (dp * scale).toInt()
        outRect.bottom = px
    }
}


