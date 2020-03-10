package com.simplemobiletools.filemanager.pro.fragments

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.StoragePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.Breadcrumbs
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.MainActivity
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.pro.dialogs.CreateNewItemDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.isPathOnRoot
import com.simplemobiletools.filemanager.pro.extensions.tryOpenPathIntent
import com.simplemobiletools.filemanager.pro.helpers.PATH
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.items_fragment.view.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ItemsFragment : Fragment(), ItemOperationsListener, Breadcrumbs.BreadcrumbsListener {
    var currentPath = ""
    var isGetContentIntent = false
    var isGetRingtonePicker = false
    var isPickMultipleIntent = false

    private var isFirstResume = true
    private var showHidden = false
    private var skipItemUpdating = false
    private var isSearchOpen = false
    private var lastSearchedText = ""
    private var scrollStates = HashMap<String, Parcelable>()

    private var storedItems = ArrayList<ListItem>()
    private var storedTextColor = 0
    private var storedFontSize = 0

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mView = inflater.inflate(R.layout.items_fragment, container, false)!!
        storeStateVariables()
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mView.apply {
            items_swipe_refresh.setOnRefreshListener { refreshItems() }
            items_fab.setOnClickListener { createNewItem() }
            breadcrumbs.listener = this@ItemsFragment
            breadcrumbs.updateFontSize(context!!.getTextSize())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH, currentPath)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            currentPath = savedInstanceState.getString(PATH)!!
            storedItems.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        context!!.updateTextColors(mView as ViewGroup)
        mView.items_fastscroller.updatePrimaryColor()
        val newTextColor = context!!.config.textColor
        if (storedTextColor != newTextColor) {
            storedItems = ArrayList()
            getRecyclerAdapter()?.apply {
                updateTextColor(newTextColor)
                initDrawables()
            }
            mView.breadcrumbs.updateColor(newTextColor)
            storedTextColor = newTextColor
        }

        val configFontSize = context!!.config.fontSize
        if (storedFontSize != configFontSize) {
            getRecyclerAdapter()?.updateFontSizes()
            storedFontSize = configFontSize
            mView.breadcrumbs.updateFontSize(context!!.getTextSize())
        }

        mView.items_fastscroller.updateBubbleColors()
        mView.items_fastscroller.allowBubbleDisplay = true
        if (!isFirstResume) {
            refreshItems()
        }
        getRecyclerAdapter()?.adjustedPrimaryColor = context!!.getAdjustedPrimaryColor()
        isFirstResume = false
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            storedTextColor = textColor
            storedFontSize = fontSize
        }
    }

    fun openPath(path: String, forceRefresh: Boolean = false) {
        if (!isAdded || (activity as? BaseSimpleActivity)?.isAskingPermissions == true) {
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
            if (currentPath != originalPath || !isAdded) {
                return@getItems
            }

            FileDirItem.sorting = context!!.config.getFolderSorting(currentPath)
            listItems.sort()
            activity?.runOnUiThread {
                addItems(listItems, forceRefresh)
            }
        }
    }

    private fun addItems(items: ArrayList<ListItem>, forceRefresh: Boolean = false) {
        skipItemUpdating = false
        mView.apply {
            activity?.runOnUiThread {
                items_swipe_refresh?.isRefreshing = false
                mView.breadcrumbs.setBreadcrumb(currentPath)
                if (!forceRefresh && items.hashCode() == storedItems.hashCode()) {
                    return@runOnUiThread
                }

                storedItems = items
                ItemsAdapter(activity as SimpleActivity, storedItems, this@ItemsFragment, items_list, isPickMultipleIntent, items_fastscroller) {
                    itemClicked(it as FileDirItem)
                }.apply {
                    addVerticalDividers(true)
                    items_list.adapter = this
                }
                items_fastscroller.allowBubbleDisplay = true
                items_fastscroller.setViews(items_list, mView.items_swipe_refresh) {
                    items_fastscroller.updateBubbleText(storedItems.getOrNull(it)?.getBubbleText(context) ?: "")
                }

                getRecyclerLayoutManager().onRestoreInstanceState(scrollStates[currentPath])
                items_list.onGlobalLayout {
                    items_fastscroller.setScrollToY(items_list.computeVerticalScrollOffset())
                }
            }
        }
    }

    fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    private fun getRecyclerLayoutManager() = (mView.items_list.layoutManager as MyLinearLayoutManager)

    private fun getItems(path: String, callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit) {
        skipItemUpdating = false
        ensureBackgroundThread {
            if (activity?.isDestroyed == false && activity?.isFinishing == false) {
                val config = context!!.config
                if (context!!.isPathOnOTG(path) && config.OTGTreeUri.isNotEmpty()) {
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
        if (context == null) {
            callback(path, items)
            return
        }

        val isSortingBySize = context!!.config.getFolderSorting(currentPath) and SORT_BY_SIZE != 0
        if (files != null) {
            for (file in files) {
                val fileDirItem = getFileDirItemFromFile(file, isSortingBySize)
                if (fileDirItem != null) {
                    items.add(fileDirItem)
                }
            }
        }

        callback(path, items)
    }

    private fun getFileDirItemFromFile(file: File, isSortingBySize: Boolean): ListItem? {
        val curPath = file.absolutePath
        val curName = file.name
        if (!showHidden && curName.startsWith(".")) {
            return null
        }

        val isDirectory = file.isDirectory
        val children = if (isDirectory) file.getDirectChildrenCount(showHidden) else 0
        val size = if (isDirectory) {
            if (isSortingBySize) {
                file.getProperSize(showHidden)
            } else {
                0L
            }
        } else {
            file.length()
        }

        return ListItem(curPath, curName, isDirectory, children, size, file.lastModified(), false)
    }

    private fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        fileDirItems.forEach {
            val listItem = ListItem(it.path, it.name, it.isDirectory, it.children, it.size, it.modified, false)
            listItems.add(listItem)
        }
        return listItems
    }

    private fun itemClicked(item: FileDirItem) {
        if (item.isDirectory) {
            (activity as? MainActivity)?.apply {
                skipItemUpdating = isSearchOpen
                openedDirectory()
            }
            openPath(item.path)
        } else {
            val path = item.path
            if (isGetContentIntent) {
                (activity as MainActivity).pickedPath(path)
            } else if (isGetRingtonePicker) {
                if (path.isAudioFast()) {
                    (activity as MainActivity).pickedRingtone(path)
                } else {
                    activity?.toast(R.string.select_audio_file)
                }
            } else {
                activity!!.tryOpenPathIntent(path, false)
            }
        }
    }

    fun searchQueryChanged(text: String) {
        val searchText = text.trim()
        lastSearchedText = searchText
        ensureBackgroundThread {
            if (context == null) {
                return@ensureBackgroundThread
            }

            when {
                searchText.isEmpty() -> activity?.runOnUiThread {
                    mView.apply {
                        items_list.beVisible()
                        getRecyclerAdapter()?.updateItems(storedItems)
                        items_placeholder.beGone()
                        items_placeholder_2.beGone()
                    }
                }
                searchText.length == 1 -> activity?.runOnUiThread {
                    mView.apply {
                        items_list.beGone()
                        items_placeholder.beVisible()
                        items_placeholder_2.beVisible()
                    }
                }
                else -> {
                    val files = searchFiles(searchText, currentPath)
                    if (lastSearchedText != searchText) {
                        return@ensureBackgroundThread
                    }

                    val listItems = ArrayList<ListItem>()

                    var previousParent = ""
                    files.forEach {
                        val parent = it.mPath.getParentPath()
                        if (parent != previousParent && context != null) {
                            listItems.add(ListItem("", context!!.humanizePath(parent), false, 0, 0, 0, true))
                            previousParent = parent
                        }
                        listItems.add(it)
                    }

                    activity?.runOnUiThread {
                        getRecyclerAdapter()?.updateItems(listItems, text)
                        mView.apply {
                            items_list.beVisibleIf(listItems.isNotEmpty())
                            items_placeholder.beVisibleIf(listItems.isEmpty())
                            items_placeholder_2.beGone()
                        }
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
        File(path).listFiles()?.forEach {
            if (it.isDirectory) {
                files.addAll(searchFiles(text, it.absolutePath))
            } else {
                if (it.name.contains(text, true)) {
                    val fileDirItem = getFileDirItemFromFile(it, isSortingBySize)
                    if (fileDirItem != null) {
                        files.add(fileDirItem)
                    }
                }
            }
        }
        files.sort()
        return files
    }

    fun searchOpened() {
        isSearchOpen = true
        lastSearchedText = ""
    }

    fun searchClosed() {
        isSearchOpen = false
        if (!skipItemUpdating) {
            getRecyclerAdapter()?.updateItems(storedItems)
        }
        skipItemUpdating = false
        lastSearchedText = ""
    }

    private fun createNewItem() {
        CreateNewItemDialog(activity as SimpleActivity, currentPath) {
            if (it) {
                refreshItems()
            } else {
                activity?.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun getRecyclerAdapter() = mView.items_list.adapter as? ItemsAdapter

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(activity as SimpleActivity, currentPath, true) {
                getRecyclerAdapter()?.finishActMode()
                openPath(it)
            }
        } else {
            val item = mView.breadcrumbs.getChildAt(id).tag as FileDirItem
            openPath(item.path)
        }
    }

    override fun refreshItems() {
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
