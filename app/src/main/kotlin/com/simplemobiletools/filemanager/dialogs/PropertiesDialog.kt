package com.simplemobiletools.filemanager.dialogs

import android.content.Context
import android.content.res.Resources
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.*
import kotlinx.android.synthetic.main.item_properties.view.*
import kotlinx.android.synthetic.main.property_item.view.*
import java.io.File

class PropertiesDialog() {
    lateinit var mContext: Context
    lateinit var mInflater: LayoutInflater
    lateinit var mPropertyView: ViewGroup
    lateinit var mResources: Resources

    private var mCountHiddenItems = false
    private var mFilesCnt = 0

    constructor(context: Context, path: String, countHiddenItems: Boolean = false) : this() {
        mContext = context
        mCountHiddenItems = countHiddenItems
        mInflater = LayoutInflater.from(context)
        mResources = mContext.resources

        val file = File(path)
        val title = if (file.isDirectory) R.string.directory_properties else R.string.file_properties
        mPropertyView = mInflater.inflate(R.layout.item_properties, null) as ViewGroup

        addProperty(R.string.name, file.name)
        addProperty(R.string.path, file.parent)
        addProperty(R.string.size, getItemSize(file))
        addProperty(R.string.last_modified, file.lastModified().formatLastModified())

        if (file.isDirectory) {
            addProperty(R.string.files_count, mFilesCnt.toString())
        } else if (file.isImage()) {
            addProperty(R.string.resolution, file.getImageResolution())
        } else if (file.isAudio()) {
            addProperty(R.string.duration, file.getDuration())
        } else if (file.isVideo()) {
            addProperty(R.string.duration, file.getDuration())
            addProperty(R.string.resolution, file.getVideoResolution())
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

    private fun getItemSize(file: File): String {
        if (file.isDirectory) {
            mCountHiddenItems = Config.newInstance(mContext).showHidden
            return getDirectorySize(File(file.path)).formatSize()
        }

        return file.length().formatSize()
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
