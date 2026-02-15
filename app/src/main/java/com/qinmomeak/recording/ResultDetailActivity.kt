package com.qinmomeak.recording

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.qinmomeak.recording.databinding.ActivityResultDetailBinding
import kotlinx.coroutines.launch

class ResultDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultDetailBinding
    private val manager by lazy { FileManager(this) }

    private var filePath: String = ""
    private var transcriptText: String = ""
    private var summaryText: String = ""
    private var currentTab: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBack.setOnClickListener { finish() }
        binding.actionCopy.setOnClickListener { copyCurrent() }
        binding.actionSave.setOnClickListener { saveCurrent() }
        setupTabs()

        filePath = intent.getStringExtra(EXTRA_PATH).orEmpty()
        if (filePath.isBlank()) {
            binding.contentEditText.setText(getString(R.string.dialog_empty))
            return
        }

        lifecycleScope.launch {
            val record = manager.find(filePath)
            binding.fileNameText.text = record?.fileName.orEmpty()
            transcriptText = record?.transcriptText.orEmpty()
            summaryText = record?.summaryText.orEmpty()
            renderCurrentTab()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_transcript))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_summary))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                renderCurrentTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun renderCurrentTab() {
        val content = if (currentTab == 0) transcriptText else summaryText
        binding.contentEditText.setText(content.ifBlank { getString(R.string.dialog_empty) })
        binding.contentEditText.setSelection(binding.contentEditText.text?.length ?: 0)
    }

    private fun copyCurrent() {
        val content = binding.contentEditText.text?.toString().orEmpty()
        if (content.isBlank() || content == getString(R.string.dialog_empty)) {
            Toast.makeText(this, "暂无可复制内容", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("result", content))
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrent() {
        if (filePath.isBlank()) return
        val content = binding.contentEditText.text?.toString().orEmpty()
        if (currentTab == 0) {
            transcriptText = if (content == getString(R.string.dialog_empty)) "" else content
        } else {
            summaryText = if (content == getString(R.string.dialog_empty)) "" else content
        }
        lifecycleScope.launch {
            manager.updateResultContent(filePath, transcriptText, summaryText)
            Toast.makeText(this@ResultDetailActivity, "内容已保存", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_PATH = "extra_media_path"
    }
}
