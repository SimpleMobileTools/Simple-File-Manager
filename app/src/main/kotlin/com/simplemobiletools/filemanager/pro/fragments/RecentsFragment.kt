package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.provider.MediaStore
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import java.util.*

class RecentsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet), ItemOperationsListener {
    override fun setupFragment(activity: SimpleActivity) {}

    override fun setupColors(textColor: Int, adjustedPrimaryColor: Int) {}

    private fun getRecents() {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED)
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 20"
        val cursor = context?.contentResolver?.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                    val modified = cursor.getIntValue(MediaStore.Files.FileColumns.DATE_MODIFIED)
                } while (cursor.moveToNext())
            }
        }
    }

    override fun refreshItems() {}

    override fun deleteFiles(files: ArrayList<FileDirItem>) {}

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun setupFontSize() {}

    override fun setupDateTimeFormat() {}
}
