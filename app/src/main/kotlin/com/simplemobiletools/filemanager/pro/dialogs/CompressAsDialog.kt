package com.simplemobiletools.filemanager.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_compress_as.view.*

class CompressAsDialog(val activity: BaseSimpleActivity, val path: String, val callback: (destination: String) -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_compress_as, null)

    init {
        val filename = path.getFilenameFromPath()
        val indexOfDot = if (filename.contains('.') && !activity.getIsPathDirectory(path)) filename.lastIndexOf(".") else filename.length
        val baseFilename = filename.substring(0, indexOfDot)
        var realPath = path.getParentPath()

        view.apply {
            file_name.setText(baseFilename)

            file_path.text = activity.humanizePath(realPath)
            file_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, activity.config.shouldShowHidden, true, true, showFavoritesButton = true) {
                    file_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.compress_as) {
                        showKeyboard(view.file_name)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                            val name = view.file_name.value
                            when {
                                name.isEmpty() -> activity.toast(R.string.empty_name)
                                name.isAValidFilename() -> {
                                    val newPath = "$realPath/$name.zip"
                                    if (activity.getDoesFilePathExist(newPath)) {
                                        activity.toast(R.string.name_taken)
                                        return@OnClickListener
                                    }

                                    dismiss()
                                    callback(newPath)
                                }
                                else -> activity.toast(R.string.invalid_name)
                            }
                        })
                    }
                }
    }
}
