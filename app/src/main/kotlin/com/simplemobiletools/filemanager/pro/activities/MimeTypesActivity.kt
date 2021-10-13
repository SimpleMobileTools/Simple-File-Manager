package com.simplemobiletools.filemanager.pro.activities

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.filemanager.pro.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.helpers.*
import com.simplemobiletools.filemanager.pro.models.ListItem
import java.util.*

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

        ensureBackgroundThread {
            getProperFileDirItems { fileDirItems ->
                FileDirItem.sorting = config.getFolderSorting(currentMimeType)
                fileDirItems.sort()
                val listItems = getListItemsFromFileDirItems(fileDirItems)

                runOnUiThread {

                }
            }
        }
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

    private fun getProperFileDirItems(callback: (ArrayList<FileDirItem>) -> Unit) {
        val fileDirItems = ArrayList<FileDirItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        try {
            queryCursor(uri, projection) { cursor ->
                try {
                    val fullMimetype = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault()) ?: return@queryCursor
                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                    val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    val lastModified = cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED) * 1000

                    val mimetype = fullMimetype.substringBefore("/")
                    when (currentMimeType) {
                        IMAGES -> {
                            if (mimetype == "image") {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        VIDEOS -> {
                            if (mimetype == "video") {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        AUDIO -> {
                            if (mimetype == "audio" || extraAudioMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        DOCUMENTS -> {
                            if (mimetype == "text" || extraDocumentMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                        ARCHIVES -> {
                            if (archiveMimeTypes.contains(fullMimetype)) {
                                fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        callback(fileDirItems)
    }

    private fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        fileDirItems.forEach {
            val listItem = ListItem(it.path, it.name, false, 0, it.size, it.modified, false)
            listItems.add(listItem)
        }
        return listItems
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
