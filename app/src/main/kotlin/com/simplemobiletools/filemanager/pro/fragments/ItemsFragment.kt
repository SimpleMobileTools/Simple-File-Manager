package com.simplemobiletools.filemanager.pro.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.StoragePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.Breadcrumbs
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.MainActivity
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.pro.dialogs.CreateNewItemDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.isPathOnRoot
import com.simplemobiletools.filemanager.pro.helpers.MAX_COLUMN_COUNT
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.items_fragment.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ItemsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet), ItemOperationsListener,
    Breadcrumbs.BreadcrumbsListener {
    private var showHidden = false
    private var skipItemUpdating = false
    private var isSearchOpen = false
    private var lastSearchedText = ""
    private var scrollStates = HashMap<String, Parcelable>()
    private var zoomListener: MyRecyclerView.MyZoomListener? = null

    private var storedItems = ArrayList<ListItem>()

    override fun setupFragment(activity: SimpleActivity) {
        if (this.activity == null) {
            this.activity = activity
            items_swipe_refresh.setOnRefreshListener { refreshFragment() }
            items_fab.setOnClickListener { createNewItem() }
            breadcrumbs.listener = this@ItemsFragment
        }
    }

    override fun onResume(textColor: Int, primaryColor: Int) {
        context!!.updateTextColors(this)
        storedItems = ArrayList()
        getRecyclerAdapter()?.apply {
            updatePrimaryColor(primaryColor)
            updateTextColor(textColor)
            initDrawables()
        }

        items_fastscroller.updateColors(context!!.getAdjustedPrimaryColor())

        if (currentPath != "") {
            breadcrumbs.updateColor(textColor)
        }
        items_swipe_refresh.isEnabled = activity?.config?.enablePullToRefresh != false
    }

    override fun setupFontSize() {
        getRecyclerAdapter()?.updateFontSizes()
        if (currentPath != "") {
            breadcrumbs.updateFontSize(context!!.getTextSize(), false)
        }
    }

    override fun setupDateTimeFormat() {
        getRecyclerAdapter()?.updateDateTimeFormat()
    }

    override fun finishActMode() {
        getRecyclerAdapter()?.finishActMode()
    }

    fun openPath(path: String, forceRefresh: Boolean = false) {
        if ((activity as? BaseSimpleActivity)?.isAskingPermissions == true) {
            return
        }

        var realPath = path.trimEnd('/')
        if (realPath.isEmpty()) {
            realPath = "/"
        }

        scrollStates[currentPath] = getScrollState()!!
        currentPath = realPath
        showHidden = context!!.config.shouldShowHidden
        getItems(currentPath) { originalPath, listItems ->
            if (currentPath != originalPath) {
                return@getItems
            }

            FileDirItem.sorting = context!!.config.getFolderSorting(currentPath)
            listItems.sort()

            if (context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_GRID && listItems.none { it.isSectionTitle }) {
                if (listItems.any { it.mIsDirectory } && listItems.any { !it.mIsDirectory }) {
                    val firstFileIndex = listItems.indexOfFirst { !it.mIsDirectory }
                    if (firstFileIndex != -1) {
                        val sectionTitle = ListItem("", "", false, 0, 0, 0, false, true)
                        listItems.add(firstFileIndex, sectionTitle)
                    }
                }
            }

            activity?.runOnUiThread {
                activity?.invalidateOptionsMenu()
                addItems(listItems, forceRefresh)
                if (context != null && currentViewType != context!!.config.getFolderViewType(currentPath)) {
                    setupLayoutManager()
                }
            }
        }
    }

    private fun addItems(items: ArrayList<ListItem>, forceRefresh: Boolean = false) {
        skipItemUpdating = false
        activity?.runOnUiThread {
            items_swipe_refresh?.isRefreshing = false
            breadcrumbs.setBreadcrumb(currentPath, false)
            if (!forceRefresh && items.hashCode() == storedItems.hashCode()) {
                return@runOnUiThread
            }

            storedItems = items
            if (items_list.adapter == null) {
                breadcrumbs.updateFontSize(context!!.getTextSize(), true)
            }

            ItemsAdapter(activity as SimpleActivity, storedItems, this, items_list, isPickMultipleIntent, items_swipe_refresh) {
                if ((it as? ListItem)?.isSectionTitle == true) {
                    openDirectory(it.mPath)
                    searchClosed()
                } else {
                    itemClicked(it as FileDirItem)
                }
            }.apply {
                setupZoomListener(zoomListener)
                items_list.adapter = this
            }

            if (context.areSystemAnimationsEnabled) {
                items_list.scheduleLayoutAnimation()
            }

            getRecyclerLayoutManager().onRestoreInstanceState(scrollStates[currentPath])
        }
    }

    private fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    private fun getRecyclerLayoutManager() = (items_list.layoutManager as MyGridLayoutManager)

    @SuppressLint("NewApi")
    private fun getItems(path: String, callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit) {
        skipItemUpdating = false
        ensureBackgroundThread {
            if (activity?.isDestroyed == false && activity?.isFinishing == false) {
                val config = context!!.config
                if (context.isRestrictedSAFOnlyRoot(path)) {
                    activity?.handleAndroidSAFDialog(path) {
                        if (!it) {
                            activity?.toast(R.string.no_storage_permissions)
                            return@handleAndroidSAFDialog
                        }
                        val getProperChildCount = context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_LIST
                        context.getAndroidSAFFileItems(path, context.config.shouldShowHidden, getProperChildCount) { fileItems ->
                            callback(path, getListItemsFromFileDirItems(fileItems))
                        }
                    }
                } else if (context!!.isPathOnOTG(path) && config.OTGTreeUri.isNotEmpty()) {
                    val getProperFileSize = context!!.config.getFolderSorting(currentPath) and SORT_BY_SIZE != 0
                    context!!.getOTGItems(path, config.shouldShowHidden, getProperFileSize) {
                        callback(path, getListItemsFromFileDirItems(it))
                    }
                } else if (!config.enableRootAccess || !context!!.isPathOnRoot(path)) {
                    getRegularItemsOf(path, callback)
                } else {
                    RootHelpers(activity!!).getFiles(path, callback)
                }
            }
        }
    }

    private fun getRegularItemsOf(path: String, callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit) {
        val items = ArrayList<ListItem>()
        val files = File(path).listFiles()?.filterNotNull()
        if (context == null || files == null) {
            callback(path, items)
            return
        }

        val isSortingBySize = context!!.config.getFolderSorting(currentPath) and SORT_BY_SIZE != 0
        val getProperChildCount = context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_LIST
        val lastModifieds = context!!.getFolderLastModifieds(path)

        for (file in files) {
            val fileDirItem = getFileDirItemFromFile(file, isSortingBySize, lastModifieds, false)
            if (fileDirItem != null) {
                items.add(fileDirItem)
            }
        }

        // send out the initial item list asap, get proper child count asynchronously as it can be slow
        callback(path, items)

        if (getProperChildCount) {
            items.filter { it.mIsDirectory }.forEach {
                if (context != null) {
                    val childrenCount = it.getDirectChildrenCount(activity as BaseSimpleActivity, showHidden)
                    if (childrenCount != 0) {
                        activity?.runOnUiThread {
                            getRecyclerAdapter()?.updateChildCount(it.mPath, childrenCount)
                        }
                    }
                }
            }
        }
    }

    private fun getFileDirItemFromFile(file: File, isSortingBySize: Boolean, lastModifieds: HashMap<String, Long>, getProperChildCount: Boolean): ListItem? {
        val curPath = file.absolutePath
        val curName = file.name
        if (!showHidden && curName.startsWith(".")) {
            return null
        }

        var lastModified = lastModifieds.remove(curPath)
        val isDirectory = if (lastModified != null) false else file.isDirectory
        val children = if (isDirectory && getProperChildCount) file.getDirectChildrenCount(context, showHidden) else 0
        val size = if (isDirectory) {
            if (isSortingBySize) {
                file.getProperSize(showHidden)
            } else {
                0L
            }
        } else {
            file.length()
        }

        if (lastModified == null) {
            lastModified = file.lastModified()
        }

        return ListItem(curPath, curName, isDirectory, children, size, lastModified, false, false)
    }

    private fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        fileDirItems.forEach {
            val listItem = ListItem(it.path, it.name, it.isDirectory, it.children, it.size, it.modified, false, false)
            listItems.add(listItem)
        }
        return listItems
    }

    private fun itemClicked(item: FileDirItem) {
        if (item.isDirectory) {
            openDirectory(item.path)
        } else {
            clickedPath(item.path)
        }
    }

    private fun openDirectory(path: String) {
        (activity as? MainActivity)?.apply {
            skipItemUpdating = isSearchOpen
            openedDirectory()
        }
        openPath(path)
    }

    override fun searchQueryChanged(text: String) {
        val searchText = text.trim()
        lastSearchedText = searchText
        if (context == null) {
            return
        }

        when {
            searchText.isEmpty() -> {
                items_fastscroller.beVisible()
                getRecyclerAdapter()?.updateItems(storedItems)
                items_placeholder.beGone()
                items_placeholder_2.beGone()
            }
            searchText.length == 1 -> {
                items_fastscroller.beGone()
                items_placeholder.beVisible()
                items_placeholder_2.beVisible()
            }
            else -> {
                ensureBackgroundThread {
                    val files = searchFiles(searchText, currentPath)
                    files.sortBy { it.getParentPath() }

                    if (lastSearchedText != searchText) {
                        return@ensureBackgroundThread
                    }

                    val listItems = ArrayList<ListItem>()

                    var previousParent = ""
                    files.forEach {
                        val parent = it.mPath.getParentPath()
                        if (!it.isDirectory && parent != previousParent && context != null) {
                            val sectionTitle = ListItem(parent, context!!.humanizePath(parent), false, 0, 0, 0, true, false)
                            listItems.add(sectionTitle)
                            previousParent = parent
                        }

                        if (it.isDirectory) {
                            val sectionTitle = ListItem(it.path, context!!.humanizePath(it.path), true, 0, 0, 0, true, false)
                            listItems.add(sectionTitle)
                            previousParent = parent
                        }

                        if (!it.isDirectory) {
                            listItems.add(it)
                        }
                    }

                    activity?.runOnUiThread {
                        getRecyclerAdapter()?.updateItems(listItems, text)
                        items_fastscroller.beVisibleIf(listItems.isNotEmpty())
                        items_placeholder.beVisibleIf(listItems.isEmpty())
                        items_placeholder_2.beGone()
                    }
                }
            }
        }
    }

    private fun searchFiles(text: String, path: String): ArrayList<ListItem> {
        val files = ArrayList<ListItem>()
        if (context == null) {
            return files
        }

        val sorting = context!!.config.getFolderSorting(path)
        FileDirItem.sorting = context!!.config.getFolderSorting(currentPath)
        val isSortingBySize = sorting and SORT_BY_SIZE != 0
        File(path).listFiles()?.sortedBy { it.isDirectory }?.forEach {
            if (!showHidden && it.isHidden) {
                return@forEach
            }

            if (it.isDirectory) {
                if (it.name.contains(text, true)) {
                    val fileDirItem = getFileDirItemFromFile(it, isSortingBySize, HashMap<String, Long>(), false)
                    if (fileDirItem != null) {
                        files.add(fileDirItem)
                    }
                }

                files.addAll(searchFiles(text, it.absolutePath))
            } else {
                if (it.name.contains(text, true)) {
                    val fileDirItem = getFileDirItemFromFile(it, isSortingBySize, HashMap<String, Long>(), false)
                    if (fileDirItem != null) {
                        files.add(fileDirItem)
                    }
                }
            }
        }
        return files
    }

    fun searchOpened() {
        isSearchOpen = true
        lastSearchedText = ""
        items_swipe_refresh.isEnabled = false
    }

    fun searchClosed() {
        isSearchOpen = false
        if (!skipItemUpdating) {
            getRecyclerAdapter()?.updateItems(storedItems)
        }

        skipItemUpdating = false
        lastSearchedText = ""

        items_swipe_refresh.isEnabled = activity?.config?.enablePullToRefresh != false
        items_fastscroller.beVisible()
        items_placeholder.beGone()
        items_placeholder_2.beGone()
    }

    private fun createNewItem() {
        CreateNewItemDialog(activity as SimpleActivity, currentPath) {
            if (it) {
                refreshFragment()
            } else {
                activity?.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun getRecyclerAdapter() = items_list.adapter as? ItemsAdapter

    private fun setupLayoutManager() {
        if (context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_GRID) {
            currentViewType = VIEW_TYPE_GRID
            setupGridLayoutManager()
        } else {
            currentViewType = VIEW_TYPE_LIST
            setupListLayoutManager()
        }

        items_list.adapter = null
        initZoomListener()
        addItems(storedItems, true)
    }

    private fun setupGridLayoutManager() {
        val layoutManager = items_list.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = context?.config?.fileColumnCnt ?: 3

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getRecyclerAdapter()?.isASectionTitle(position) == true || getRecyclerAdapter()?.isGridTypeDivider(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = items_list.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        zoomListener = null
    }

    private fun initZoomListener() {
        if (context?.config?.getFolderViewType(currentPath) == VIEW_TYPE_GRID) {
            val layoutManager = items_list.layoutManager as MyGridLayoutManager
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

    override fun increaseColumnCount() {
        if (currentViewType == VIEW_TYPE_GRID) {
            context?.config?.fileColumnCnt = ++(items_list.layoutManager as MyGridLayoutManager).spanCount
            columnCountChanged()
        }
    }

    override fun reduceColumnCount() {
        if (currentViewType == VIEW_TYPE_GRID) {
            context?.config?.fileColumnCnt = --(items_list.layoutManager as MyGridLayoutManager).spanCount
            columnCountChanged()
        }
    }

    private fun columnCountChanged() {
        activity?.invalidateOptionsMenu()
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, listItems.size)
        }
    }

    override fun toggleFilenameVisibility() {
        getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
    }

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(activity as SimpleActivity, currentPath, context!!.config.enableRootAccess, true) {
                getRecyclerAdapter()?.finishActMode()
                openPath(it)
            }
        } else {
            val item = breadcrumbs.getItem(id)
            openPath(item.path)
        }
    }

    override fun refreshFragment() {
        openPath(currentPath)
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {
        val hasFolder = files.any { it.isDirectory }
        val firstPath = files.firstOrNull()?.path
        if (firstPath == null || firstPath.isEmpty() || context == null) {
            return
        }

        if (context!!.isPathOnRoot(firstPath)) {
            RootHelpers(activity!!).deleteFiles(files)
        } else {
            (activity as SimpleActivity).deleteFiles(files, hasFolder) {
                if (!it) {
                    activity!!.runOnUiThread {
                        activity!!.toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }

    override fun selectedPaths(paths: ArrayList<String>) {
        (activity as MainActivity).pickedPaths(paths)
    }
}
