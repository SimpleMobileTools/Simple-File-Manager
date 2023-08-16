package com.simplemobiletools.filemanager.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.databinding.DialogInsertFilenameBinding

class InsertFilenameDialog(
    val activity: BaseSimpleActivity, var path: String, val callback: (filename: String) -> Unit
) {
    init {
        val binding = DialogInsertFilenameBinding.inflate(activity.layoutInflater)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.filename) { alertDialog ->
                    alertDialog.showKeyboard(binding.insertFilenameTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.insertFilenameTitle.value
                        val extension = binding.insertFilenameExtensionTitle.value

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
