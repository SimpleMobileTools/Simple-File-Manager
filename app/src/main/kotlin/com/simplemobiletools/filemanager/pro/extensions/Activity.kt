package com.simplemobiletools.filemanager.pro.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.filemanager.pro.BuildConfig
import com.simplemobiletools.filemanager.pro.helpers.*
import java.io.File
import java.util.*

fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.tryOpenPathIntent(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT) {
    if (!forceChooser && path.endsWith(".apk", true)) {
        val uri = if (isNougatPlus()) {
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", File(path))
        } else {
            Uri.fromFile(File(path))
        }

        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, getMimeTypeFromUri(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    } else {
        openPath(path, forceChooser, openAsType)
    }
}

fun Activity.openPath(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT) {
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID, getMimeType(openAsType))
}

private fun getMimeType(type: Int) = when (type) {
    OPEN_AS_DEFAULT -> ""
    OPEN_AS_TEXT -> "text/*"
    OPEN_AS_IMAGE -> "image/*"
    OPEN_AS_AUDIO -> "audio/*"
    OPEN_AS_VIDEO -> "video/*"
    else -> "*/*"
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}

fun BaseSimpleActivity.toggleItemVisibility(oldPath: String, hide: Boolean, callback: ((newPath: String) -> Unit)? = null) {
    val path = oldPath.getParentPath()
    var filename = oldPath.getFilenameFromPath()
    if ((hide && filename.startsWith('.')) || (!hide && !filename.startsWith('.'))) {
        callback?.invoke(oldPath)
        return
    }

    filename = if (hide) {
        ".${filename.trimStart('.')}"
    } else {
        filename.substring(1, filename.length)
    }

    val newPath = "$path/$filename"
    if (oldPath != newPath) {
        renameFile(oldPath, newPath) {
            callback?.invoke(newPath)
        }
    }
}
