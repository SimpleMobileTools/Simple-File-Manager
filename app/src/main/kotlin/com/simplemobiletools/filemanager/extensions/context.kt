package com.simplemobiletools.filemanager.extensions

import android.content.Context
import android.media.MediaScannerConnection
import android.widget.Toast
import com.simplemobiletools.filepicker.extensions.getSDCardPath
import java.io.File

fun Context.toast(id: Int) = Toast.makeText(this, resources.getString(id), Toast.LENGTH_SHORT).show()

fun Context.rescanItem(item: File) {
    if (item.isDirectory) {
        for (child in item.listFiles()) {
            rescanItem(child)
        }
    }

    MediaScannerConnection.scanFile(this, arrayOf(item.absolutePath), null, null)
}

fun Context.isPathOnSD(path: String) = path.startsWith(getSDCardPath())
