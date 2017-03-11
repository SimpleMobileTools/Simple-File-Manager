package com.simplemobiletools.filemanager.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.dialog_rename_item.view.*
import java.io.File

class RenameItemDialog(val activity: Activity, val item: FileDirItem, val callback: () -> Unit) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_rename_item, null)
        view.item_name.setText(item.name)

        val path = File(item.path).parent

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.rename)
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val newName = view.item_name.value
                if (!newName.isAValidFilename()) {
                    context.toast(R.string.invalid_name)
                    return@setOnClickListener
                }

                val currFile = File(path, item.name)
                val newFile = File(path, newName)

                if (newFile.exists()) {
                    context.toast(R.string.name_taken)
                    return@setOnClickListener
                }

                if (context.needsStupidWritePermissions(path)) {
                    val document = context.getFileDocument(currFile.absolutePath, context.config.treeUri) ?: return@setOnClickListener
                    if (document.canWrite())
                        document.renameTo(newName)
                    sendSuccess(newFile)
                    dismiss()
                } else {
                    if (currFile.renameTo(newFile)) {
                        sendSuccess(newFile)
                        dismiss()
                    } else {
                        context.toast(R.string.unknown_error_occurred)
                    }
                }
            })
        }
    }

    private fun sendSuccess(newFile: File) {
        activity.scanFiles(arrayListOf(newFile)) {}
        callback.invoke()
    }
}
