package com.simplemobiletools.filemanager.pro.activities

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
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
import java.util.Locale

class MimeTypesActivity : SimpleActivity(), ItemOperationsListener {
    private var isSearchOpen = false
    private var currentMimeType = ""
    private var lastSearchedText = ""
    private var searchMenuItem: MenuItem? = null
    private var zoomListener: MyRecyclerView.MyZoomListener? = null
    private var storedItems = ArrayList<ListItem>()
    private var currentViewType = VIEW_TYPE_LIST

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mimetypes)
        setupOptionsMenu()
        refreshMenuItems()
        updateMaterialActivityViews(mimetypes_coordinator, mimetypes_list, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(mimetypes_list, mimetypes_toolbar)

        currentMimeType = intent.getStringExtra(SHOW_MIMETYPE) ?: return
        mimetypes_toolbar.title = getString(
            when (currentMimeType) {
                IMAGES -> R.string.images
                VIDEOS -> R.string.videos
                AUDIO -> R.string.audio
                DOCUMENTS -> R.string.documents
                DOWNLOADS -> R.string.archives
                APPS -> R.string.apps
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

        mimetypes_fastscroller.updateColors(getProperPrimaryColor())
        mimetypes_placeholder.setTextColor(getProperTextColor())
        mimetypes_placeholder_2.setTextColor(getProperTextColor())
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(mimetypes_toolbar, NavigationIcon.Arrow, searchMenuItem = searchMenuItem)
    }

    private fun refreshMenuItems() {
        val currentViewType = config.getFolderViewType(currentMimeType)

        mimetypes_toolbar.menu.apply {
            findItem(R.id.toggle_filename).isVisible = currentViewType == VIEW_TYPE_GRID

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden()
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.column_count).isVisible = currentViewType == VIEW_TYPE_GRID
        }
    }

    private fun setupOptionsMenu() {
        setupSearch(mimetypes_toolbar.menu)
        mimetypes_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog()
                R.id.toggle_filename -> toggleFilenameVisibility()
                R.id.change_view_type -> changeViewType()
                R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
                R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
                R.id.column_count -> changeColumnCount()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun refreshFragment() {
        reFetchItems()
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {
        deleteFiles(files, false) {
            if (!it) {
                runOnUiThread {
                    toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    override fun selectedPaths(paths: ArrayList<String>) {}

    fun searchQueryChanged(text: String) {
        val searchText = text.trim()
        lastSearchedText = searchText
        when {
            searchText.isEmpty() -> {
                mimetypes_fastscroller.beVisible()
                getRecyclerAdapter()?.updateItems(storedItems)
                mimetypes_placeholder.beGoneIf(storedItems.isNotEmpty())
                mimetypes_placeholder_2.beGone()
            }
            searchText.length == 1 -> {
                mimetypes_fastscroller.beGone()
                mimetypes_placeholder.beVisible()
                mimetypes_placeholder_2.beVisible()
            }
            else -> {
                ensureBackgroundThread {
                    if (lastSearchedText != searchText) {
                        return@ensureBackgroundThread
                    }

                    val listItems = storedItems.filter { it.name.contains(searchText, true) } as ArrayList<ListItem>

                    runOnUiThread {
                        getRecyclerAdapter()?.updateItems(listItems, text)
                        mimetypes_fastscroller.beVisibleIf(listItems.isNotEmpty())
                        mimetypes_placeholder.beVisibleIf(listItems.isEmpty())
                        mimetypes_placeholder_2.beGone()
                    }
                }
            }
        }
    }

    override fun setupDateTimeFormat() {}

    override fun setupFontSize() {}

    override fun toggleFilenameVisibility() {
        config.displayFilenames = !config.displayFilenames
        getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
    }

    private fun changeColumnCount() {
        val items = ArrayList<RadioItem>()
        for (i in 1..MAX_COLUMN_COUNT) {
            items.add(RadioItem(i, resources.getQuantityString(R.plurals.column_counts, i, i)))
        }

        val currentColumnCount = config.fileColumnCnt
        RadioGroupDialog(this, items, config.fileColumnCnt) {
            val newColumnCount = it as Int
            if (currentColumnCount != newColumnCount) {
                config.fileColumnCnt = newColumnCount
                columnCountChanged()
            }
        }
    }

    fun increaseColumnCount() {
        if (currentViewType == VIEW_TYPE_GRID) {
            config.fileColumnCnt += 1
            columnCountChanged()
        }
    }

    fun reduceColumnCount() {
        if (currentViewType == VIEW_TYPE_GRID) {
            config.fileColumnCnt -= 1
            columnCountChanged()
        }
    }

    override fun columnCountChanged() {
        (mimetypes_list.layoutManager as MyGridLayoutManager).spanCount = config.fileColumnCnt
        refreshMenuItems()
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, listItems.size)
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
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearchOpen = true
                searchOpened()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchOpen = false
                searchClosed()
                return true
            }
        })
    }

    fun searchOpened() {
        isSearchOpen = true
        lastSearchedText = ""
    }

    fun searchClosed() {
        isSearchOpen = false
        lastSearchedText = ""
        searchQueryChanged("")
    }

    private fun getProperFileDirItems(callback: (ArrayList<FileDirItem>) -> Unit) {
        val fileDirItems = ArrayList<FileDirItem>()
        val showHidden = config.shouldShowHidden()
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

                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    if (size == 0L) {
                        return@queryCursor
                    }

                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
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
                        DOWNLOADS -> {
                            if (downloadsMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        APPS -> {
                            if (mimetype != "image" && mimetype != "video" && mimetype != "audio" && mimetype != "text" &&
                                !extraAudioMimeTypes.contains(fullMimetype) && !extraDocumentMimeTypes.contains(fullMimetype) &&
                                !downloadsMimeTypes.contains(fullMimetype)
                            ) {
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

    private fun addItems(items: ArrayList<ListItem>) {
        FileDirItem.sorting = config.getFolderSorting(currentMimeType)
        items.sort()

        if (isDestroyed || isFinishing) {
            return
        }

        storedItems = items
        ItemsAdapter(this as SimpleActivity, storedItems, this, mimetypes_list, false, null) {
            tryOpenPathIntent((it as ListItem).path, false)
        }.apply {
            setupZoomListener(zoomListener)
            mimetypes_list.adapter = this
        }

        if (areSystemAnimationsEnabled) {
            mimetypes_list.scheduleLayoutAnimation()
        }

        mimetypes_placeholder.beVisibleIf(items.isEmpty())
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
            setupLayoutManager()
            refreshMenuItems()
        }
    }

    private fun reFetchItems() {
        getProperFileDirItems { fileDirItems ->
            val listItems = getListItemsFromFileDirItems(fileDirItems)

            runOnUiThread {
                addItems(listItems)
                if (currentViewType != config.getFolderViewType(currentMimeType)) {
                    setupLayoutManager()
                }
            }
        }
    }

    private fun recreateList() {
        val listItems = getRecyclerAdapter()?.listItems
        if (listItems != null) {
            addItems(listItems as ArrayList<ListItem>)
        }
    }

    private fun setupLayoutManager() {
        if (config.getFolderViewType(currentMimeType) == VIEW_TYPE_GRID) {
            currentViewType = VIEW_TYPE_GRID
            setupGridLayoutManager()
        } else {
            currentViewType = VIEW_TYPE_LIST
            setupListLayoutManager()
        }

        mimetypes_list.adapter = null
        initZoomListener()
        addItems(storedItems)
    }

    private fun setupGridLayoutManager() {
        val layoutManager = mimetypes_list.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = config.fileColumnCnt ?: 3

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getRecyclerAdapter()?.isASectionTitle(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = mimetypes_list.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        zoomListener = null
    }

    private fun initZoomListener() {
        if (config.getFolderViewType(currentMimeType) == VIEW_TYPE_GRID) {
            val layoutManager = mimetypes_list.layoutManager as MyGridLayoutManager
            zoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                        increaseColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }
            }
        } else {
            zoomListener = null
        }
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
        ensureBackgroundThread {
            reFetchItems()
        }
    }
}
