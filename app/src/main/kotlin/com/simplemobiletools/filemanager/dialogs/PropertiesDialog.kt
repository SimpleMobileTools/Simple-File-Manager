package com.simplemobiletools.filemanager.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.format.DateFormat
import android.view.View
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.formatSize
import com.simplemobiletools.filepicker.models.FileDirItem
import kotlinx.android.synthetic.main.item_properties.view.*
import java.io.File
import java.util.*

class PropertiesDialog : DialogFragment() {
    companion object {
        lateinit var mItem: FileDirItem
        private var mFilesCnt: Int = 0
        private var mShowHidden: Boolean = false

        fun newInstance(item: FileDirItem): PropertiesDialog {
            mItem = item
            mFilesCnt = 0
            return PropertiesDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = if (mItem.isDirectory) R.string.directory_properties else R.string.file_properties
        val infoView = activity.layoutInflater.inflate(R.layout.item_properties, null)

        infoView.apply {
            properties_name.text = mItem.name
            properties_path.text = mItem.path
            properties_size.text = getItemSize()

            if (mItem.isDirectory) {
                properties_files_count_label.visibility = View.VISIBLE
                properties_files_count.visibility = View.VISIBLE
                properties_files_count.text = mFilesCnt.toString()
            } else if (mItem.isImage()) {

            }

            val file = File(mItem.path)
            properties_last_modified.text = formatLastModified(file.lastModified())
        }

        return AlertDialog.Builder(context)
                .setTitle(resources.getString(title))
                .setView(infoView)
                .setPositiveButton(R.string.ok, null)
                .create()
    }

    private fun getItemSize(): String {
        if (mItem.isDirectory) {
            mShowHidden = Config.newInstance(context).showHidden
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
                } else {
                    size += files[i].length()
                    if (!files[i].isHidden && !dir.isHidden || mShowHidden)
                        mFilesCnt++
                }
            }
            return size
        }
        return 0
    }
}
