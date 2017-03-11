package com.simplemobiletools.filemanager.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
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
import java.io.File
import java.util.*

class ItemsFragment : android.support.v4.app.Fragment(), ItemsAdapter.ItemOperationsListener {
    private var mListener: ItemInteractionListener? = null
    private var mSnackbar: Snackbar? = null

    lateinit var mItems: List<FileDirItem>
    lateinit var mConfig: Config
    lateinit var mToBeDeleted: MutableList<String>

    private var mShowHidden = false
    var mPath = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.items_fragment, container, false)!!

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mConfig = context.config
        mShowHidden = mConfig.showHidden
        mItems = ArrayList<FileDirItem>()
        mToBeDeleted = ArrayList<String>()
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
    }

    override fun onPause() {
        super.onPause()
        deleteItems()
    }

    private fun fillItems() {
        mPath = arguments.getString(PATH)
        val newItems = getItems(mPath)
        Collections.sort(newItems)
        items_swipe_refresh.isRefreshing = false
        if (newItems.toString() == mItems.toString()) {
            return
        }

        mItems = newItems

        val adapter = ItemsAdapter(activity as SimpleActivity, mItems, this) {
            itemClicked(it)
        }

        val currAdapter = items_list.adapter
        if (currAdapter == null) {
            items_list.apply {
                this@apply.adapter = adapter
                addItemDecoration(RecyclerViewDivider(context))
                setOnTouchListener { view, motionEvent -> checkDelete(); false }
            }
        } else {
            val state = (items_list.layoutManager as LinearLayoutManager).onSaveInstanceState()
            (currAdapter as ItemsAdapter).updateItems(mItems)
            (items_list.layoutManager as LinearLayoutManager).onRestoreInstanceState(state)
        }

        getRecyclerLayoutManager().onRestoreInstanceState(arguments.getParcelable<Parcelable>(SCROLL_STATE))
    }

    fun getRecyclerLayoutManager() = (items_list.layoutManager as LinearLayoutManager)

    fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    fun setListener(listener: ItemInteractionListener) {
        mListener = listener
    }

    private fun getItems(path: String): List<FileDirItem> {
        val items = ArrayList<FileDirItem>()
        val files = File(path).listFiles()
        if (files != null) {
            for (file in files) {
                val curPath = file.absolutePath
                val curName = curPath.getFilenameFromPath()
                if (!mShowHidden && curName.startsWith("."))
                    continue

                if (mToBeDeleted.contains(curPath))
                    continue

                val children = getChildren(file)
                val size = file.length()

                items.add(FileDirItem(curPath, curName, file.isDirectory, children, size))
            }
        }
        return items
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

    override fun prepareForDeleting(paths: ArrayList<String>) {
        activity.toast(R.string.deleting)
        mToBeDeleted = paths
        val deletedCnt = mToBeDeleted.size

        if ((activity as SimpleActivity).isShowingPermDialog(File(mToBeDeleted[0])))
            return

        notifyDeletion(deletedCnt)
    }

    private fun notifyDeletion(cnt: Int) {
        val res = resources
        val msg = res.getQuantityString(R.plurals.items_deleted, cnt, cnt)
        mSnackbar = Snackbar.make(items_holder, msg, Snackbar.LENGTH_INDEFINITE)
        mSnackbar!!.apply {
            setAction(res.getString(R.string.undo), undoDeletion)
            setActionTextColor(Color.WHITE)
            show()
        }
        fillItems()
    }

    fun checkDelete() {
        if (mSnackbar?.isShown == true) {
            deleteItems()
        }
    }

    private fun deleteItems() {
        if (mToBeDeleted.isEmpty())
            return

        mSnackbar?.dismiss()
        mToBeDeleted
                .map(::File)
                .filter(File::exists)
                .forEach { deleteItem(it) }

        mToBeDeleted.clear()
    }

    private fun deleteItem(item: File) {
        if (item.isDirectory) {
            for (child in item.listFiles()) {
                deleteItem(child)
            }
        }

        if (context.needsStupidWritePermissions(item.absolutePath)) {
            val document = context.getFileDocument(item.absolutePath, mConfig.treeUri) ?: return

            // double check we have the uri to the proper file path, not some parent folder
            if (document.uri.toString().endsWith(item.absolutePath.getFilenameFromPath()))
                document.delete()
        } else {
            item.delete()
        }
    }

    private val undoDeletion = View.OnClickListener {
        mToBeDeleted.clear()
        mSnackbar!!.dismiss()
        fillItems()
    }

    override fun refreshItems() {
        fillItems()
    }

    interface ItemInteractionListener {
        fun itemClicked(item: FileDirItem)
    }
}
