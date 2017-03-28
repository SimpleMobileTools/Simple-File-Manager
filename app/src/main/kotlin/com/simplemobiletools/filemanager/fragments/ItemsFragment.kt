package com.simplemobiletools.filemanager.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.RecyclerViewDivider
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.PATH
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.SCROLL_STATE
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.dialogs.CreateNewItemDialog
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.items_fragment.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import java.io.File
import java.util.*

class ItemsFragment : android.support.v4.app.Fragment(), ItemsAdapter.ItemOperationsListener {
    private var mListener: ItemInteractionListener? = null
    private var mStoredTextColor = 0

    lateinit var mItems: ArrayList<FileDirItem>
    lateinit var mConfig: Config
    lateinit var fragmentView: View

    private var mShowHidden = false
    var mPath = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentView = inflater!!.inflate(R.layout.items_fragment, container, false)!!
        return fragmentView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mConfig = context.config
        mShowHidden = mConfig.showHidden
        mItems = ArrayList<FileDirItem>()
        fillItems()

        items_swipe_refresh.setOnRefreshListener({ fillItems() })
        items_fab.setOnClickListener { createNewItem() }
    }

    override fun onResume() {
        super.onResume()
        if (mShowHidden != mConfig.showHidden) {
            mShowHidden = !mShowHidden
            fillItems()
        }
        context.updateTextColors(items_holder)
        if (mStoredTextColor != context.config.textColor) {
            mItems = ArrayList<FileDirItem>()
            fillItems()
            mStoredTextColor = context.config.textColor
        }
    }

    override fun onPause() {
        super.onPause()
        mStoredTextColor = context.config.textColor
    }

    fun fillItems() {
        mPath = arguments.getString(PATH)
        getItems(mPath) {
            val newItems = it
            FileDirItem.sorting = mConfig.getFolderSorting(mPath)
            newItems.sort()

            fragmentView.apply {
                activity?.runOnUiThread {
                    items_swipe_refresh.isRefreshing = false
                    if (newItems.hashCode() == mItems.hashCode()) {
                        return@runOnUiThread
                    }

                    mItems = newItems

                    val adapter = ItemsAdapter(activity as SimpleActivity, mItems, this@ItemsFragment) {
                        itemClicked(it)
                    }

                    val currAdapter = items_list.adapter
                    if (currAdapter == null) {
                        items_list.apply {
                            this.adapter = adapter
                            addItemDecoration(RecyclerViewDivider(context))
                        }
                        items_fastscroller.setViews(items_list, items_swipe_refresh)
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

    fun getRecyclerLayoutManager() = (fragmentView.items_list.layoutManager as LinearLayoutManager)

    fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    fun setListener(listener: ItemInteractionListener) {
        mListener = listener
    }

    private fun getItems(path: String, callback: (items: ArrayList<FileDirItem>) -> Unit) {
        Thread({
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
        }).start()
    }

    private fun getChildren(file: File): Int {
        if (file.listFiles() == null)
            return 0

        if (file.isDirectory) {
            return if (mShowHidden) {
                file.listFiles().size
            } else {
                file.listFiles { file -> !file.isHidden }.size
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
                        context.toast(R.string.no_app_found)
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
        CreateNewItemDialog(activity, mPath) {
            fillItems()
        }
    }

    private fun getGenericMimeType(mimeType: String): String {
        if (!mimeType.contains("/"))
            return mimeType

        val type = mimeType.substring(0, mimeType.indexOf("/"))
        return "$type/*"
    }

    override fun deleteFiles(files: ArrayList<File>) {
        val hasFolder = files.any { it.isDirectory }
        (activity as SimpleActivity).deleteFiles(files, hasFolder) {
            if (!it) {
                activity.runOnUiThread {
                    activity.toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    override fun refreshItems() {
        fillItems()
    }

    interface ItemInteractionListener {
        fun itemClicked(item: FileDirItem)
    }
}
