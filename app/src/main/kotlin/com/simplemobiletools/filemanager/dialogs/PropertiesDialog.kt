package com.simplemobiletools.filemanager.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.formatSize
import com.simplemobiletools.filepicker.models.FileDirItem
import kotlinx.android.synthetic.main.item_properties.view.*
import java.io.File
import java.util.*

class PropertiesDialog() {
    lateinit var mContext: Context
    lateinit var mItem: FileDirItem
    private var mCountHiddenItems = false
    private var mFilesCnt = 0

    constructor(context: Context, item: FileDirItem, countHiddenItems: Boolean = false) : this() {
        mContext = context
        mItem = item
        mCountHiddenItems = countHiddenItems

        val title = if (mItem.isDirectory) R.string.directory_properties else R.string.file_properties
        val infoView = LayoutInflater.from(context).inflate(R.layout.item_properties, null)

        infoView.apply {
            properties_name.text = mItem.name
            properties_path.text = mItem.path
            properties_size.text = getItemSize()

            if (mItem.isDirectory) {
                properties_files_count_label.visibility = View.VISIBLE
                properties_files_count.visibility = View.VISIBLE
                properties_files_count.text = mFilesCnt.toString()
            } else if (mItem.isImage()) {
                properties_resolution_label.visibility = View.VISIBLE
                properties_resolution.visibility = View.VISIBLE
                properties_resolution.text = mItem.getImageResolution()
            } else if (mItem.isAudio()) {
                properties_duration_label.visibility = View.VISIBLE
                properties_duration.visibility = View.VISIBLE
                properties_duration.text = mItem.getDuration()
            } else if (mItem.isVideo()) {
                properties_duration_label.visibility = View.VISIBLE
                properties_duration.visibility = View.VISIBLE
                properties_duration.text = mItem.getDuration()

                properties_resolution_label.visibility = View.VISIBLE
                properties_resolution.visibility = View.VISIBLE
                properties_resolution.text = mItem.getVideoResolution()
            }

            val file = File(mItem.path)
            properties_last_modified.text = formatLastModified(file.lastModified())
        }

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(title))
                .setView(infoView)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
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
        return DateFormat.format("dd/MM/yyyy HH:mm", cal).toString()
    }

    private fun getDirectorySize(dir: File): Long {
        if (dir.exists()) {
            var size: Long = 0
            val files = dir.listFiles()
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    size += getDirectorySize(files[i])
                } else if (!files[i].isHidden && !dir.isHidden || mCountHiddenItems) {
                    mFilesCnt++
                    size += files[i].length()
                }
            }
            return size
        }
        return 0
    }
}
