package com.simplemobiletools.filemanager.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import kotlinx.android.synthetic.main.dialog_enter_password.view.password

class EnterPasswordDialog(
    val activity: BaseSimpleActivity,
    private val callback: (password: String) -> Unit,
    private val cancelCallback: () -> Unit
) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_enter_password, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.enter_password) { alertDialog ->
                    alertDialog.showKeyboard(view.password)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val password = view.password.value

                        if (password.isEmpty()) {
                            activity.toast(R.string.empty_password)
                            return@setOnClickListener
                        }

                        callback(password)
                        alertDialog.setOnDismissListener(null)
                        alertDialog.dismiss()
                    }

                    alertDialog.setOnDismissListener {
                        cancelCallback()
                    }
                }
            }
    }
}
