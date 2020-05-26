package com.simplemobiletools.filemanager.pro.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getRealPathFromURI
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.activity_decompress.*
import java.io.BufferedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DecompressActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decompress)
        val uri = intent.data
        if (uri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        getRealPathFromURI(uri)?.apply {
            title = getFilenameFromPath()
        }

        try {
            val listItems = getListItems(uri)
            ItemsAdapter(this, listItems, null, decompress_list, false, null) {
            }.apply {
                decompress_list.adapter = this
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @SuppressLint("NewApi")
    private fun getListItems(uri: Uri): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        val inputStream = contentResolver.openInputStream(uri)
        val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
        var zipEntry: ZipEntry?
        while (true) {
            zipEntry = zipInputStream.nextEntry

            if (zipEntry == null) {
                break
            }

            val lastModified = if (isOreoPlus()) zipEntry.lastModifiedTime.toMillis() else 0
            val listItem = ListItem(zipEntry.name, zipEntry.name, zipEntry.isDirectory, 0, 0L, lastModified, false)
            listItems.add(listItem)
        }
        return listItems
    }
}
