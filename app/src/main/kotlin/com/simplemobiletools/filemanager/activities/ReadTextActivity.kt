package com.simplemobiletools.filemanager.activities

import android.net.Uri
import android.os.Bundle
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
            R.id.menu_open_with -> openPath(intent.dataString, true)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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

        val text = if (uri.scheme == "file") {
            filePath = uri.path
            File(uri.path).readText()
        } else {
            try {
                contentResolver.openInputStream(uri).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                showErrorToast(e)
                finish()
                return
            }
        }

        read_text_view.setText(text)
    }
}
