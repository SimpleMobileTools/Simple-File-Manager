package com.simplemobiletools.filemanager.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.Utils
import com.simplemobiletools.filemanager.extensions.toast
import com.simplemobiletools.filemanager.extensions.value
import kotlinx.android.synthetic.main.create_new.view.*
import java.io.File
import java.io.IOException

class CreateNewItemDialog() {
    interface OnCreateNewItemListener {
        fun onSuccess()
    }

    lateinit var mContext: Context
    var mListener: OnCreateNewItemListener? = null

    constructor(context: Context, path: String, listener: OnCreateNewItemListener) : this() {
        mContext = context
        mListener = listener

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
                } else if (Utils.isNameValid(name)) {
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
        return if (file.mkdirs()) {
            alertDialog.dismiss()
            mListener?.onSuccess()
            true
        } else
            false
    }

    private fun errorOccurred() {
        mContext.toast(R.string.error_occurred)
    }

    private fun createFile(file: File, alertDialog: AlertDialog): Boolean {
        try {
            if (file.createNewFile()) {
                alertDialog.dismiss()
                mListener?.onSuccess()
                return true
            }
        } catch (ignored: IOException) {

        }

        return false
    }
}
