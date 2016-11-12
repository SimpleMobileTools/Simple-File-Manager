package com.simplemobiletools.filemanager.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filepicker.extensions.*
import kotlinx.android.synthetic.main.create_new.view.*
import java.io.File
import java.io.IOException

class CreateNewItemDialog(val context: Context, val path: String, val listener: OnCreateNewItemListener) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.create_new, null)

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.create_new))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            show()
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
        return if (context.needsStupidWritePermissions(path)) {
            val documentFile = context.getFileDocument(file.absolutePath, Config.newInstance(context).treeUri)
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
        context.toast(R.string.error_occurred)
    }

    private fun createFile(file: File, alertDialog: AlertDialog): Boolean {
        try {
            if (context.needsStupidWritePermissions(path)) {
                val documentFile = context.getFileDocument(file.absolutePath, Config.newInstance(context).treeUri)
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
        listener.onSuccess()
    }

    interface OnCreateNewItemListener {
        fun onSuccess()
    }
}
