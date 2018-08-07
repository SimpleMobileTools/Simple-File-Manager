package com.simplemobiletools.filemanager.activities

import android.app.SearchManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.dialogs.SaveAsDialog
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.extensions.openPath
import kotlinx.android.synthetic.main.activity_read_text.*
import java.io.File

class ReadTextActivity : SimpleActivity() {
    private var filePath = ""
    private var originalText = ""
    private var isSearchOpen = false

    private var searchMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_text)
        hideKeyboard()

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                checkIntent()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        setupSearch(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveText()
            R.id.menu_open_with -> openPath(intent.dataString, true)
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
            setOnQueryTextListener(object : android.support.v7.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearchOpen = true
                searchQueryChanged("")
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchOpen = false
                return true
            }
        })
    }

    private fun searchQueryChanged(text: String) {
        val textToHighlight = if (text.length < 2) "" else text
        read_text_view.setText(originalText.highlightTextPart(textToHighlight, getAdjustedPrimaryColor(), true))
    }

    private fun saveText() {
        if (filePath.isEmpty()) {
            filePath = getRealPathFromURI(intent.data) ?: ""
        }

        SaveAsDialog(this, filePath) {
            getFileOutputStream(FileDirItem(it, it.getFilenameFromPath())) {
                if (it != null) {
                    it.bufferedWriter().use { it.write(read_text_view.text.toString()) }
                    toast(R.string.file_saved)
                    hideKeyboard()
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    private fun checkIntent() {
        read_text_view.setTextColor(config.textColor)
        val uri = if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            Uri.fromFile(File(intent.extras?.get(REAL_FILE_PATH).toString()))
        } else {
            intent.data
        }

        if (uri == null) {
            finish()
            return
        }

        originalText = if (uri.scheme == "file") {
            filePath = uri.path
            val file = File(filePath)
            if (file.exists()) {
                file.readText()
            } else {
                toast(R.string.unknown_error_occurred)
                ""
            }
        } else {
            try {
                contentResolver.openInputStream(uri).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                showErrorToast(e)
                finish()
                return
            }
        }

        read_text_view.setText(originalText)
    }
}
