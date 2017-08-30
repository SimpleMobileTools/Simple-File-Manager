package com.simplemobiletools.filemanager.dialogs

import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import kotlinx.android.synthetic.main.dialog_create_new.view.*
import java.io.File
import java.io.IOException

class CreateNewItemDialog(val activity: SimpleActivity, val path: String, val callback: () -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_create_new, null)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.create_new)
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                val name = view.item_name.value
                if (name.isEmpty()) {
                    activity.toast(R.string.empty_name)
                } else if (name.isAValidFilename()) {
                    val file = File(path, name)
                    if (file.exists()) {
                        activity.toast(R.string.name_taken)
                        return@OnClickListener
                    }

                    if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_directory) {
                        createDirectory(file, this) {
                            if (!it) {
                                errorOccurred()
                            }
                        }
                    } else {
                        createFile(file, this) {
                            if (!it) {
                                errorOccurred()
                            }
                        }
                    }
                } else {
                    activity.toast(R.string.invalid_name)
                }
            })
        }
    }

    private fun createDirectory(file: File, alertDialog: AlertDialog, callback: (Boolean) -> Unit) {
        if (activity.needsStupidWritePermissions(path)) {
            activity.handleSAFDialog(file) {
                val documentFile = activity.getFileDocument(file.absolutePath)
                if (documentFile == null) {
                    callback(false)
                    return@handleSAFDialog
                }
                documentFile.createDirectory(file.name)
                success(alertDialog)
            }
        } else if (file.mkdirs()) {
            success(alertDialog)
            callback(true)
        } else
            callback(false)
    }

    private fun errorOccurred() {
        activity.toast(R.string.unknown_error_occurred)
    }

    private fun createFile(file: File, alertDialog: AlertDialog, callback: (Boolean) -> Unit) {
        try {
            if (activity.needsStupidWritePermissions(path)) {
                activity.handleSAFDialog(file) {
                    val documentFile = activity.getFileDocument(file.absolutePath)
                    if (documentFile == null) {
                        callback(false)
                        return@handleSAFDialog
                    }
                    documentFile.createFile("", file.name)
                    success(alertDialog)
                }
            } else if (file.createNewFile()) {
                success(alertDialog)
                callback(true)
            }
        } catch (exception: IOException) {
            activity.showErrorToast(exception.toString())
        }
    }

    private fun success(alertDialog: AlertDialog) {
        alertDialog.dismiss()
        callback()
    }
}
