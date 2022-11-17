package com.simplemobiletools.filemanager.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import kotlinx.android.synthetic.main.dialog_insert_filename.view.*

class InsertFilenameDialog(
    val activity: BaseSimpleActivity, var path: String, val callback: (filename: String) -> Unit
) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_insert_filename, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.filename) { alertDialog ->
                    alertDialog.showKeyboard(view.insert_filename_title)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.insert_filename_title.value
                        val extension = view.insert_filename_extension_title.value

                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        var newFilename = filename
                        if (extension.isNotEmpty()) {
                            newFilename += ".$extension"
                        }

                        val newPath = "$path/$newFilename"
                        if (!newFilename.isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (activity.getDoesFilePathExist(newPath)) {
                            val msg = String.format(activity.getString(R.string.file_already_exists), newFilename)
                            activity.toast(msg)
                        } else {
                            callback(newFilename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
