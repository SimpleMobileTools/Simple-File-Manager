package com.simplemobiletools.filepicker.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
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

    fun isVideo(context: Context): Boolean {
        return getMimeType(context).startsWith("video")
    }

    fun isAudio(context: Context): Boolean {
        return getMimeType(context).startsWith("audio")
    }

    fun getMimeType(context: Context): String {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.fromFile(File(path)))
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (ignored: Exception) {

        }
        return ""
    }

    fun getDuration(context: Context): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(File(path)))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillisec = java.lang.Long.parseLong(time)
        return getFormattedDuration((timeInMillisec / 1000).toInt())
    }

    val imageResolution: String
        get () {
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
