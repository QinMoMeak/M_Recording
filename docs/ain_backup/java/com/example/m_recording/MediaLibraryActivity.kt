 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.qinmomeak.recording.data.FileRecord
import com.qinmomeak.recording.databinding.ActivityMediaLibraryBinding

class MediaLibraryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaLibraryBinding
    private lateinit var viewModel: MediaLibraryViewModel
    private lateinit var adapter: MediaLibraryAdapter

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.syncAndLoad()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            MediaLibraryViewModelFactory(FileManager(this))
        )[MediaLibraryViewModel::class.java]

        setupRecycler()
        setupSortActions()
        setupScopeActions()
        setupToolbarActions()
        observeViewModel()

        ensurePermissionsAndLoad()
    }

    private fun setupRecycler() {
        adapter = MediaLibraryAdapter(
            onItemClick = { showRecordDialog(it) },
            onItemLongClick = { startSelection(it) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.addItemDecoration(SpaceItemDecoration(8))
        binding.recycler.adapter = adapter
    }

    private fun setupSortActions() {
        binding.sortTime.setOnClickListener {
            viewModel.toggleSort(SortBy.TIME)
            updateSortHighlight()
        }
        binding.sortName.setOnClickListener {
            viewModel.toggleSort(SortBy.NAME)
            updateSortHighlight()
        }
        binding.sortSize.setOnClickListener {
            viewModel.toggleSort(SortBy.SIZE)
            updateSortHighlight()
        }
        updateSortHighlight()
    }

    private fun setupScopeActions() {
        binding.scopeVisible.setOnClickListener {
            viewModel.changeScope(MediaScope.VISIBLE)
            updateScopeHighlight()
        }
        binding.scopeHidden.setOnClickListener {
            viewModel.changeScope(MediaScope.HIDDEN)
            updateScopeHighlight()
        }
        updateScopeHighlight()
    }

    private fun setupToolbarActions() {
        binding.actionBack.setOnClickListener { finish() }
        binding.actionRefresh.setOnClickListener { viewModel.syncAndLoad() }
        binding.actionCancel.setOnClickListener { stopSelection() }
        binding.actionHide.setOnClickListener { applyHide() }
        binding.actionUnhide.setOnClickListener { applyUnhide() }
    }

    private fun observeViewModel() {
        viewModel.records.observe(this) { records ->
            adapter.submit(records)
            binding.emptyState.text = if (records.isEmpty()) {
                if (viewModel.currentScope == MediaScope.HIDDEN) "鏆傛棤闅愯棌鏂囦欢" else "鏆傛棤濯掍綋鏂囦欢"
            } else ""
            updateScopeHighlight()
            updateSortHighlight()
        }
        viewModel.loading.observe(this) { loading ->
            binding.loadingText.text = if (loading) "鎵弿涓?.." else ""
        }
    }

    private fun updateSortHighlight() {
        val timeActive = viewModel.sortBy == SortBy.TIME
        val nameActive = viewModel.sortBy == SortBy.NAME
        val sizeActive = viewModel.sortBy == SortBy.SIZE
        binding.sortTime.setBackgroundColor(
            getColor(if (timeActive) R.color.ios_toggle_on else R.color.ios_surface)
        )
        binding.sortName.setBackgroundColor(
            getColor(if (nameActive) R.color.ios_toggle_on else R.color.ios_surface)
        )
        binding.sortSize.setBackgroundColor(
            getColor(if (sizeActive) R.color.ios_toggle_on else R.color.ios_surface)
        )
        binding.sortTime.setTextColor(getColor(if (timeActive) R.color.ios_accent else R.color.ios_text_secondary))
        binding.sortName.setTextColor(getColor(if (nameActive) R.color.ios_accent else R.color.ios_text_secondary))
        binding.sortSize.setTextColor(getColor(if (sizeActive) R.color.ios_accent else R.color.ios_text_secondary))
    }

    private fun updateScopeHighlight() {
        val visibleActive = viewModel.currentScope == MediaScope.VISIBLE
        val hiddenActive = viewModel.currentScope == MediaScope.HIDDEN
        binding.scopeVisible.setBackgroundColor(
            getColor(if (visibleActive) R.color.ios_toggle_on else R.color.ios_surface)
        )
        binding.scopeHidden.setBackgroundColor(
            getColor(if (hiddenActive) R.color.ios_toggle_on else R.color.ios_surface)
        )
        binding.scopeVisible.setTextColor(getColor(if (visibleActive) R.color.ios_accent else R.color.ios_text_secondary))
        binding.scopeHidden.setTextColor(getColor(if (hiddenActive) R.color.ios_accent else R.color.ios_text_secondary))
    }

    private fun ensurePermissionsAndLoad() {
        requestPermissions.launch(PermissionUtils.mediaPermissions())
    }

    private fun startSelection(record: FileRecord) {
        if (!adapter.selectionEnabled) {
            adapter.setSelectionEnabled(true)
            binding.selectionBar.visibility = android.view.View.VISIBLE
        }
        adapter.toggleSelection(record)
    }

    private fun stopSelection() {
        adapter.setSelectionEnabled(false)
        binding.selectionBar.visibility = android.view.View.GONE
    }

    private fun applyHide() {
        val selected = adapter.getSelectedPaths()
        if (selected.isEmpty()) {
            Toast.makeText(this, "鏈€夋嫨鏂囦欢", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.currentScope == MediaScope.HIDDEN) {
            Toast.makeText(this, "褰撳墠涓洪殣钘忓垪琛?, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.hide(selected)
        stopSelection()
    }

    private fun applyUnhide() {
        val selected = adapter.getSelectedPaths()
        if (selected.isEmpty()) {
            Toast.makeText(this, "鏈€夋嫨鏂囦欢", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.currentScope == MediaScope.VISIBLE) {
            Toast.makeText(this, "褰撳墠涓哄彲瑙佸垪琛?, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.unhide(selected)
        stopSelection()
    }

    private fun showRecordDialog(record: FileRecord) {
        val items = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (!record.isProcessed) {
            items += "寮€濮嬪鐞?
            actions += { startProcess(record) }
        } else {
            items += "鏌ョ湅缁撴灉"
            actions += { openDetail(record) }
            items += "閲嶆柊璇嗗埆"
            actions += { startProcess(record) }
        }

        AlertDialog.Builder(this)
            .setTitle(record.fileName)
            .setItems(items.toTypedArray()) { _, which -> actions[which].invoke() }
            .show()
    }

    private fun startProcess(record: FileRecord) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(EXTRA_MEDIA_URI, record.filePath)
        startActivity(intent)
    }

    private fun openDetail(record: FileRecord) {
        val intent = Intent(this, ResultDetailActivity::class.java)
        intent.putExtra(ResultDetailActivity.EXTRA_PATH, record.filePath)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_MEDIA_URI = "extra_media_uri"

        fun open(context: Context) {
            context.startActivity(Intent(context, MediaLibraryActivity::class.java))
        }
    }
}


