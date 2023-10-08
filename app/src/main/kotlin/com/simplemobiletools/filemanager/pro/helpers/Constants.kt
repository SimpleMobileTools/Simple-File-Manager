package com.simplemobiletools.filemanager.pro.helpers

import com.simplemobiletools.commons.helpers.TAB_FILES
import com.simplemobiletools.commons.helpers.TAB_RECENT_FILES
import com.simplemobiletools.commons.helpers.TAB_STORAGE_ANALYSIS
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.pro.models.ListItem

const val MAX_COLUMN_COUNT = 15

// shared preferences
const val SHOW_HIDDEN = "show_hidden"
const val PRESS_BACK_TWICE = "press_back_twice"
const val HOME_FOLDER = "home_folder"
const val TEMPORARILY_SHOW_HIDDEN = "temporarily_show_hidden"
const val IS_ROOT_AVAILABLE = "is_root_available"
const val ENABLE_ROOT_ACCESS = "enable_root_access"
const val EDITOR_TEXT_ZOOM = "editor_text_zoom"
const val VIEW_TYPE_PREFIX = "view_type_folder_"
const val FILE_COLUMN_CNT = "file_column_cnt"
const val FILE_LANDSCAPE_COLUMN_CNT = "file_landscape_column_cnt"
const val DISPLAY_FILE_NAMES = "display_file_names"
const val SHOW_TABS = "show_tabs"
const val WAS_STORAGE_ANALYSIS_TAB_ADDED = "was_storage_analysis_tab_added"

// open as
const val OPEN_AS_DEFAULT = 0
const val OPEN_AS_TEXT = 1
const val OPEN_AS_IMAGE = 2
const val OPEN_AS_AUDIO = 3
const val OPEN_AS_VIDEO = 4
const val OPEN_AS_OTHER = 5

const val ALL_TABS_MASK = TAB_FILES or TAB_RECENT_FILES or TAB_STORAGE_ANALYSIS

const val IMAGES = "images"
const val VIDEOS = "videos"
const val AUDIO = "audio"
const val DOCUMENTS = "documents"
const val DOWNLOADS = "downloads"
const val APPS = "apps"
const val SHOW_MIMETYPE = "show_mimetype"

// what else should we count as an audio except "audio/*" mimetype
val extraAudioMimeTypes = arrayListOf("application/ogg")
val extraDocumentMimeTypes = arrayListOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/javascript"

)

val downloadsMimeTypes = arrayListOf(
    "application/zip",
    "application/octet-stream",
    "application/json",
    "application/x-tar",
    "application/x-rar-compressed",
    "application/x-zip-compressed",
    "application/x-7z-compressed",
    "application/x-compressed",
    "application/x-gzip",
    "application/java-archive",
    "multipart/x-zip"

)

fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
    val listItems = ArrayList<ListItem>()
    fileDirItems.forEach {
        val listItem = ListItem(it.path, it.name, false, 0, it.size, it.modified, false, false)
        listItems.add(listItem)
    }
    return listItems
}
