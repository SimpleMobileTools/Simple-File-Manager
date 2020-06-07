package com.simplemobiletools.filemanager.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private var view: View

    init {
        currSorting = config.getFolderSorting(path)
        view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null).apply {
            sorting_dialog_use_for_this_folder.isChecked = config.hasCustomSorting(path)

            sorting_dialog_numeric_sorting.beVisibleIf(currSorting and SORT_BY_NAME != 0)
            sorting_dialog_numeric_sorting.isChecked = currSorting and SORT_USE_NUMERIC_VALUE != 0
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.sort_by)
            }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting

        sortingRadio.setOnCheckedChangeListener { group, checkedId ->
            val isSortingByName = checkedId == sortingRadio.sorting_dialog_radio_name.id
            view.sorting_dialog_numeric_sorting.beVisibleIf(isSortingByName)
        }

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

        if (view.sorting_dialog_numeric_sorting.isChecked) {
            sorting = sorting or SORT_USE_NUMERIC_VALUE
        }

        if (view.sorting_dialog_use_for_this_folder.isChecked) {
            config.saveCustomSorting(path, sorting)
        } else {
            config.removeCustomSorting(path)
            config.sorting = sorting
        }
        callback()
    }
}
