package com.simplemobiletools.filemanager.dialogs

import android.content.Context
import android.content.res.Resources
import android.support.v7.app.AlertDialog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.formatSize
import com.simplemobiletools.filepicker.models.FileDirItem
import kotlinx.android.synthetic.main.item_properties.view.*
import kotlinx.android.synthetic.main.property_item.view.*
import java.io.File
import java.util.*

class PropertiesDialog() {
    lateinit var mContext: Context
    lateinit var mItem: FileDirItem
    lateinit var mInflater: LayoutInflater
    lateinit var mPropertyView: ViewGroup
    lateinit var mResources: Resources

    private var mCountHiddenItems = false
    private var mFilesCnt = 0

    constructor(context: Context, item: FileDirItem, countHiddenItems: Boolean = false) : this() {
        mContext = context
        mItem = item
        mCountHiddenItems = countHiddenItems
        mInflater = LayoutInflater.from(context)
        mResources = mContext.resources

        val file = File(mItem.path)
        val title = if (mItem.isDirectory) R.string.directory_properties else R.string.file_properties
        mPropertyView = mInflater.inflate(R.layout.item_properties, null) as ViewGroup

        addProperty(R.string.name, mItem.name)
        addProperty(R.string.path, mItem.path)
        addProperty(R.string.size, getItemSize())
        addProperty(R.string.last_modified, formatLastModified(file.lastModified()))

        if (mItem.isDirectory) {
            addProperty(R.string.files_count, mFilesCnt.toString())
        } else if (mItem.isImage()) {
            addProperty(R.string.resolution, mItem.getImageResolution())
        } else if (mItem.isAudio()) {
            addProperty(R.string.duration, mItem.getDuration())
        } else if (mItem.isVideo()) {
            addProperty(R.string.duration, mItem.getDuration())
            addProperty(R.string.resolution, mItem.getVideoResolution())
        }

        AlertDialog.Builder(context)
                .setTitle(mResources.getString(title))
                .setView(mPropertyView)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
    }

    private fun addProperty(labelId: Int, value: String) {
        val view = mInflater.inflate(R.layout.property_item, mPropertyView, false)
        view.property_label.text = mResources.getString(labelId)
        view.property_value.text = value
        mPropertyView.properties_holder.addView(view)
    }

    private fun getItemSize(): String {
        if (mItem.isDirectory) {
            mCountHiddenItems = Config.newInstance(mContext).showHidden
            return getDirectorySize(File(mItem.path)).formatSize()
        }

        return mItem.size.formatSize()
    }

    private fun formatLastModified(ts: Long): String {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = ts
        return DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            val files = dir.listFiles()
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    size += getDirectorySize(files[i])
                } else if (!files[i].isHidden && !dir.isHidden || mCountHiddenItems) {
                    mFilesCnt++
                    size += files[i].length()
                }
            }
        }
        return size
    }
}
