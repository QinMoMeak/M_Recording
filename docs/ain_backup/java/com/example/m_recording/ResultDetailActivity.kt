 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

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
            binding.transcriptText.text = "鏆傛棤鍐呭"
            binding.summaryText.text = "鏆傛棤鍐呭"
            return
        }

        lifecycleScope.launch {
            val manager = FileManager(this@ResultDetailActivity)
            val record = manager.find(path)
            binding.transcriptText.text = record?.transcriptText?.ifBlank { "鏆傛棤鍐呭" } ?: "鏆傛棤鍐呭"
            binding.summaryText.text = record?.summaryText?.ifBlank { "鏆傛棤鍐呭" } ?: "鏆傛棤鍐呭"
        }
    }

    companion object {
        const val EXTRA_PATH = "extra_media_path"
    }
}


