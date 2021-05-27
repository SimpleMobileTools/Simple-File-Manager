warning: LF will be replaced by CRLF in app/src/main/kotlin/com/simplemobiletools/filemanager/pro/activities/SettingsActivity.kt.
The file will have its original line endings in your working directory
warning: LF will be replaced by CRLF in app/src/main/kotlin/com/simplemobiletools/filemanager/pro/adapters/ItemsAdapter.kt.
The file will have its original line endings in your working directory
warning: LF will be replaced by CRLF in app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Config.kt.
The file will have its original line endings in your working directory
warning: LF will be replaced by CRLF in app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Constants.kt.
The file will have its original line endings in your working directory
warning: LF will be replaced by CRLF in app/src/main/res/layout/activity_settings.xml.
The file will have its original line endings in your working directory
warning: LF will be replaced by CRLF in app/src/main/res/values/strings.xml.
The file will have its original line endings in your working directory
[1mdiff --git a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/activities/SettingsActivity.kt b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/activities/SettingsActivity.kt[m
[1mindex c787a668..2bcb2a2a 100644[m
[1m--- a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/activities/SettingsActivity.kt[m
[1m+++ b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/activities/SettingsActivity.kt[m
[36m@@ -38,6 +38,7 @@[m [mclass SettingsActivity : SimpleActivity() {[m
         setupAppPasswordProtection()[m
         setupFileDeletionPasswordProtection()[m
         setupKeepLastModified()[m
[32m+[m[32m        setupDeleteConfirmation()[m
         setupEnableRootAccess()[m
         updateTextColors(settings_holder)[m
         setupSectionColors()[m
[36m@@ -206,6 +207,14 @@[m [mclass SettingsActivity : SimpleActivity() {[m
         }[m
     }[m
 [m
[32m+[m[32m    private fun setupDeleteConfirmation() {[m
[32m+[m[32m        settings_delete_confirmation.isChecked = config.deleteConfirmationEnabled[m
[32m+[m[32m        settings_delete_confirmation_holder.setOnClickListener {[m
[32m+[m[32m            settings_delete_confirmation.toggle()[m
[32m+[m[32m            config.deleteConfirmationEnabled = settings_delete_confirmation.isChecked[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m
     private fun setupEnableRootAccess() {[m
         settings_enable_root_access_holder.beVisibleIf(config.isRootAvailable)[m
         settings_enable_root_access.isChecked = config.enableRootAccess[m
[1mdiff --git a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/adapters/ItemsAdapter.kt b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/adapters/ItemsAdapter.kt[m
[1mindex 8b4f3ba7..832218fd 100644[m
[1m--- a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/adapters/ItemsAdapter.kt[m
[1m+++ b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/adapters/ItemsAdapter.kt[m
[36m@@ -123,7 +123,7 @@[m [mclass ItemsAdapter(activity: SimpleActivity, var listItems: MutableList<ListItem[m
             R.id.cab_compress -> compressSelection()[m
             R.id.cab_decompress -> decompressSelection()[m
             R.id.cab_select_all -> selectAll()[m
[31m-            R.id.cab_delete -> askConfirmDelete()[m
[32m+[m[32m            R.id.cab_delete -> if (config.deleteConfirmationEnabled) askConfirmDelete() else deleteFiles()[m
         }[m
     }[m
 [m
[1mdiff --git a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Config.kt b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Config.kt[m
[1mindex 7c73b2bc..8f38a143 100644[m
[1m--- a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Config.kt[m
[1m+++ b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Config.kt[m
[36m@@ -76,6 +76,11 @@[m [mclass Config(context: Context) : BaseConfig(context) {[m
         get() = prefs.getFloat(EDITOR_TEXT_ZOOM, 1.2f)[m
         set(editorTextZoom) = prefs.edit().putFloat(EDITOR_TEXT_ZOOM, editorTextZoom).apply()[m
 [m
[32m+[m[32m    var deleteConfirmationEnabled: Boolean[m
[32m+[m[32m        get() = prefs.getBoolean(DELETE_CONFIRMATION_ENABLED, true)[m
[32m+[m[32m        set(deleteConfirmationEnabled) = prefs.edit().putBoolean(DELETE_CONFIRMATION_ENABLED, deleteConfirmationEnabled).apply()[m
[32m+[m
[32m+[m
     var viewType: Int[m
         get() = prefs.getInt(VIEW_TYPE, VIEW_TYPE_LIST)[m
         set(viewTypeFiles) = prefs.edit().putInt(VIEW_TYPE, viewTypeFiles).apply()[m
[1mdiff --git a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Constants.kt b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Constants.kt[m
[1mindex 9a0c221f..fa0345c9 100644[m
[1m--- a/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Constants.kt[m
[1m+++ b/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/helpers/Constants.kt[m
[36m@@ -20,6 +20,10 @@[m [mconst val FILE_COLUMN_CNT = "file_column_cnt"[m
 const val FILE_LANDSCAPE_COLUMN_CNT = "file_landscape_column_cnt"[m
 const val DISPLAY_FILE_NAMES = "display_file_names"[m
 const val SHOW_TABS = "show_tabs"[m
[32m+[m[32mconst val DELETE_CONFIRMATION_ENABLED = "delete_confirm"[m
[32m+[m
[32m+[m
[32m+[m
 [m
 // open as[m
 const val OPEN_AS_DEFAULT = 0[m
[1mdiff --git a/app/src/main/res/layout/activity_settings.xml b/app/src/main/res/layout/activity_settings.xml[m
[1mindex dad60f05..55f091c3 100644[m
[1m--- a/app/src/main/res/layout/activity_settings.xml[m
[1m+++ b/app/src/main/res/layout/activity_settings.xml[m
[36m@@ -237,6 +237,31 @@[m
                 android:text="@string/keep_last_modified"[m
                 app:switchPadding="@dimen/medium_margin" />[m
 [m
[32m+[m
[32m+[m[32m        </RelativeLayout>[m
[32m+[m
[32m+[m[32m        <RelativeLayout[m
[32m+[m[32m            android:id="@+id/settings_delete_confirmation_holder"[m
[32m+[m[32m            android:layout_width="match_parent"[m
[32m+[m[32m            android:layout_height="wrap_content"[m
[32m+[m[32m            android:layout_marginTop="@dimen/medium_margin"[m
[32m+[m[32m            android:background="?attr/selectableItemBackground"[m
[32m+[m[32m            android:paddingStart="@dimen/normal_margin"[m
[32m+[m[32m            android:paddingTop="@dimen/activity_margin"[m
[32m+[m[32m            android:paddingEnd="@dimen/normal_margin"[m
[32m+[m[32m            android:paddingBottom="@dimen/activity_margin">[m
[32m+[m
[32m+[m[32m            <com.simplemobiletools.commons.views.MySwitchCompat[m
[32m+[m[32m                android:id="@+id/settings_delete_confirmation"[m
[32m+[m[32m                android:layout_width="match_parent"[m
[32m+[m[32m                android:layout_height="wrap_content"[m
[32m+[m[32m                android:background="@null"[m
[32m+[m[32m                android:clickable="false"[m
[32m+[m[32m                android:paddingStart="@dimen/medium_margin"[m
[32m+[m[32m                android:text="@string/delete_confirmation"[m
[32m+[m[32m                app:switchPadding="@dimen/medium_margin" />[m
[32m+[m
[32m+[m
         </RelativeLayout>[m
 [m
         <com.simplemobiletools.commons.views.MyTextView[m
[1mdiff --git a/app/src/main/res/values/strings.xml b/app/src/main/res/values/strings.xml[m
[1mindex af92bdb8..2ed64bdf 100644[m
[1m--- a/app/src/main/res/values/strings.xml[m
[1m+++ b/app/src/main/res/values/strings.xml[m
[36m@@ -44,6 +44,7 @@[m
     <!-- Settings -->[m
     <string name="enable_root_access">Enable root access</string>[m
     <string name="press_back_twice">Require pressing Back twice to leave the app</string>[m
[32m+[m[32m    <string name="delete_confirmation">Enable delete confirmation</string>[m
 [m
     <!-- Strings displayed only on Google Play Store. Optional, but good to have -->[m
     <!-- App title has to have less than 50 characters. If yo