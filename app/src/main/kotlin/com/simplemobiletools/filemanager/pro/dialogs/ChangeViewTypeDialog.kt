package com.simplemobiletools.filemanager.pro.dialogs

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.databinding.DialogChangeViewTypeBinding
import com.simplemobiletools.filemanager.pro.extensions.config

class ChangeViewTypeDialog(val activity: BaseSimpleActivity, val path: String = "", showFolderCheck: Boolean = true, val callback: () -> Unit) {
    private var binding: DialogChangeViewTypeBinding
    private var config = activity.config

    init {
        binding = DialogChangeViewTypeBinding.inflate(activity.layoutInflater).apply {
            val currViewType = config.getFolderViewType(this@ChangeViewTypeDialog.path)
            val viewToCheck = if (currViewType == VIEW_TYPE_GRID) {
                changeViewTypeDialogRadioGrid.id
            } else {
                changeViewTypeDialogRadioList.id
            }

            changeViewTypeDialogRadio.check(viewToCheck)
            if (!showFolderCheck) {
                useForThisFolderDivider.beGone()
                changeViewTypeDialogUseForThisFolder.beGone()
            }

            changeViewTypeDialogUseForThisFolder.apply {
                isChecked = config.hasCustomViewType(this@ChangeViewTypeDialog.path)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        val viewType = if (binding.changeViewTypeDialogRadio.checkedRadioButtonId == binding.changeViewTypeDialogRadioGrid.id) {
            VIEW_TYPE_GRID
        } else {
            VIEW_TYPE_LIST
        }

        if (binding.changeViewTypeDialogUseForThisFolder.isChecked) {
            config.saveFolderViewType(this.path, viewType)
        } else {
            config.removeFolderViewType(this.path)
            config.viewType = viewType
        }

        callback()
    }
}
