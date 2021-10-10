package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.queryCursor
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity

class StorageFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun setupFragment(activity: SimpleActivity) {}

    override fun refreshItems() {}

    override fun setupColors(textColor: Int, primaryColor: Int) {}

    override fun toggleFilenameVisibility() {}

    override fun increaseColumnCount() {}

    override fun reduceColumnCount() {}

    override fun setupFontSize() {}

    override fun setupDateTimeFormat() {}

    override fun searchQueryChanged(text: String) {}

    override fun finishActMode() {}

    private fun getFileTypeSize(uri: Uri): Long {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE
        )

        var totalSize = 0L
        try {
            context.queryCursor(uri, projection) { cursor ->
                try {
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    totalSize += size
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }

        return totalSize
    }
}
