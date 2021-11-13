package com.simplemobiletools.filemanager.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.TAB_FILES
import com.simplemobiletools.commons.helpers.TAB_RECENT_FILES
import com.simplemobiletools.commons.helpers.TAB_STORAGE_ANALYSIS
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.commons.views.MyAppCompatCheckbox
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.helpers.ALL_TABS_MASK
import kotlinx.android.synthetic.main.dialog_manage_visible_tabs.view.*

class ManageVisibleTabsDialog(val activity: BaseSimpleActivity) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_manage_visible_tabs, null)
    private val tabs = LinkedHashMap<Int, Int>()

    init {
        tabs.apply {
            put(TAB_FILES, R.id.manage_visible_tabs_files)
            put(TAB_RECENT_FILES, R.id.manage_visible_tabs_recent_files)
            put(TAB_STORAGE_ANALYSIS, R.id.manage_visible_tabs_storage_analysis)
        }

        if (!isOreoPlus()) {
            view.manage_visible_tabs_storage_analysis.beGone()
        }

        val showTabs = activity.config.showTabs
        for ((key, value) in tabs) {
            view.findViewById<MyAppCompatCheckbox>(value).isChecked = showTabs and key != 0
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in tabs) {
            if (view.findViewById<MyAppCompatCheckbox>(value).isChecked) {
                result += key
            }
        }

        if (result == 0) {
            result = ALL_TABS_MASK
        }

        activity.config.showTabs = result
    }
}
