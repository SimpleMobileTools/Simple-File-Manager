package com.simplemobiletools.filepicker.extensions

fun String.getFilenameFromPath(): String {
    return substring(lastIndexOf("/") + 1)
}
