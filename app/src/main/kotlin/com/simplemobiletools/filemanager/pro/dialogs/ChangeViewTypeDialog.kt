package com.simplemobiletools.filemanager.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_change_view_type.view.*

class ChangeViewTypeDialog(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) {
    private var view: View
    private var config = activity.config

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_view_type, null).apply {
            val currViewType = config.getFolderViewType(this@ChangeViewTypeDialog.path)
            val viewToCheck = if (currViewType == VIEW_TYPE_GRID) {
                change_view_type_dialog_radio_grid.id
            } else {
                change_view_type_dialog_radio_list.id
            }

            change_view_type_dialog_radio.check(viewToCheck)
            change_view_type_dialog_use_for_this_folder.apply {
                isChecked = config.hasCustomViewType(this@ChangeViewTypeDialog.path)
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val viewType = if (view.change_view_type_dialog_radio.checkedRadioButtonId == view.change_view_type_dialog_radio_grid.id) VIEW_TYPE_GRID else VIEW_TYPE_LIST
        if (view.change_view_type_dialog_use_for_this_folder.isChecked) {
            config.saveFolderViewType(this.path, viewType)
        } else {
            config.removeFolderViewType(this.path)
            config.viewType = viewType
        }

        callback()
    }
}
