package com.simplemobiletools.filemanager.pro.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.adapters.DecompressItemsAdapter
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.activity_decompress.*
import java.io.BufferedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DecompressActivity : SimpleActivity() {
    private val allFiles = ArrayList<ListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decompress)
        val uri = intent.data
        if (uri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        val realPath = getRealPathFromURI(uri)
        title = realPath?.getFilenameFromPath() ?: uri.toString().getFilenameFromPath()

        try {
            fillAllListItems(uri)
            val listItems = getFolderItems("")
            DecompressItemsAdapter(this, listItems, decompress_list) {

            }.apply {
                decompress_list.adapter = this
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_decompress, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.decompress -> decompressFiles()
        }

        return true
    }

    private fun decompressFiles() {
        val defaultFolder = getRealPathFromURI(intent.data!!) ?: internalStoragePath
        FilePickerDialog(this, defaultFolder, false, config.showHidden, true, true) { destination ->
            handleSAFDialog(destination) {
                if (it) {
                    ensureBackgroundThread {
                        decompressTo(destination)
                    }
                }
            }
        }
    }

    private fun decompressTo(destination: String) {
        try {
            val inputStream = contentResolver.openInputStream(intent.data!!)
            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream!!))
            val buffer = ByteArray(1024)

            zipInputStream.use {
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break
                    val newPath = "$destination/${entry.name}"
                    val fos = getFileOutputStreamSync(newPath, newPath.getMimeType())

                    var count: Int
                    while (true) {
                        count = zipInputStream.read(buffer)
                        if (count == -1) {
                            break
                        }

                        fos!!.write(buffer, 0, count)
                    }
                    fos!!.close()
                }

                toast(R.string.decompression_successful)
                finish()
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun getFolderItems(parent: String): ArrayList<ListItem> {
        return allFiles.filter {
            val fileParent = if (it.path.contains("/")) {
                it.path.getParentPath()
            } else {
                ""
            }

            fileParent == parent
        }.toMutableList() as ArrayList<ListItem>
    }

    @SuppressLint("NewApi")
    private fun fillAllListItems(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
        var zipEntry: ZipEntry?
        while (true) {
            zipEntry = zipInputStream.nextEntry

            if (zipEntry == null) {
                break
            }

            val lastModified = if (isOreoPlus()) zipEntry.lastModifiedTime.toMillis() else 0
            val listItem = ListItem(zipEntry.name.removeSuffix("/"), zipEntry.name.removeSuffix("/").getFilenameFromPath(), zipEntry.isDirectory, 0, 0L, lastModified, false)
            allFiles.add(listItem)
        }
    }
}
