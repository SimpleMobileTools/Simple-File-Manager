package com.simplemobiletools.filemanager

import android.content.Context
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import java.io.File
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showHidden: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN, false)
        set(show) = prefs.edit().putBoolean(SHOW_HIDDEN, show).apply()

    var temporarilyShowHidden: Boolean
        get() = prefs.getBoolean(TEMPORARILY_SHOW_HIDDEN, false)
        set(temporarilyShowHidden) = prefs.edit().putBoolean(TEMPORARILY_SHOW_HIDDEN, temporarilyShowHidden).apply()

    var shouldShowHidden = showHidden || temporarilyShowHidden

    var homeFolder: String
        get(): String {
            var home = prefs.getString(HOME_FOLDER, "")
            if (home.isEmpty() || !File(home).exists() || !File(home).isDirectory) {
                home = context.getInternalStoragePath()
            }
            return home
        }
        set(homeFolder) = prefs.edit().putString(HOME_FOLDER, homeFolder).apply()

    fun addFavorite(path: String) {
        val currFavorites = HashSet<String>(favorites)
        currFavorites.add(path)
        favorites = currFavorites
    }

    fun removeFavorite(path: String) {
        val currFavorites = HashSet<String>(favorites)
        currFavorites.remove(path)
        favorites = currFavorites
    }

    var favorites: MutableSet<String>
        get() = prefs.getStringSet(FAVORITES, HashSet<String>())
        set(favorites) = prefs.edit().remove(FAVORITES).putStringSet(FAVORITES, favorites).apply()

    var sorting: Int
        get() = prefs.getInt(SORT_ORDER, SORT_BY_NAME)
        set(sorting) = prefs.edit().putInt(SORT_ORDER, sorting).apply()

    fun saveFolderSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            sorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path, value).apply()
        }
    }

    fun getFolderSorting(path: String) = prefs.getInt(SORT_FOLDER_PREFIX + path, sorting)

    fun removeFolderSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path).apply()
    }

    fun hasCustomSorting(path: String) = prefs.contains(SORT_FOLDER_PREFIX + path)

    var enableRootAccess: Boolean
        get() = prefs.getBoolean(ENABLE_ROOT_ACCESS, false)
        set(enableRootAccess) = prefs.edit().putBoolean(ENABLE_ROOT_ACCESS, enableRootAccess).apply()
}
