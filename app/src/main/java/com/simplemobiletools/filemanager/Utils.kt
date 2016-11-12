package com.simplemobiletools.filemanager

import android.content.Context
import com.simplemobiletools.filepicker.extensions.*
import java.io.File
import java.util.*

class Utils {
    companion object {
        fun getFilename(path: String) = path.substring(path.lastIndexOf("/") + 1)

        fun getFileExtension(fileName: String) = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase()

        fun showToast(context: Context, resId: Int) = context.toast(resId)

        fun needsStupidWritePermissions(context: Context, path: String) = context.needsStupidWritePermissions(path)

        fun getFileDocument(context: Context, path: String, treeUri: String) = context.getFileDocument(path, treeUri)

        fun scanFile(context: Context, file: File) = context.scanFile(file) {}

        fun scanFiles(context: Context, files: ArrayList<File>) = context.scanFiles(files) {}
    }
}
