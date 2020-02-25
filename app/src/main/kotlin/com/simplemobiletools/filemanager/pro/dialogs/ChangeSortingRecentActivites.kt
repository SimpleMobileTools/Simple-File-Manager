package com.simplemobiletools.filemanager.pro.dialogs

import android.view.Menu
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*



class ChangeSortingRecentActivites(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private var view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null)


    init {


        recentDialogConfirmed()
        currSorting = config.getFolderSorting(path)
        setupSortRadio()
    }


    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        val sortBtn = when {
            currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingRadio.sorting_dialog_radio_last_modified
            else -> sortingRadio.sorting_dialog_radio_name
        }
        sortBtn.isChecked = true
    }

    private fun recentDialogConfirmed() {
        var sorting = SORT_BY_DATE_MODIFIED
        config.sorting = sorting
        callback()

    }
}
