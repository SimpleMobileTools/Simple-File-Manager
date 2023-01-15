package com.simplemobiletools.filemanager.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import kotlinx.android.synthetic.main.dialog_save_as.view.*

class SaveAsDialog(
    val activity: BaseSimpleActivity, var path: String, val hidePath: Boolean,
    val callback: (path: String, filename: String) -> Unit
) {

    init {
        if (path.isEmpty()) {
            path = "${activity.internalStoragePath}/${activity.getCurrentFormattedDateTime()}.txt"
        }

        var realPath = path.getParentPath()
        val view = activity.layoutInflater.inflate(R.layout.dialog_save_as, null).apply {
            folder_value.setText(activity.humanizePath(realPath))

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                extension_value.setText(extension)
            }

            filename_value.setText(name)

            if (hidePath) {
                folder_hint.beGone()
            } else {
                folder_value.setOnClickListener {
                    FilePickerDialog(activity, realPath, false, false, true, true, showFavoritesButton = true) {
                        folder_value.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.save_as) { alertDialog ->
                    alertDialog.showKeyboard(view.filename_value)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.filename_value.value
                        val extension = view.extension_value.value

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

                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newFilename)
                            ConfirmationDialog(activity, title) {
                                callback(newPath, newFilename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, newFilename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
