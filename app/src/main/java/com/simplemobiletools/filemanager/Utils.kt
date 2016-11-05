package com.simplemobiletools.filemanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.simplemobiletools.filepicker.extensions.getSDCardPath
import java.util.regex.Pattern

object Utils {
    fun getFilename(path: String): String {
        return path.substring(path.lastIndexOf("/") + 1)
    }

    fun getFileExtension(fileName: String): String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase()
    }

    fun showToast(context: Context, resId: Int) {
        Toast.makeText(context, context.resources.getString(resId), Toast.LENGTH_SHORT).show()
    }

    fun hasStoragePermission(cxt: Context): Boolean {
        return ContextCompat.checkSelfPermission(cxt, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun isNameValid(name: String): Boolean {
        val pattern = Pattern.compile("^[-_.A-Za-z0-9 ]+$")
        val matcher = pattern.matcher(name)
        return matcher.matches()
    }

    fun isPathOnSD(context: Context, path: String): Boolean {
        return path.startsWith(context.getSDCardPath())
    }
}
