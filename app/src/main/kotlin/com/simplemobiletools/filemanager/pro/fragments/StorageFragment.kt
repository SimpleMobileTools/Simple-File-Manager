package com.simplemobiletools.filemanager.pro.fragments

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
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

    @SuppressLint("NewApi")
    private fun getStorageStats() {
        if (activity == null) {
            return
        }

        val externalDirs = activity!!.getExternalFilesDirs(null)
        val storageManager = activity!!.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

        externalDirs.forEach { file ->
            val storageVolume = storageManager.getStorageVolume(file) ?: return
            if (storageVolume.isPrimary) {
                // internal storage
                val storageStatsManager = activity!!.getSystemService(AppCompatActivity.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                val totalSpace = storageStatsManager.getTotalBytes(uuid)
                val freeSpace = storageStatsManager.getFreeBytes(uuid)
            } else {
                // sd card
                val totalSpace = file.totalSpace
                val freeSpace = file.freeSpace
            }
        }
    }
}
