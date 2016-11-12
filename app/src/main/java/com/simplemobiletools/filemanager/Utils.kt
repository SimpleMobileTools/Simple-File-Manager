package com.simplemobiletools.filemanager

import android.content.Context
import com.simplemobiletools.filepicker.extensions.getFileDocument
import com.simplemobiletools.filepicker.extensions.needsStupidWritePermissions
import com.simplemobiletools.filepicker.extensions.toast

class Utils {
    companion object {
        fun getFilename(path: String) = path.substring(path.lastIndexOf("/") + 1)

        fun getFileExtension(fileName: String) = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase()

        fun showToast(context: Context, resId: Int) = context.toast(resId)

        fun needsStupidWritePermissions(context: Context, path: String) = context.needsStupidWritePermissions(path)

        fun getFileDocument(context: Context, path: String, treeUri: String) = context.getFileDocument(path, treeUri)
    }
}
