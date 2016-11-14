package com.simplemobiletools.filemanager

import android.content.Context
import android.content.SharedPreferences

class Config(context: Context) {
    private val mPrefs: SharedPreferences

    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    init {
        mPrefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean
        get() = mPrefs.getBoolean(IS_FIRST_RUN, true)
        set(firstRun) = mPrefs.edit().putBoolean(IS_FIRST_RUN, firstRun).apply()

    var isDarkTheme: Boolean
        get() = mPrefs.getBoolean(IS_DARK_THEME, false)
        set(isDarkTheme) = mPrefs.edit().putBoolean(IS_DARK_THEME, isDarkTheme).apply()

    var showHidden: Boolean
        get() = mPrefs.getBoolean(SHOW_HIDDEN, false)
        set(show) = mPrefs.edit().putBoolean(SHOW_HIDDEN, show).apply()

    var treeUri: String
        get() = mPrefs.getString(TREE_URI, "")
        set(uri) = mPrefs.edit().putString(TREE_URI, uri).apply()
}
