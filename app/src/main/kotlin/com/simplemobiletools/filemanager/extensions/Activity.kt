package com.simplemobiletools.filemanager.extensions

import android.app.Activity
import android.net.Uri
import com.simplemobiletools.commons.extensions.openFile
import com.simplemobiletools.commons.extensions.setAs
import com.simplemobiletools.commons.extensions.shareUri
import com.simplemobiletools.commons.extensions.shareUris
import com.simplemobiletools.filemanager.BuildConfig
import java.util.*


fun Activity.shareUri(uri: Uri) {
    shareUri(uri, BuildConfig.APPLICATION_ID)
}

fun Activity.shareUris(uris: ArrayList<Uri>) {
    if (uris.size == 1) {
        shareUri(uris.first())
    } else {
        shareUris(uris, BuildConfig.APPLICATION_ID)
    }
}

fun Activity.openFile(uri: Uri, forceChooser: Boolean) {
    openFile(uri, forceChooser, BuildConfig.APPLICATION_ID)
}

fun Activity.setAs(uri: Uri) {
    setAs(uri, BuildConfig.APPLICATION_ID)
}
