package com.simplemobiletools.filemanager.extensions

import android.content.Context
import android.media.MediaScannerConnection
import android.widget.Toast
import java.io.File

fun Context.toast(id: Int) = Toast.makeText(this, resources.getString(id), Toast.LENGTH_SHORT).show()

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.rescanItem(item: File) {
    if (item.isDirectory) {
        for (child in item.listFiles()) {
            rescanItem(child)
        }
    }

    MediaScannerConnection.scanFile(this, arrayOf(item.absolutePath), null, null)
}
