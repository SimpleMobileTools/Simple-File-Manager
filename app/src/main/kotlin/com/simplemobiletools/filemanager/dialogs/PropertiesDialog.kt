package com.simplemobiletools.filemanager.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.formatSize
import com.simplemobiletools.filepicker.models.FileDirItem
import java.io.File
import java.util.*

class PropertiesDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mShowHidden = Config.newInstance(context).showHidden
        val title = if (mItem!!.isDirectory) R.string.directory_properties else R.string.file_properties

        val infoView = activity.layoutInflater.inflate(R.layout.item_properties, null)
        (infoView.findViewById(R.id.properties_name) as TextView).text = mItem!!.name
        (infoView.findViewById(R.id.properties_path) as TextView).text = mItem!!.path
        (infoView.findViewById(R.id.properties_size) as TextView).text = getItemSize()

        if (mItem!!.isDirectory) {
            infoView.findViewById(R.id.properties_files_count_label).visibility = View.VISIBLE
            infoView.findViewById(R.id.properties_files_count).visibility = View.VISIBLE
            (infoView.findViewById(R.id.properties_files_count) as TextView).text = mFilesCnt.toString()
        }

        val file = File(mItem!!.path)
        (infoView.findViewById(R.id.properties_last_modified) as TextView).text = formatLastModified(file.lastModified())

        val builder = AlertDialog.Builder(context)
        builder.setTitle(resources.getString(title))
        builder.setView(infoView)
        builder.setPositiveButton(R.string.ok, null)

        return builder.create()
    }

    fun getItemSize(): String {
        if (mItem!!.isDirectory) {
            return getDirectorySize(File(mItem!!.path)).formatSize()
        }

        return ""
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

    companion object {
        private var mItem: FileDirItem? = null
        private var mFilesCnt: Int = 0
        private var mShowHidden: Boolean = false

        fun newInstance(item: FileDirItem): PropertiesDialog {
            mItem = item
            mFilesCnt = 0
            return PropertiesDialog()
        }
    }
}
