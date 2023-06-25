package com.simplemobiletools.filemanager.pro.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.usage.StorageStatsManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.MimeTypesActivity
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.formatSizeThousand
import com.simplemobiletools.filemanager.pro.helpers.*
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.simplemobiletools.filemanager.pro.models.ListItem
import kotlinx.android.synthetic.main.storage_fragment.view.*
import java.util.*

class StorageFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet), ItemOperationsListener {
    private val SIZE_DIVIDER = 100000
    private var allDeviceListItems = ArrayList<ListItem>()
    private var lastSearchedText = ""

    override fun setupFragment(activity: SimpleActivity) {
        if (this.activity == null) {
            this.activity = activity
        }

        total_space.text = String.format(context.getString(R.string.total_storage), "…")
        getSizes()

        free_space_holder.setOnClickListener {
            try {
                val storageSettingsIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                activity.startActivity(storageSettingsIntent)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }

        images_holder.setOnClickListener { launchMimetypeActivity(IMAGES) }
        videos_holder.setOnClickListener { launchMimetypeActivity(VIDEOS) }
        audio_holder.setOnClickListener { launchMimetypeActivity(AUDIO) }
        documents_holder.setOnClickListener { launchMimetypeActivity(DOCUMENTS) }
        archives_holder.setOnClickListener { launchMimetypeActivity(ARCHIVES) }
        others_holder.setOnClickListener { launchMimetypeActivity(OTHERS) }

        Handler().postDelayed({
            refreshFragment()
        }, 2000)
    }

    override fun onResume(textColor: Int) {
        getSizes()
        context.updateTextColors(storage_fragment)

        val properPrimaryColor = context.getProperPrimaryColor()
        main_storage_usage_progressbar.setIndicatorColor(properPrimaryColor)
        main_storage_usage_progressbar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)

        val redColor = context.resources.getColor(R.color.md_red_700)
        images_progressbar.setIndicatorColor(redColor)
        images_progressbar.trackColor = redColor.adjustAlpha(LOWER_ALPHA)

        val greenColor = context.resources.getColor(R.color.md_green_700)
        videos_progressbar.setIndicatorColor(greenColor)
        videos_progressbar.trackColor = greenColor.adjustAlpha(LOWER_ALPHA)

        val lightBlueColor = context.resources.getColor(R.color.md_light_blue_700)
        audio_progressbar.setIndicatorColor(lightBlueColor)
        audio_progressbar.trackColor = lightBlueColor.adjustAlpha(LOWER_ALPHA)

        val yellowColor = context.resources.getColor(R.color.md_yellow_700)
        documents_progressbar.setIndicatorColor(yellowColor)
        documents_progressbar.trackColor = yellowColor.adjustAlpha(LOWER_ALPHA)

        val tealColor = context.resources.getColor(R.color.md_teal_700)
        archives_progressbar.setIndicatorColor(tealColor)
        archives_progressbar.trackColor = tealColor.adjustAlpha(LOWER_ALPHA)

        val pinkColor = context.resources.getColor(R.color.md_pink_700)
        others_progressbar.setIndicatorColor(pinkColor)
        others_progressbar.trackColor = pinkColor.adjustAlpha(LOWER_ALPHA)

        search_holder.setBackgroundColor(context.getProperBackgroundColor())
        progress_bar.setIndicatorColor(properPrimaryColor)
        progress_bar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)
    }

    private fun launchMimetypeActivity(mimetype: String) {
        Intent(context, MimeTypesActivity::class.java).apply {
            putExtra(SHOW_MIMETYPE, mimetype)
            context.startActivity(this)
        }
    }

    private fun getSizes() {
        if (!isOreoPlus()) {
            return
        }

        ensureBackgroundThread {
            getMainStorageStats(context)

            val filesSize = getSizesByMimeType()
            val imagesSize = filesSize[IMAGES]!!
            val videosSize = filesSize[VIDEOS]!!
            val audioSize = filesSize[AUDIO]!!
            val documentsSize = filesSize[DOCUMENTS]!!
            val archivesSize = filesSize[ARCHIVES]!!
            val othersSize = filesSize[OTHERS]!!

            post {
                images_size.text = imagesSize.formatSize()
                images_progressbar.progress = (imagesSize / SIZE_DIVIDER).toInt()

                videos_size.text = videosSize.formatSize()
                videos_progressbar.progress = (videosSize / SIZE_DIVIDER).toInt()

                audio_size.text = audioSize.formatSize()
                audio_progressbar.progress = (audioSize / SIZE_DIVIDER).toInt()

                documents_size.text = documentsSize.formatSize()
                documents_progressbar.progress = (documentsSize / SIZE_DIVIDER).toInt()

                archives_size.text = archivesSize.formatSize()
                archives_progressbar.progress = (archivesSize / SIZE_DIVIDER).toInt()

                others_size.text = othersSize.formatSize()
                others_progressbar.progress = (othersSize / SIZE_DIVIDER).toInt()
            }
        }
    }

    private fun getSizesByMimeType(): HashMap<String, Long> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        var imagesSize = 0L
        var videosSize = 0L
        var audioSize = 0L
        var documentsSize = 0L
        var archivesSize = 0L
        var othersSize = 0L
        try {
            context.queryCursor(uri, projection) { cursor ->
                try {
                    val mimeType = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault())
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    if (mimeType == null) {
                        if (size > 0 && size != 4096L) {
                            val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                            if (!context.getIsPathDirectory(path)) {
                                othersSize += size
                            }
                        }
                        return@queryCursor
                    }

                    when (mimeType.substringBefore("/")) {
                        "image" -> imagesSize += size
                        "video" -> videosSize += size
                        "audio" -> audioSize += size
                        "text" -> documentsSize += size
                        else -> {
                            when {
                                extraDocumentMimeTypes.contains(mimeType) -> documentsSize += size
                                extraAudioMimeTypes.contains(mimeType) -> audioSize += size
                                archiveMimeTypes.contains(mimeType) -> archivesSize += size
                                else -> othersSize += size
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }

        val mimeTypeSizes = HashMap<String, Long>().apply {
            put(IMAGES, imagesSize)
            put(VIDEOS, videosSize)
            put(AUDIO, audioSize)
            put(DOCUMENTS, documentsSize)
            put(ARCHIVES, archivesSize)
            put(OTHERS, othersSize)
        }

        return mimeTypeSizes
    }

    @SuppressLint("NewApi")
    private fun getMainStorageStats(context: Context) {
        val externalDirs = context.getExternalFilesDirs(null)
        val storageManager = context.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

        externalDirs.forEach { file ->
            val storageVolume = storageManager.getStorageVolume(file) ?: return
            if (storageVolume.isPrimary) {
                // internal storage
                val storageStatsManager = context.getSystemService(AppCompatActivity.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                val totalSpace = storageStatsManager.getTotalBytes(uuid)
                val freeSpace = storageStatsManager.getFreeBytes(uuid)

                post {
                    arrayOf(
                        main_storage_usage_progressbar, images_progressbar, videos_progressbar, audio_progressbar, documents_progressbar,
                        archives_progressbar, others_progressbar
                    ).forEach {
                        it.max = (totalSpace / SIZE_DIVIDER).toInt()
                    }

                    main_storage_usage_progressbar.progress = ((totalSpace - freeSpace) / SIZE_DIVIDER).toInt()

                    main_storage_usage_progressbar.beVisible()
                    free_space_value.text = freeSpace.formatSizeThousand()
                    total_space.text = String.format(context.getString(R.string.total_storage), totalSpace.formatSizeThousand())
                    free_space_label.beVisible()
                }
            } else {
                // sd card
                val totalSpace = file.totalSpace
                val freeSpace = file.freeSpace
            }
        }
    }

    override fun searchQueryChanged(text: String) {
        lastSearchedText = text

        if (text.isNotEmpty()) {
            if (search_holder.alpha < 1f) {
                search_holder.fadeIn()
            }
        } else {
            search_holder.animate().alpha(0f).setDuration(SHORT_ANIMATION_DURATION).withEndAction {
                search_holder.beGone()
                (search_results_list.adapter as? ItemsAdapter)?.updateItems(allDeviceListItems, text)
            }.start()
        }

        if (text.length == 1) {
            search_results_list.beGone()
            search_placeholder.beVisible()
            search_placeholder_2.beVisible()
            hideProgressBar()
        } else if (text.isEmpty()) {
            search_results_list.beGone()
            hideProgressBar()
        } else {
            showProgressBar()
            ensureBackgroundThread {
                val start = System.currentTimeMillis()
                val filtered = allDeviceListItems.filter { it.mName.contains(text, true) }.toMutableList() as ArrayList<ListItem>
                if (lastSearchedText != text) {
                    return@ensureBackgroundThread
                }

                (context as? Activity)?.runOnUiThread {
                    (search_results_list.adapter as? ItemsAdapter)?.updateItems(filtered, text)
                    search_results_list.beVisible()
                    search_placeholder.beVisibleIf(filtered.isEmpty())
                    search_placeholder_2.beGone()
                    hideProgressBar()
                }
            }
        }
    }

    private fun setupLayoutManager() {
        if (context!!.config.getFolderViewType("") == VIEW_TYPE_GRID) {
            currentViewType = VIEW_TYPE_GRID
            setupGridLayoutManager()
        } else {
            currentViewType = VIEW_TYPE_LIST
            setupListLayoutManager()
        }

        search_results_list.adapter = null
        addItems()
    }

    private fun setupGridLayoutManager() {
        val layoutManager = search_results_list.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = context?.config?.fileColumnCnt ?: 3
    }

    private fun setupListLayoutManager() {
        val layoutManager = search_results_list.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
    }

    private fun addItems() {
        ItemsAdapter(context as SimpleActivity, ArrayList(), this, search_results_list, false, null, false) {
            clickedPath((it as FileDirItem).path)
        }.apply {
            search_results_list.adapter = this
        }
    }

    private fun getAllFiles(): ArrayList<FileDirItem> {
        val fileDirItems = ArrayList<FileDirItem>()
        val showHidden = context?.config?.shouldShowHidden() ?: return fileDirItems
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        try {
            if (isOreoPlus()) {
                val queryArgs = bundleOf(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED),
                    ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                context?.contentResolver?.query(uri, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                context?.contentResolver?.query(uri, projection, null, null, sortOrder)
            }?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME)
                            if (!showHidden && name.startsWith(".")) {
                                continue
                            }

                            val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                            if (size == 0L) {
                                continue
                            }

                            val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                            val lastModified = cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED) * 1000
                            fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                        } catch (e: Exception) {
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            context?.showErrorToast(e)
        }

        return fileDirItems
    }

    private fun showProgressBar() {
        progress_bar.show()
    }

    private fun hideProgressBar() {
        progress_bar.hide()
    }

    private fun getRecyclerAdapter() = search_results_list.adapter as? ItemsAdapter

    override fun refreshFragment() {
        ensureBackgroundThread {
            val fileDirItems = getAllFiles()
            allDeviceListItems = getListItemsFromFileDirItems(fileDirItems)
        }
        setupLayoutManager()
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {
        handleFileDeleting(files, false)
    }

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun setupDateTimeFormat() {
        getRecyclerAdapter()?.updateDateTimeFormat()
    }

    override fun setupFontSize() {
        getRecyclerAdapter()?.updateFontSizes()
    }

    override fun toggleFilenameVisibility() {
        getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
    }

    override fun columnCountChanged() {
        (search_results_list.layoutManager as MyGridLayoutManager).spanCount = context!!.config.fileColumnCnt
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, listItems.size)
        }
    }

    override fun finishActMode() {
        getRecyclerAdapter()?.finishActMode()
    }
}
