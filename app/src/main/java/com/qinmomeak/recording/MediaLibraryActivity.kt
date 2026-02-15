package com.qinmomeak.recording

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.qinmomeak.recording.data.FileRecord
import com.qinmomeak.recording.databinding.ActivityMediaLibraryBinding
import kotlinx.coroutines.launch

class MediaLibraryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaLibraryBinding
    private lateinit var viewModel: MediaLibraryViewModel
    private lateinit var adapter: MediaLibraryAdapter
    private lateinit var folderStore: FolderFilterStore
    private val prefs by lazy { getSharedPreferences("media_library_prefs", Context.MODE_PRIVATE) }
    private val fileManager by lazy { FileManager(this) }

    private val requestPermissions = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.syncAndLoad()
    }

    private val pickFolder = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val changed = folderStore.addTree(uri)
        if (changed) {
            Toast.makeText(this, "已添加文件夹", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "该文件夹已存在或无效", Toast.LENGTH_SHORT).show()
        }
        updateFolderHint()
        viewModel.syncAndLoad()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        folderStore = FolderFilterStore(this)

        viewModel = ViewModelProvider(
            this,
            MediaLibraryViewModelFactory(FileManager(this))
        )[MediaLibraryViewModel::class.java]
        restoreSortPrefs()

        setupRecycler()
        setupSortActions()
        setupScopeActions()
        setupToolbarActions()
        setupFolderActions()
        observeViewModel()
        updateFolderHint()

        ensurePermissionsAndLoad()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from processing/detail pages so isProcessed state is current.
        viewModel.syncAndLoad()
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
            persistSortPrefs()
            updateSortHighlight()
        }
        binding.sortName.setOnClickListener {
            viewModel.toggleSort(SortBy.NAME)
            persistSortPrefs()
            updateSortHighlight()
        }
        binding.sortSize.setOnClickListener {
            viewModel.toggleSort(SortBy.SIZE)
            persistSortPrefs()
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
        binding.filterProcessed.setOnClickListener {
            viewModel.toggleProcessedOnly()
            updateFilterHighlight()
        }
        updateScopeHighlight()
        updateFilterHighlight()
    }

    private fun setupToolbarActions() {
        binding.actionBack.setOnClickListener { finish() }
        binding.actionRefresh.setOnClickListener { viewModel.syncAndLoad() }
        binding.actionCancel.setOnClickListener { stopSelection() }
        binding.actionHide.setOnClickListener { applyHide() }
        binding.actionUnhide.setOnClickListener { applyUnhide() }
    }

    private fun setupFolderActions() {
        binding.actionPickFolder.setOnClickListener {
            pickFolder.launch(null)
        }
        binding.actionClearFolder.setOnClickListener {
            folderStore.clearAll()
            updateFolderHint()
            viewModel.syncAndLoad()
            Toast.makeText(this, "已清空已选文件夹", Toast.LENGTH_SHORT).show()
        }
        binding.actionExportCsv.setOnClickListener {
            lifecycleScope.launch {
                val csv = fileManager.exportAllRecordsCsv()
                Toast.makeText(
                    this@MediaLibraryActivity,
                    "CSV已导出: ${csv.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.actionImportCsv.setOnClickListener {
            showImportCsvDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.records.observe(this) { records ->
            adapter.submit(records)
            binding.emptyState.text = if (records.isEmpty()) {
                when {
                    folderStore.treeCount() == 0 -> "请先选择文件夹"
                    viewModel.currentScope == MediaScope.HIDDEN && viewModel.processedOnly -> "暂无已识别隐藏文件"
                    viewModel.currentScope == MediaScope.HIDDEN -> "暂无隐藏文件"
                    viewModel.processedOnly -> "暂无已识别文件"
                    else -> "暂无媒体文件"
                }
            } else ""
            updateScopeHighlight()
            updateSortHighlight()
            updateFilterHighlight()
        }
        viewModel.loading.observe(this) { loading ->
            binding.loadingText.text = if (loading) "扫描中..." else ""
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

    private fun updateFilterHighlight() {
        val active = viewModel.processedOnly
        binding.filterProcessed.setBackgroundColor(
            getColor(if (active) R.color.ios_toggle_on else R.color.ios_surface)
        )
        binding.filterProcessed.setTextColor(
            getColor(if (active) R.color.ios_accent else R.color.ios_text_secondary)
        )
    }

    private fun ensurePermissionsAndLoad() {
        requestPermissions.launch(PermissionUtils.mediaPermissions())
    }

    private fun showImportCsvDialog() {
        val files = fileManager.listBackupCsvFiles()
        if (files.isEmpty()) {
            Toast.makeText(this, "未找到可导入CSV，请先导出备份", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = files.map { "${it.name} (${formatFileSize(it.length())})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择要导入的CSV")
            .setItems(labels) { _, which ->
                lifecycleScope.launch {
                    val result = fileManager.importRecordsCsv(files[which])
                    Toast.makeText(
                        this@MediaLibraryActivity,
                        "CSV导入完成: 总计${result.total}，成功${result.imported}，失败${result.failed}",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.load()
                }
            }
            .show()
    }

    private fun updateFolderHint() {
        val count = folderStore.treeCount()
        binding.folderHint.text = if (count <= 0) {
            getString(R.string.folder_not_selected)
        } else {
            val dir = fileManager.getCsvBackupDir().absolutePath
            getString(R.string.folder_selected_count, count) + " | CSV: $dir"
        }
    }

    private fun persistSortPrefs() {
        prefs.edit()
            .putString(KEY_SORT_BY, viewModel.sortBy.name)
            .putBoolean(KEY_SORT_ASC, viewModel.ascending)
            .apply()
    }

    private fun restoreSortPrefs() {
        val savedSort = prefs.getString(KEY_SORT_BY, SortBy.TIME.name).orEmpty()
        val sort = runCatching { SortBy.valueOf(savedSort) }.getOrDefault(SortBy.TIME)
        val asc = prefs.getBoolean(KEY_SORT_ASC, false)
        viewModel.sortBy = sort
        viewModel.ascending = asc
    }

    private fun formatFileSize(size: Long): String {
        val mb = size / 1024.0 / 1024.0
        return String.format("%.2fMB", mb)
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
            Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.currentScope == MediaScope.HIDDEN) {
            Toast.makeText(this, "当前为隐藏列表", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.hide(selected)
        stopSelection()
    }

    private fun applyUnhide() {
        val selected = adapter.getSelectedPaths()
        if (selected.isEmpty()) {
            Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.currentScope == MediaScope.VISIBLE) {
            Toast.makeText(this, "当前为可见列表", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.unhide(selected)
        stopSelection()
    }

    private fun showRecordDialog(record: FileRecord) {
        lifecycleScope.launch {
            val latest = FileManager(this@MediaLibraryActivity).find(record.filePath) ?: record
            val items = mutableListOf<String>()
            val actions = mutableListOf<() -> Unit>()
            val hasResult = latest.isProcessed || latest.transcriptText.isNotBlank() || latest.summaryText.isNotBlank()

            if (!hasResult) {
                items += "开始处理"
                actions += { startProcess(latest) }
            } else {
                items += "查看结果"
                actions += { openDetail(latest) }
                items += "重新识别"
                actions += { startProcess(latest) }
            }

            AlertDialog.Builder(this@MediaLibraryActivity)
                .setTitle(latest.fileName)
                .setItems(items.toTypedArray()) { _, which -> actions[which].invoke() }
                .show()
        }
    }

    private fun startProcess(record: FileRecord) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(EXTRA_MEDIA_URI, record.filePath)
        intent.putExtra(EXTRA_MEDIA_TYPE, record.mediaType)
        startActivity(intent)
    }

    private fun openDetail(record: FileRecord) {
        val intent = Intent(this, ResultDetailActivity::class.java)
        intent.putExtra(ResultDetailActivity.EXTRA_PATH, record.filePath)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_MEDIA_URI = "extra_media_uri"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        private const val KEY_SORT_BY = "key_sort_by"
        private const val KEY_SORT_ASC = "key_sort_asc"

        fun open(context: Context) {
            context.startActivity(Intent(context, MediaLibraryActivity::class.java))
        }
    }
}

