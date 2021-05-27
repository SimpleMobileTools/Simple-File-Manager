package com.simplemobiletools.filemanager.pro.helpers

import android.content.Context
import android.content.res.Configuration
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
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

    var pressBackTwice: Boolean
        get() = prefs.getBoolean(PRESS_BACK_TWICE, true)
        set(pressBackTwice) = prefs.edit().putBoolean(PRESS_BACK_TWICE, pressBackTwice).apply()

    var homeFolder: String
        get(): String {
            var path = prefs.getString(HOME_FOLDER, "")!!
            if (path.isEmpty() || !File(path).isDirectory) {
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

    var isRootAvailable: Boolean
        get() = prefs.getBoolean(IS_ROOT_AVAILABLE, false)
        set(isRootAvailable) = prefs.edit().putBoolean(IS_ROOT_AVAILABLE, isRootAvailable).apply()

    var enableRootAccess: Boolean
        get() = prefs.getBoolean(ENABLE_ROOT_ACCESS, false)
        set(enableRootAccess) = prefs.edit().putBoolean(ENABLE_ROOT_ACCESS, enableRootAccess).apply()

    var editorTextZoom: Float
        get() = prefs.getFloat(EDITOR_TEXT_ZOOM, 1.2f)
        set(editorTextZoom) = prefs.edit().putFloat(EDITOR_TEXT_ZOOM, editorTextZoom).apply()

    var deleteConfirmationEnabled: Boolean
        get() = prefs.getBoolean(DELETE_CONFIRMATION_ENABLED, true)
        set(deleteConfirmationEnabled) = prefs.edit().putBoolean(DELETE_CONFIRMATION_ENABLED, deleteConfirmationEnabled).apply()


    var viewType: Int
        get() = prefs.getInt(VIEW_TYPE, VIEW_TYPE_LIST)
        set(viewTypeFiles) = prefs.edit().putInt(VIEW_TYPE, viewTypeFiles).apply()

    fun saveFolderViewType(path: String, value: Int) {
        if (path.isEmpty()) {
            viewType = value
        } else {
            prefs.edit().putInt(VIEW_TYPE_PREFIX + path.toLowerCase(), value).apply()
        }
    }

    fun getFolderViewType(path: String) = prefs.getInt(VIEW_TYPE_PREFIX + path.toLowerCase(), viewType)

    fun removeFolderViewType(path: String) {
        prefs.edit().remove(VIEW_TYPE_PREFIX + path.toLowerCase()).apply()
    }

    fun hasCustomViewType(path: String) = prefs.contains(VIEW_TYPE_PREFIX + path.toLowerCase())

    var fileColumnCnt: Int
        get() = prefs.getInt(getFileColumnsField(), getDefaultFileColumnCount())
        set(fileColumnCnt) = prefs.edit().putInt(getFileColumnsField(), fileColumnCnt).apply()

    private fun getFileColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            FILE_COLUMN_CNT
        } else {
            FILE_LANDSCAPE_COLUMN_CNT
        }
    }

    private fun getDefaultFileColumnCount(): Int {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) 3 else 5
    }

    var displayFilenames: Boolean
        get() = prefs.getBoolean(DISPLAY_FILE_NAMES, true)
        set(displayFilenames) = prefs.edit().putBoolean(DISPLAY_FILE_NAMES, displayFilenames).apply()

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()
}
