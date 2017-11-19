package com.simplemobiletools.filemanager.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.BuildConfig
import java.io.File
import java.util.*

fun Activity.shareUris(uris: ArrayList<Uri>) {
    shareUris(uris, BuildConfig.APPLICATION_ID)
}

fun Activity.openFile(file: File, forceChooser: Boolean) {
    if (!forceChooser && file.absolutePath.endsWith(".apk", true)) {
        val uri = if (isNougatPlus()) {
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
        } else {
            Uri.fromFile(file)
        }

        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, getMimeTypeFromUri(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(this)
        }
    } else {
        openFile(Uri.fromFile(file), forceChooser)
    }
}

fun Activity.openFile(uri: Uri, forceChooser: Boolean) {
    openFile(uri, forceChooser, BuildConfig.APPLICATION_ID)
}

fun Activity.setAs(uri: Uri) {
    setAs(uri, BuildConfig.APPLICATION_ID)
}
