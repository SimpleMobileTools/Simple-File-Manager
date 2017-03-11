package com.simplemobiletools.filemanager.extensions

import android.content.Context
import com.simplemobiletools.filemanager.Config

val Context.config: Config get() = Config.newInstance(this)
