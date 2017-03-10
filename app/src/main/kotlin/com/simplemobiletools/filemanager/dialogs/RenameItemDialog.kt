package com.simplemobiletools.filemanager.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import kotlinx.android.synthetic.main.rename_item.view.*
import java.io.File

class RenameItemDialog(val context: Context, val item: FileDirItem, val listener: OnRenameItemListener) {

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.rename_item, null)
        view.item_name.setText(item.name)

        val path = File(item.path).parent

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.rename_item))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            setCanceledOnTouchOutside(true)
            show()
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
                    val document = context.getFileDocument(currFile.absolutePath, Config.newInstance(context).treeUri) ?: return@setOnClickListener
                    if (document.canWrite())
                        document.renameTo(newName)
                    sendSuccess(newFile)
                    dismiss()
                } else {
                    if (currFile.renameTo(newFile)) {
                        sendSuccess(newFile)
                        dismiss()
                    } else {
                        context.toast(R.string.error_occurred)
                    }
                }
            })
        }
    }

    private fun sendSuccess(newFile: File) {
        context.scanFiles(arrayListOf(newFile)) {}
        listener.onSuccess()
    }

    interface OnRenameItemListener {
        fun onSuccess()
    }
}
