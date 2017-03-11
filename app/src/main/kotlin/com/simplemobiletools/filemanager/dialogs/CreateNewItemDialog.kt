package com.simplemobiletools.filemanager.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.dialog_create_new.view.*
import java.io.File
import java.io.IOException

class CreateNewItemDialog(val activity: Activity, val path: String, val callback: () -> Unit) {
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
                    context.toast(R.string.empty_name)
                } else if (name.isAValidFilename()) {
                    val file = File(path, name)
                    if (file.exists()) {
                        context.toast(R.string.name_taken)
                        return@OnClickListener
                    }

                    if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_directory) {
                        if (!createDirectory(file, this)) {
                            errorOccurred()
                        }
                    } else {
                        if (!createFile(file, this)) {
                            errorOccurred()
                        }
                    }
                } else {
                    context.toast(R.string.invalid_name)
                }
            })
        }
    }

    private fun createDirectory(file: File, alertDialog: AlertDialog): Boolean {
        return if (activity.needsStupidWritePermissions(path)) {
            val documentFile = activity.getFileDocument(file.absolutePath, activity.config.treeUri) ?: return false
            documentFile.createDirectory(file.name)
            success(alertDialog)
            true
        } else if (file.mkdirs()) {
            success(alertDialog)
            true
        } else
            false
    }

    private fun errorOccurred() {
        activity.toast(R.string.unknown_error_occurred)
    }

    private fun createFile(file: File, alertDialog: AlertDialog): Boolean {
        try {
            if (activity.needsStupidWritePermissions(path)) {
                val documentFile = activity.getFileDocument(file.absolutePath, activity.config.treeUri) ?: return false
                documentFile.createFile("", file.name)
                success(alertDialog)
                return true
            } else if (file.createNewFile()) {
                success(alertDialog)
                return true
            }
        } catch (ignored: IOException) {

        }

        return false
    }

    private fun success(alertDialog: AlertDialog) {
        alertDialog.dismiss()
        callback.invoke()
    }
}
