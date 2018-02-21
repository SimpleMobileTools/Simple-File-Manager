package com.simplemobiletools.filemanager.extensions

import android.content.Context
import com.simplemobiletools.commons.extensions.hasExternalSDCard
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.filemanager.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)

fun Context.isPathOnRoot(path: String) = !(path.startsWith(config.internalStoragePath) || path.startsWith(OTG_PATH) || (hasExternalSDCard() && path.startsWith(config.sdCardPath)))
