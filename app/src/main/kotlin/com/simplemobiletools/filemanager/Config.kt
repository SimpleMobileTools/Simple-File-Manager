package com.simplemobiletools.filemanager

import android.content.Context
import android.content.SharedPreferences

class Config(context: Context) {
    private val mPrefs: SharedPreferences

    companion object {
        fun newInstance(context: Context): Config {
            return Config(context)
        }
    }

    init {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean
        get() = mPrefs.getBoolean(Constants.IS_FIRST_RUN, true)
        set(firstRun) = mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply()

    var isDarkTheme: Boolean
        get() = mPrefs.getBoolean(Constants.IS_DARK_THEME, false)
        set(isDarkTheme) = mPrefs.edit().putBoolean(Constants.IS_DARK_THEME, isDarkTheme).apply()

    var showHidden: Boolean
        get() = mPrefs.getBoolean(Constants.SHOW_HIDDEN, false)
        set(show) = mPrefs.edit().putBoolean(Constants.SHOW_HIDDEN, show).apply()

    var treeUri: String
        get() = mPrefs.getString(Constants.TREE_URI, "")
        set(uri) = mPrefs.edit().putString(Constants.TREE_URI, uri).apply()
}
