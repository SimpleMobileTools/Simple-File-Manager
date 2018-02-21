package com.simplemobiletools.filemanager.helpers

import android.content.Context
import com.simplemobiletools.commons.extensions.getDocumentFile
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.OTG_PATH
import java.io.File

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
            var path = prefs.getString(HOME_FOLDER, "")
            if (path.isEmpty() ||
                    (path.startsWith(OTG_PATH) && context.getDocumentFile(path)?.isDirectory != true) ||
                    (!path.startsWith(OTG_PATH) && !File(path).isDirectory)) {
                path = context.getInternalStoragePath()
                homeFolder = path
            }
            return path
        }
        set(homeFolder) = prefs.edit().putString(HOME_FOLDER, homeFolder).apply()

    fun addFavorite(path: String) {
        val currFavorites = HashSet<String>(favorites)
        currFavorites.add(path)
        favorites = currFavorites
    }

    fun moveFavorite(oldPath: String, newPath: String) {
        if (!favorites.contains(oldPath)) {
            return
        }

        val currFavorites = HashSet<String>(favorites)
        currFavorites.remove(oldPath)
        currFavorites.add(newPath)
        favorites = currFavorites
    }

    fun removeFavorite(path: String) {
        if (!favorites.contains(path)) {
            return
        }

        val currFavorites = HashSet<String>(favorites)
        currFavorites.remove(path)
        favorites = currFavorites
    }

    var favorites: MutableSet<String>
        get() = prefs.getStringSet(FAVORITES, HashSet<String>())
        set(favorites) = prefs.edit().remove(FAVORITES).putStringSet(FAVORITES, favorites).apply()

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

    var isRootAvailable: Boolean
        get() = prefs.getBoolean(IS_ROOT_AVAILABLE, false)
        set(isRootAvailable) = prefs.edit().putBoolean(IS_ROOT_AVAILABLE, isRootAvailable).apply()

    var enableRootAccess: Boolean
        get() = prefs.getBoolean(ENABLE_ROOT_ACCESS, false)
        set(enableRootAccess) = prefs.edit().putBoolean(ENABLE_ROOT_ACCESS, enableRootAccess).apply()
}
