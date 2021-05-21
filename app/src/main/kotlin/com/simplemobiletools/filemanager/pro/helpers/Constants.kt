package com.simplemobiletools.filemanager.pro.helpers

import com.simplemobiletools.commons.helpers.TAB_FILES
import com.simplemobiletools.commons.helpers.TAB_RECENT_FILES

const val PATH = "path"
const val MAX_COLUMN_COUNT = 20

// shared preferences
const val SHOW_HIDDEN = "show_hidden"
const val PRESS_BACK_TWICE = "press_back_twice"
const val HOME_FOLDER = "home_folder"
const val TEMPORARILY_SHOW_HIDDEN = "temporarily_show_hidden"
const val IS_ROOT_AVAILABLE = "is_root_available"
const val ENABLE_ROOT_ACCESS = "enable_root_access"
const val EDITOR_TEXT_ZOOM = "editor_text_zoom"
const val VIEW_TYPE = "view_type"
const val VIEW_TYPE_PREFIX = "view_type_folder_"
const val FILE_COLUMN_CNT = "file_column_cnt"
const val FILE_LANDSCAPE_COLUMN_CNT = "file_landscape_column_cnt"
const val DISPLAY_FILE_NAMES = "display_file_names"
const val SHOW_TABS = "show_tabs"

// open as
const val OPEN_AS_DEFAULT = 0
const val OPEN_AS_TEXT = 1
const val OPEN_AS_IMAGE = 2
const val OPEN_AS_AUDIO = 3
const val OPEN_AS_VIDEO = 4
const val OPEN_AS_OTHER = 5

const val ALL_TABS_MASK = TAB_FILES or TAB_RECENT_FILES
val tabsList = arrayListOf(TAB_FILES, TAB_RECENT_FILES)
