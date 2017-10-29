package com.simplemobiletools.filemanager.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.getFileOutputStream
import com.simplemobiletools.commons.extensions.getRealPathFromURI
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.dialogs.SaveAsDialog
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.activity_read_text.*
import java.io.File

class ReadTextActivity : SimpleActivity() {
    var filePath = ""

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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveText()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun saveText() {
        if (filePath.isEmpty()) {
            filePath = getRealPathFromURI(intent.data) ?: ""
        }

        SaveAsDialog(this, filePath) {
            getFileOutputStream(File(it)) {
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
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val text = if (uri.scheme == "file") {
            filePath = uri.path
            File(uri.path).readText()
        } else {
            contentResolver.openInputStream(uri).bufferedReader().use { it.readText() }
        }

        read_text_view.setText(text)
    }
}
