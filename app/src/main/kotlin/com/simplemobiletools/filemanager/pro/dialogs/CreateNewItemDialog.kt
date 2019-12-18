package com.simplemobiletools.filemanager.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import kotlinx.android.synthetic.main.dialog_create_new.view.*
import java.io.File
import java.io.IOException

class CreateNewItemDialog(val activity: SimpleActivity, val path: String, val callback: (success: Boolean) -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_create_new, null)

    init {
        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.create_new) {
                        showKeyboard(view.item_name)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                            val name = view.item_name.value
                            if (name.isEmpty()) {
                                activity.toast(R.string.empty_name)
                            } else if (name.isAValidFilename()) {
                                val newPath = "$path/$name"
                                if (activity.getDoesFilePathExist(newPath)) {
                                    activity.toast(R.string.name_taken)
                                    return@OnClickListener
                                }

                                if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_directory) {
                                    createDirectory(newPath, this) {
                                        callback(it)
                                    }
                                } else {
                                    createFile(newPath, this) {
                                        callback(it)
                                    }
                                }
                            } else {
                                activity.toast(R.string.invalid_name)
                            }
                        })
                    }
                }
    }

    private fun createDirectory(path: String, alertDialog: AlertDialog, callback: (Boolean) -> Unit) {
        when {
            activity.needsStupidWritePermissions(path) -> activity.handleSAFDialog(path) {
                if (!it) {
                    return@handleSAFDialog
                }

                val documentFile = activity.getDocumentFile(path.getParentPath())
                if (documentFile == null) {
                    val error = String.format(activity.getString(R.string.could_not_create_folder), path)
                    activity.showErrorToast(error)
                    callback(false)
                    return@handleSAFDialog
                }
                documentFile.createDirectory(path.getFilenameFromPath())
                success(alertDialog)
            }
            path.startsWith(activity.internalStoragePath, true) -> {
                if (File(path).mkdirs()) {
                    success(alertDialog)
                }
            }
            else -> {
                RootHelpers(activity).createFileFolder(path, false) {
                    if (it) {
                        success(alertDialog)
                    } else {
                        callback(false)
                    }
                }
            }
        }
    }

    private fun createFile(path: String, alertDialog: AlertDialog, callback: (Boolean) -> Unit) {
        try {
            when {
                activity.needsStupidWritePermissions(path) -> {
                    activity.handleSAFDialog(path) {
                        if (!it) {
                            return@handleSAFDialog
                        }

                        val documentFile = activity.getDocumentFile(path.getParentPath())
                        if (documentFile == null) {
                            val error = String.format(activity.getString(R.string.could_not_create_file), path)
                            activity.showErrorToast(error)
                            callback(false)
                            return@handleSAFDialog
                        }
                        documentFile.createFile(path.getMimeType(), path.getFilenameFromPath())
                        success(alertDialog)
                    }
                }
                path.startsWith(activity.internalStoragePath, true) -> {
                    if (File(path).createNewFile()) {
                        success(alertDialog)
                    }
                }
                else -> {
                    RootHelpers(activity).createFileFolder(path, true) {
                        if (it) {
                            success(alertDialog)
                        } else {
                            callback(false)
                        }
                    }
                }
            }
        } catch (exception: IOException) {
            activity.showErrorToast(exception)
            callback(false)
        }
    }

    private fun success(alertDialog: AlertDialog) {
        alertDialog.dismiss()
        callback(true)
    }
}
