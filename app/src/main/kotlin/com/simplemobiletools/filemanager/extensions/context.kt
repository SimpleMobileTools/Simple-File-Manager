package com.simplemobiletools.filemanager.extensions

import android.content.Context
import android.media.MediaScannerConnection
import android.widget.Toast
import java.io.File

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, resources.getString(id), length).show()

fun Context.rescanItem(item: File) {
    if (item.isDirectory) {
        for (child in item.listFiles()) {
            rescanItem(child)
        }
    }

    MediaScannerConnection.scanFile(this, arrayOf(item.absolutePath), null, null)
}
