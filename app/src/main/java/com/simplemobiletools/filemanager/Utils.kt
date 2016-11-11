package com.simplemobiletools.filemanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.provider.DocumentFile
import com.simplemobiletools.filepicker.extensions.getSDCardPath
import com.simplemobiletools.filepicker.extensions.toast
import java.util.regex.Pattern

class Utils {
    companion object {
        fun getFilename(path: String): String {
            return path.substring(path.lastIndexOf("/") + 1)
        }

        fun getFileExtension(fileName: String): String {
            return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase()
        }

        fun showToast(context: Context, resId: Int) {
            context.toast(resId)
        }

        fun hasStoragePermission(cxt: Context): Boolean {
            return ContextCompat.checkSelfPermission(cxt, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        fun isNameValid(name: String): Boolean {
            val pattern = Pattern.compile("^[-_.A-Za-z0-9()#& ]+$")
            val matcher = pattern.matcher(name)
            return matcher.matches()
        }

        fun needsStupidWritePermissions(context: Context, path: String) = isPathOnSD(context, path) && isKitkat() && !context.getSDCardPath().isEmpty()

        fun isPathOnSD(context: Context, path: String): Boolean {
            return path.startsWith(context.getSDCardPath())
        }

        fun isKitkat() = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT

        fun getFileDocument(context: Context, path: String): DocumentFile {
            val relativePath = path.substring(context.getSDCardPath().length + 1)
            var document = DocumentFile.fromTreeUri(context, Uri.parse(Config.newInstance(context).treeUri))
            val parts = relativePath.split("/")
            for (part in parts) {
                val currDocument = document.findFile(part)
                if (currDocument != null)
                    document = currDocument
            }
            return document
        }
    }
}
