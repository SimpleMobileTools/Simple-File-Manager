package com.simplemobiletools.filepicker.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.util.*

class FileDirItem(val path: String, val name: String, val isDirectory: Boolean, val children: Int, val size: Long) :
        Comparable<FileDirItem> {

    override fun compareTo(other: FileDirItem): Int {
        if (isDirectory && !other.isDirectory) {
            return -1
        } else if (!isDirectory && other.isDirectory) {
            return 1
        }

        return name.toLowerCase().compareTo(other.name.toLowerCase())
    }

    override fun toString(): String {
        return "FileDirItem{name=$name, isDirectory=$isDirectory, path=$path, children=$children, size=$size}"
    }

    fun isImage(): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return options.outWidth != -1 && options.outHeight != -1
    }

    fun isVideo(): Boolean {
        return getMimeType().startsWith("video")
    }

    fun isAudio(): Boolean {
        return getMimeType().startsWith("audio")
    }

    fun getMimeType(): String {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (ignored: Exception) {

        }
        return ""
    }

    fun getDuration(): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillisec = java.lang.Long.parseLong(time)
        return getFormattedDuration((timeInMillisec / 1000).toInt())
    }

    fun getVideoResolution(): String {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            return "$width x $height"
        } catch (ignored: Exception) {

        }
        return ""
    }

    fun getImageResolution(): String {
        val bitmap: Bitmap? = BitmapFactory.decodeFile(path)
        if (bitmap == null)
            return ""

        return "${bitmap.width} x ${bitmap.height}"
    }

    private fun getFormattedDuration(duration: Int): String {
        val sb = StringBuilder(8)
        val hours = duration / (60 * 60)
        val minutes = duration % (60 * 60) / 60
        val seconds = duration % (60 * 60) % 60

        if (duration > 3600) {
            sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
        }

        sb.append(String.format(Locale.getDefault(), "%02d", minutes))
        sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
        return sb.toString()
    }
}
