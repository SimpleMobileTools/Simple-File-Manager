package com.simplemobiletools.filemanager.extensions

import android.app.Activity
import android.content.Intent
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.BuildConfig
import java.util.*

fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.tryOpenPathIntent(path: String, forceChooser: Boolean) {
    if (!forceChooser && path.endsWith(".apk", true)) {
        val uri = getFinalUriFromPath(path, BuildConfig.APPLICATION_ID) ?: return
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, getMimeTypeFromUri(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(this)
        }
    } else {
        openPath(path, forceChooser)
    }
}

fun Activity.openPath(path: String, forceChooser: Boolean) {
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID)
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}
