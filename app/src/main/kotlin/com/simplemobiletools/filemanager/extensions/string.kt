package com.simplemobiletools.filemanager.extensions

import java.util.regex.Pattern

fun String.isValidFilename(): Boolean {
    val pattern = Pattern.compile("^[-_.A-Za-z0-9()#& ]+$")
    val matcher = pattern.matcher(this)
    return matcher.matches()
}
