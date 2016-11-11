package com.simplemobiletools.filemanager.dialogs

import android.app.Activity
import android.content.Intent
import android.support.v4.util.Pair
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.Utils
import com.simplemobiletools.filemanager.activities.MainActivity
import com.simplemobiletools.filemanager.asynctasks.CopyTask
import com.simplemobiletools.filemanager.extensions.rescanItem
import com.simplemobiletools.filemanager.extensions.toast
import com.simplemobiletools.filemanager.extensions.value
import com.simplemobiletools.filepicker.dialogs.FilePickerDialog
import com.simplemobiletools.filepicker.extensions.humanizePath
import kotlinx.android.synthetic.main.copy_item.view.*
import java.io.File

class CopyDialog(val activity: Activity, val files: List<File>, val copyListener: CopyTask.CopyListener, val listener: OnCopyListener) {

    init {
        val context = activity
        val view = LayoutInflater.from(context).inflate(R.layout.copy_item, null)
        val path = files[0].parent.trimEnd('/')
        var destinationPath = ""
        view.source.text = "${context.humanizePath(path)}/"

        view.destination.setOnClickListener {
            val config = Config.newInstance(context)
            FilePickerDialog(activity, destinationPath, false, config.showHidden, object : FilePickerDialog.OnFilePickerListener {
                override fun onFail(error: FilePickerDialog.FilePickerResult) {
                }

                override fun onSuccess(pickedPath: String) {
                    destinationPath = pickedPath
                    view.destination.text = context.humanizePath(pickedPath)
                }
            })
        }

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(if (files.size == 1) R.string.copy_item else R.string.copy_items))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                if (destinationPath == context.resources.getString(R.string.select_destination) || destinationPath.isEmpty()) {
                    context.toast(R.string.please_select_destination)
                    return@setOnClickListener
                }

                if (view.source.text.trimEnd('/') == destinationPath.trimEnd('/')) {
                    context.toast(R.string.source_and_destination_same)
                    return@setOnClickListener
                }

                val destinationDir = File(destinationPath)
                if (!destinationDir.exists()) {
                    context.toast(R.string.invalid_destination)
                    return@setOnClickListener
                }

                if (files.size == 1) {
                    val newFile = File(files[0].path)
                    if (File(destinationPath, newFile.name).exists()) {
                        context.toast(R.string.already_exists)
                        return@setOnClickListener
                    }
                }

                if (Utils.needsStupidWritePermissions(context, destinationPath) && Config.newInstance(context).treeUri.isEmpty()) {
                    WritePermissionDialog(activity, object : WritePermissionDialog.OnWritePermissionListener {
                        override fun onConfirmed() {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            activity.startActivityForResult(intent, MainActivity.OPEN_DOCUMENT_TREE)
                        }
                    })
                    return@setOnClickListener
                }

                if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_copy) {
                    context.toast(R.string.copying)
                    val pair = Pair<List<File>, File>(files, destinationDir)
                    CopyTask(copyListener, context).execute(pair)
                    dismiss()
                } else {
                    if (Utils.isPathOnSD(context, view.source.value) && Utils.isPathOnSD(context, destinationPath)) {
                        for (f in files) {
                            val destination = File(destinationDir, f.name)
                            f.renameTo(destination)
                            context.rescanItem(destination)
                        }

                        dismiss()
                        listener.onSuccess()
                    } else {
                        context.toast(R.string.copying_no_delete, Toast.LENGTH_LONG)
                        val pair = Pair<List<File>, File>(files, destinationDir)
                        CopyTask(copyListener, context).execute(pair)
                        dismiss()
                    }
                }
            })
        }
    }

    interface OnCopyListener {
        fun onSuccess()
    }
}
