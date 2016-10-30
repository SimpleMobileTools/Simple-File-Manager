package com.simplemobiletools.filemanager.extensions

import android.widget.TextView

val TextView.value: String get() = this.text.toString().trim()
