package com.simplemobiletools.filemanager.pro.activities

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.filemanager.pro.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.tryOpenPathIntent
import com.simplemobiletools.filemanager.pro.helpers.*
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.activity_mimetypes.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import java.util.*

class MimeTypesActivity : SimpleActivity(), ItemOperationsListener {
    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null
    private var currentMimeType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mimetypes)
        currentMimeType = intent.getStringExtra(SHOW_MIMETYPE) ?: return
        title = getString(
            when (currentMimeType) {
                IMAGES -> R.string.images
                VIDEOS -> R.string.videos
                AUDIO -> R.string.audio
                DOCUMENTS -> R.string.documents
                ARCHIVES -> R.string.archives
                OTHERS -> R.string.others
                else -> {
                    toast(R.string.unknown_error_occurred)
                    finish()
                    return
                }
            }
        )

        ensureBackgroundThread {
            reFetchItems()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        setupSearch(menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val currentViewType = config.getFolderViewType(currentMimeType)

        menu!!.apply {
            findItem(R.id.add_favorite).isVisible = false
            findItem(R.id.remove_favorite).isVisible = false
            findItem(R.id.go_to_favorite).isVisible = false

            findItem(R.id.toggle_filename).isVisible = currentViewType == VIEW_TYPE_GRID
            findItem(R.id.go_home).isVisible = false
            findItem(R.id.set_as_home).isVisible = false
            findItem(R.id.settings).isVisible = false
            findItem(R.id.about).isVisible = false

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.increase_column_count).isVisible = currentViewType == VIEW_TYPE_GRID && config.fileColumnCnt < MAX_COLUMN_COUNT
            findItem(R.id.reduce_column_count).isVisible = currentViewType == VIEW_TYPE_GRID && config.fileColumnCnt > 1
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.toggle_filename -> toggleFilenameVisibility()
            R.id.change_view_type -> changeViewType()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun refreshFragment() {
        recreateList()
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {}

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun searchQueryChanged(text: String) {}

    override fun setupDateTimeFormat() {}

    override fun setupFontSize() {}

    override fun toggleFilenameVisibility() {
        config.displayFilenames = !config.displayFilenames
        getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
    }

    override fun increaseColumnCount() {
        if (config.getFolderViewType(currentMimeType) == VIEW_TYPE_GRID) {
            config.fileColumnCnt = ++(mimetypes_list.layoutManager as MyGridLayoutManager).spanCount
            columnCountChanged()
        }
    }

    override fun reduceColumnCount() {
        if (config.getFolderViewType(currentMimeType) == VIEW_TYPE_GRID) {
            config.fileColumnCnt = --(mimetypes_list.layoutManager as MyGridLayoutManager).spanCount
            columnCountChanged()
        }
    }

    override fun finishActMode() {}

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(R.string.search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchOpen = false
                return true
            }
        })
    }

    private fun getProperFileDirItems(callback: (ArrayList<FileDirItem>) -> Unit) {
        val fileDirItems = ArrayList<FileDirItem>()
        val showHidden = config.shouldShowHidden
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        try {
            queryCursor(uri, projection) { cursor ->
                try {
                    val fullMimetype = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault()) ?: return@queryCursor
                    val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    if (!showHidden && name.startsWith(".")) {
                        return@queryCursor
                    }

                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    val lastModified = cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED) * 1000

                    val mimetype = fullMimetype.substringBefore("/")
                    when (currentMimeType) {
                        IMAGES -> {
                            if (mimetype == "image") {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        VIDEOS -> {
                            if (mimetype == "video") {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        AUDIO -> {
                            if (mimetype == "audio" || extraAudioMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        DOCUMENTS -> {
                            if (mimetype == "text" || extraDocumentMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        ARCHIVES -> {
                            if (archiveMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        callback(fileDirItems)
    }

    private fun setupAdapter(listItems: ArrayList<ListItem>) {
        FileDirItem.sorting = config.getFolderSorting(currentMimeType)
        listItems.sort()

        runOnUiThread {
            ItemsAdapter(this as SimpleActivity, listItems, this, mimetypes_list, false, items_fastscroller, null) {
                tryOpenPathIntent((it as ListItem).path, false)
            }.apply {
                mimetypes_list.adapter = this
            }

            if (areSystemAnimationsEnabled) {
                mimetypes_list.scheduleLayoutAnimation()
            }

            val dateFormat = config.dateFormat
            val timeFormat = getTimeFormat()
            items_fastscroller.setViews(mimetypes_list) {
                val listItem = getRecyclerAdapter()?.listItems?.getOrNull(it)
                items_fastscroller.updateBubbleText(listItem?.getBubbleText(this, dateFormat, timeFormat) ?: "")
            }
        }
    }

    private fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        fileDirItems.forEach {
            val listItem = ListItem(it.path, it.name, false, 0, it.size, it.modified, false)
            listItems.add(listItem)
        }
        return listItems
    }

    private fun getRecyclerAdapter() = mimetypes_list.adapter as? ItemsAdapter

    private fun showSortingDialog() {
        ChangeSortingDialog(this, currentMimeType) {
            recreateList()
        }
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(this, currentMimeType, true) {
            recreateList()
        }
    }

    private fun reFetchItems() {
        getProperFileDirItems { fileDirItems ->
            val listItems = getListItemsFromFileDirItems(fileDirItems)
            setupAdapter(listItems)
        }
    }

    private fun recreateList() {
        val listItems = getRecyclerAdapter()?.listItems
        if (listItems != null) {
            setupAdapter(listItems as ArrayList<ListItem>)
        }
    }

    private fun columnCountChanged() {
        invalidateOptionsMenu()
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, listItems.size)
            calculateContentHeight(listItems)
        }
    }

    private fun calculateContentHeight(items: MutableList<ListItem>) {
        val layoutManager = mimetypes_list.layoutManager as MyGridLayoutManager
        val thumbnailHeight = layoutManager.getChildAt(0)?.height ?: 0
        val fullHeight = ((items.size - 1) / layoutManager.spanCount + 1) * thumbnailHeight
        items_fastscroller.setContentHeight(fullHeight)
        items_fastscroller.setScrollToY(mimetypes_list.computeVerticalScrollOffset())
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
        } else {
            handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        config.temporarilyShowHidden = show
        reFetchItems()
    }
}
