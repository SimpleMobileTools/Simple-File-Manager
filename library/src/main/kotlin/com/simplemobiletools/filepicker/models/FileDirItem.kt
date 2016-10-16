package com.simplemobiletools.filepicker.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory

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

    val resolution: String
        get () {
            val bitmap: Bitmap? = BitmapFactory.decodeFile(path)
            if (bitmap == null)
                return ""

            return "${bitmap.width} x ${bitmap.height}"
        }
}
