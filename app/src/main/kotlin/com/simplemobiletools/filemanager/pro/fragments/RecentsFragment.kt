package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.provider.MediaStore
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.tryOpenPathIntent
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.recents_fragment.view.*
import java.util.*

class RecentsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet), ItemOperationsListener {
    private var activity: SimpleActivity? = null

    override fun setupFragment(activity: SimpleActivity) {
        if (this.activity == null) {
            this.activity = activity
            recents_swipe_refresh.setOnRefreshListener { refreshItems() }
        }

        refreshItems()
    }

    override fun refreshItems() {
        ensureBackgroundThread {
            getRecents { recents ->
                recents_swipe_refresh?.isRefreshing = false
                ItemsAdapter(activity as SimpleActivity, recents, this, recents_list, false, null, recents_swipe_refresh) {
                    activity?.tryOpenPathIntent((it as FileDirItem).path, false)
                }.apply {
                    recents_list.adapter = this
                }

                recents_list.scheduleLayoutAnimation()
            }
        }
    }

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {}

    private fun getRecents(callback: (recents: ArrayList<ListItem>) -> Unit) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 50"
        val cursor = context?.contentResolver?.query(uri, projection, null, null, sortOrder)
        val listItems = arrayListOf<ListItem>()

        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                    val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    val modified = cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED) * 1000
                    val fileDirItem = ListItem(path, name, false, 0, size, modified, false)
                    listItems.add(fileDirItem)
                } while (cursor.moveToNext())
            }
        }

        activity?.runOnUiThread {
            callback(listItems)
        }
    }

    private fun getRecyclerAdapter() = recents_list.adapter as? ItemsAdapter

    override fun toggleFilenameVisibility() {
        context?.config?.displayFilenames = !context!!.config.displayFilenames
        getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
    }

    override fun increaseColumnCount() {
        context?.config?.fileColumnCnt = ++(recents_list.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    override fun reduceColumnCount() {
        context?.config?.fileColumnCnt = --(recents_list.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun columnCountChanged() {
        activity?.invalidateOptionsMenu()
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, listItems.size)
        }
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {}

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun setupFontSize() {}

    override fun setupDateTimeFormat() {}

    override fun searchQueryChanged(text: String) {}

    override fun finishActMode() {
        getRecyclerAdapter()?.finishActMode()
    }
}
