package com.simplemobiletools.filemanager.dialogs

import android.app.Activity
import android.support.v4.util.Pair
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.Utils
import com.simplemobiletools.filemanager.asynctasks.CopyTask
import com.simplemobiletools.filemanager.extensions.rescanItem
import com.simplemobiletools.filemanager.extensions.toast
import com.simplemobiletools.filemanager.extensions.value
import com.simplemobiletools.filepicker.dialogs.FilePickerDialog
import kotlinx.android.synthetic.main.copy_item.view.*
import java.io.File

class CopyDialog(val activity: Activity, val files: List<File>, val path: String, val copyListener: CopyTask.CopyListener, val listener: OnCopyListener) {
    val mContext = activity
    init {
        val view = LayoutInflater.from(mContext).inflate(R.layout.copy_item, null)
        view.source.text = "$path/"

        view.destination.setOnClickListener {
            val config = Config.newInstance(mContext)
            FilePickerDialog(activity, path, false, config.showHidden, true, object: FilePickerDialog.OnFilePickerListener {
                override fun onFail(error: FilePickerDialog.FilePickerResult) {
                }

                override fun onSuccess(pickedPath: String) {
                    view.destination.text = pickedPath
                }
            })
        }

        AlertDialog.Builder(mContext)
                .setTitle(mContext.resources.getString(R.string.create_new))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, { dialog, which -> dialogDismissed() })
                .setOnCancelListener { dialogDismissed() }
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val destinationPath = view.destination.value
                if (destinationPath == context.resources.getString(R.string.select_destination)) {
                    context.toast(R.string.please_select_destination)
                    return@setOnClickListener
                }

                val destinationDir = File(destinationPath)
                if (!destinationDir.exists()) {
                    context.toast(R.string.invalid_destination)
                    return@setOnClickListener
                }

                if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_copy) {
                    Utils.showToast(context, R.string.copying)
                    val pair = Pair<List<File>, File>(files, destinationDir)
                    CopyTask(copyListener).execute(pair)
                    dismiss()
                } else {
                    for (f in files) {
                        val destination = File(destinationDir, f.name)
                        f.renameTo(destination)
                        context.rescanItem(destination)
                    }

                    dismiss()
                    listener.onSuccess()
                }
            })
        }
    }

    private fun dialogDismissed() {
        listener.onCancel()
    }

    interface OnCopyListener {
        fun onSuccess()

        fun onCancel()
    }
}
