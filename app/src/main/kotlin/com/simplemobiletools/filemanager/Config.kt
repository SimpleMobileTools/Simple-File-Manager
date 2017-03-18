package com.simplemobiletools.filemanager

import android.content.Context
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.commons.helpers.BaseConfig
import java.io.File

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showHidden: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN, false)
        set(show) = prefs.edit().putBoolean(SHOW_HIDDEN, show).apply()

    var homeFolder: String
        get(): String {
            var home = prefs.getString(HOME_FOLDER, "")
            if (home.isEmpty() || !File(home).exists() || !File(home).isDirectory) {
                home = context.getInternalStoragePath()
            }
            return home
        }
        set(homeFolder) = prefs.edit().putString(HOME_FOLDER, homeFolder).apply()
}
