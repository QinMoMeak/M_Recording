package com.qinmomeak.recording

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.qinmomeak.recording.databinding.ActivityResultDetailBinding
import kotlinx.coroutines.launch

class ResultDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBack.setOnClickListener { finish() }

        val path = intent.getStringExtra(EXTRA_PATH).orEmpty()
        if (path.isBlank()) {
            binding.transcriptText.text = "暂无内容"
            binding.summaryText.text = "暂无内容"
            return
        }

        lifecycleScope.launch {
            val manager = FileManager(this@ResultDetailActivity)
            val record = manager.find(path)
            binding.transcriptText.text = record?.transcriptText?.ifBlank { "暂无内容" } ?: "暂无内容"
            binding.summaryText.text = record?.summaryText?.ifBlank { "暂无内容" } ?: "暂无内容"
        }
    }

    companion object {
        const val EXTRA_PATH = "extra_media_path"
    }
}

