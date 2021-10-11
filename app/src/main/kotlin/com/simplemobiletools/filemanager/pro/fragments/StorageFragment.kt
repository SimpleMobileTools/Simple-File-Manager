package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getProperSize
import com.simplemobiletools.commons.extensions.queryCursor
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity

class StorageFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun setupFragment(activity: SimpleActivity) {
        val imagesSize = getMediaTypeSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videosSize = getMediaTypeSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val audioSize = getMediaTypeSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getProperSize(true)
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getProperSize(true)
    }

    override fun refreshFragment() {}

    override fun setupColors(textColor: Int, primaryColor: Int) {}

    private fun getMediaTypeSize(uri: Uri): Long {
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
