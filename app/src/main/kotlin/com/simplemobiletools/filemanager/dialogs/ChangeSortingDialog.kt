package com.simplemobiletools.filemanager.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private var view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null)

    init {
        view.sorting_dialog_use_for_this_folder.isChecked = config.hasCustomSorting(path)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.sort_by)
                }

        currSorting = config.getFolderSorting(path)
        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        val sortBtn = when {
            currSorting and SORT_BY_SIZE != 0 -> sortingRadio.sorting_dialog_radio_size
            currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingRadio.sorting_dialog_radio_last_modified
            currSorting and SORT_BY_EXTENSION != 0 -> sortingRadio.sorting_dialog_radio_extension
            else -> sortingRadio.sorting_dialog_radio_name
        }
        sortBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.sorting_dialog_radio_order
        var orderBtn = orderRadio.sorting_dialog_radio_ascending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = orderRadio.sorting_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_name -> SORT_BY_NAME
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            R.id.sorting_dialog_radio_last_modified -> SORT_BY_DATE_MODIFIED
            else -> SORT_BY_EXTENSION
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (view.sorting_dialog_use_for_this_folder.isChecked) {
            config.saveFolderSorting(path, sorting)
        } else {
            config.removeFolderSorting(path)
            config.sorting = sorting
        }
        callback()
    }
}
