package com.simplemobiletools.filemanager.pro.activities

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.simplemobiletools.commons.extensions.handleHiddenFolderPasswordProtection
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.filemanager.pro.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.helpers.*

class MimeTypesActivity : SimpleActivity() {
    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null
    private var currentMimeType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mimetypes)
        currentMimeType = intent.getStringExtra(SHOW_MIMETYPE) ?: return
        title = getString(
            when (currentMimeType) {
                IMAGES -> R.string.images
                VIDEOS -> R.string.videos
                AUDIO -> R.string.audio
                DOCUMENTS -> R.string.documents
                ARCHIVES -> R.string.archives
                OTHERS -> R.string.others
                else -> {
                    toast(R.string.unknown_error_occurred)
                    finish()
                    return
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        setupSearch(menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val currentViewType = config.getFolderViewType(currentMimeType)

        menu!!.apply {
            findItem(R.id.add_favorite).isVisible = false
            findItem(R.id.remove_favorite).isVisible = false
            findItem(R.id.go_to_favorite).isVisible = false

            findItem(R.id.toggle_filename).isVisible = currentViewType == VIEW_TYPE_GRID
            findItem(R.id.go_home).isVisible = false
            findItem(R.id.set_as_home).isVisible = false
            findItem(R.id.settings).isVisible = false
            findItem(R.id.about).isVisible = false

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.increase_column_count).isVisible = currentViewType == VIEW_TYPE_GRID && config.fileColumnCnt < MAX_COLUMN_COUNT
            findItem(R.id.reduce_column_count).isVisible = currentViewType == VIEW_TYPE_GRID && config.fileColumnCnt > 1
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.toggle_filename -> toggleFilenameVisibility()
            R.id.change_view_type -> changeViewType()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(R.string.search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchOpen = false
                return true
            }
        })
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, currentMimeType) {
        }
    }

    private fun toggleFilenameVisibility() {
        config.displayFilenames = !config.displayFilenames
    }

    private fun increaseColumnCount() {}

    private fun reduceColumnCount() {}

    private fun changeViewType() {
        ChangeViewTypeDialog(this, currentMimeType, true) { }
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
        } else {
            handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        config.temporarilyShowHidden = show
    }
}
