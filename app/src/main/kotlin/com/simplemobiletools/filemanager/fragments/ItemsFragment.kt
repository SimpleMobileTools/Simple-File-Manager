package com.simplemobiletools.filemanager.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyScalableRecyclerView
import com.simplemobiletools.filemanager.PATH
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.SCROLL_STATE
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.dialogs.CreateNewItemDialog
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.helpers.RootHelpers
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.items_fragment.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import java.io.File
import java.util.*

class ItemsFragment : Fragment(), ItemsAdapter.ItemOperationsListener {
    private var mListener: ItemInteractionListener? = null
    private var mStoredTextColor = 0
    private var mShowHidden = false
    private var mItems = ArrayList<FileDirItem>()

    lateinit var fragmentView: View

    var mPath = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentView = inflater!!.inflate(R.layout.items_fragment, container, false)!!
        storeConfigVariables()
        return fragmentView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillItems()

        items_swipe_refresh.setOnRefreshListener({ fillItems() })
        items_fab.setOnClickListener { createNewItem() }
    }

    override fun onResume() {
        super.onResume()
        val config = context.config
        if (mShowHidden != config.shouldShowHidden) {
            mShowHidden = !mShowHidden
            fillItems()
        }
        context.updateTextColors(items_holder)
        if (mStoredTextColor != config.textColor) {
            mItems = ArrayList()
            fillItems()
            mStoredTextColor = config.textColor
        }
    }

    override fun onPause() {
        super.onPause()
        storeConfigVariables()
    }

    private fun storeConfigVariables() {
        mShowHidden = context.config.shouldShowHidden
        mStoredTextColor = context.config.textColor
    }

    fun fillItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)
            return

        mPath = arguments.getString(PATH)
        getItems(mPath) {
            if (!isAdded)
                return@getItems

            val newItems = it
            FileDirItem.sorting = context.config.getFolderSorting(mPath)
            newItems.sort()

            fragmentView.apply {
                activity?.runOnUiThread {
                    items_swipe_refresh?.isRefreshing = false
                    if (newItems.hashCode() == mItems.hashCode()) {
                        return@runOnUiThread
                    }

                    mItems = newItems

                    val currAdapter = items_list.adapter
                    if (currAdapter == null) {
                        items_list.apply {
                            this.adapter = ItemsAdapter(activity as SimpleActivity, mItems, this@ItemsFragment) {
                                itemClicked(it)
                            }

                            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                                setDrawable(context.resources.getDrawable(com.simplemobiletools.commons.R.drawable.divider))
                                addItemDecoration(this)
                            }

                            isDragSelectionEnabled = true
                        }
                        items_fastscroller.setViews(items_list, items_swipe_refresh)
                        setupRecyclerViewListener()
                    } else {
                        val state = (items_list.layoutManager as LinearLayoutManager).onSaveInstanceState()
                        (currAdapter as ItemsAdapter).updateItems(mItems)
                        (items_list.layoutManager as LinearLayoutManager).onRestoreInstanceState(state)
                    }

                    getRecyclerLayoutManager().onRestoreInstanceState(arguments.getParcelable<Parcelable>(SCROLL_STATE))
                }
            }
        }
    }

    private fun getRecyclerLayoutManager() = (fragmentView.items_list.layoutManager as LinearLayoutManager)

    private fun setupRecyclerViewListener() {
        fragmentView.items_list.listener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
            override fun zoomIn() {

            }

            override fun zoomOut() {

            }

            override fun selectItem(position: Int) {
                getRecyclerAdapter().selectItem(position)
            }

            override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                getRecyclerAdapter().selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)
            }
        }
    }

    private fun getRecyclerAdapter() = (items_list.adapter as ItemsAdapter)

    fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    fun setListener(listener: ItemInteractionListener) {
        mListener = listener
    }

    private fun getItems(path: String, callback: (items: ArrayList<FileDirItem>) -> Unit) {
        Thread({
            if (!context.config.enableRootAccess || !isPathOnRoot(path)) {
                getRegularItemsOf(path, callback)
            } else {
                getRootItemsOf(path, callback)
            }
        }).start()
    }

    private fun isPathOnRoot(path: String) =
            !(path.startsWith(context.config.internalStoragePath) || (context.hasExternalSDCard() && path.startsWith(context.config.sdCardPath)))

    private fun getRegularItemsOf(path: String, callback: (items: ArrayList<FileDirItem>) -> Unit) {
        val items = ArrayList<FileDirItem>()
        val files = File(path).listFiles()
        if (files != null) {
            for (file in files) {
                val curPath = file.absolutePath
                val curName = curPath.getFilenameFromPath()
                if (!mShowHidden && curName.startsWith("."))
                    continue

                val children = getChildren(file)
                val size = file.length()

                items.add(FileDirItem(curPath, curName, file.isDirectory, children, size))
            }
        }
        callback(items)
    }

    private fun getRootItemsOf(path: String, callback: (items: ArrayList<FileDirItem>) -> Unit) {
        RootHelpers().getFiles(activity as SimpleActivity, path.trimEnd('/'), callback)
    }

    private fun getChildren(file: File): Int {
        val fileList: Array<out String>? = file.list() ?: return 0

        if (file.isDirectory) {
            return if (mShowHidden) {
                fileList!!.size
            } else {
                fileList!!.count { fileName -> fileName[0] != '.' }
            }
        }
        return 0
    }

    fun itemClicked(item: FileDirItem) {
        if (item.isDirectory) {
            mListener?.itemClicked(item)
        } else {
            val path = item.path
            val file = File(path)
            var mimeType: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.getFilenameExtension().toLowerCase())
            if (mimeType == null)
                mimeType = "text/plain"

            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    startActivity(this)
                } catch (e: ActivityNotFoundException) {
                    if (!tryGenericMimeType(this, mimeType!!, file)) {
                        activity.toast(R.string.no_app_found)
                    }
                }
            }
        }
    }

    private fun tryGenericMimeType(intent: Intent, mimeType: String, file: File): Boolean {
        val genericMimeType = getGenericMimeType(mimeType)
        intent.setDataAndType(Uri.fromFile(file), genericMimeType)
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun createNewItem() {
        CreateNewItemDialog(activity as SimpleActivity, mPath) {
            if (it) {
                fillItems()
            }
        }
    }

    private fun getGenericMimeType(mimeType: String): String {
        if (!mimeType.contains("/"))
            return mimeType

        val type = mimeType.substring(0, mimeType.indexOf("/"))
        return "$type/*"
    }

    override fun refreshItems() {
        fillItems()
    }

    override fun deleteFiles(files: ArrayList<File>) {
        val hasFolder = files.any { it.isDirectory }
        if (isPathOnRoot(files.firstOrNull()?.absolutePath ?: context.config.internalStoragePath)) {
            files.forEach {
                RootTools.deleteFileOrDirectory(it.path, false)
            }
        } else {
            (activity as SimpleActivity).deleteFiles(files, hasFolder) {
                if (!it) {
                    activity.runOnUiThread {
                        activity.toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }

    override fun itemLongClicked(position: Int) {
        items_list.setDragSelectActive(position)
    }

    interface ItemInteractionListener {
        fun itemClicked(item: FileDirItem)
    }
}
