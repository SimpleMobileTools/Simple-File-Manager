package com.simplemobiletools.filemanager.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.R
import kotlinx.android.synthetic.main.dialog_save_as.view.*

class SaveAsDialog(val activity: BaseSimpleActivity, var path: String, val callback: (savePath: String) -> Unit) {

    init {
        if (path.isEmpty()) {
            path = "${activity.internalStoragePath}/${activity.getCurrentFormattedDateTime()}.txt"
        }

        var realPath = path.getParentPath()
        val view = activity.layoutInflater.inflate(R.layout.dialog_save_as, null).apply {
            save_as_path.text = activity.humanizePath(realPath)

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                save_as_extension.setText(extension)
            }

            save_as_name.setText(name)
            save_as_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, false, true) {
                    save_as_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.save_as) {
                        showKeyboard(view.save_as_name)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val filename = view.save_as_name.value
                            val extension = view.save_as_extension.value

                            if (filename.isEmpty()) {
                                activity.toast(R.string.filename_cannot_be_empty)
                                return@setOnClickListener
                            }

                            var newFilename = filename
                            if (extension.isNotEmpty()) {
                                newFilename += ".$extension"
                            }

                            val newPath = "$realPath/$newFilename"
                            if (!newFilename.isAValidFilename()) {
                                activity.toast(R.string.filename_invalid_characters)
                                return@setOnClickListener
                            }

                            if (activity.getDoesFilePathExist(newPath)) {
                                val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newFilename)
                                ConfirmationDialog(activity, title) {
                                    callback(newPath)
                                    dismiss()
                                }
                            } else {
                                callback(newPath)
                                dismiss()
                            }
                        }
                    }
                }
    }
}
