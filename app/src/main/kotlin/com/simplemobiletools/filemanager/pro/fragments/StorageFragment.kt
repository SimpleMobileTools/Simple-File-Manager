package com.simplemobiletools.filemanager.pro.fragments

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.extensions.formatSizeThousand
import kotlinx.android.synthetic.main.storage_fragment.view.*
import java.util.*
import kotlin.collections.HashMap

class StorageFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private val IMAGES = "images"
    private val VIDEOS = "videos"
    private val AUDIO = "audio"
    private val DOCUMENTS = "documents"
    private val ARCHIVES = "archives"
    private val OTHERS = "others"

    private val SIZE_DIVIDER = 100000

    // what else should we count as an audio except "audio/*" mimetype
    private val extraAudioMimeTypes = arrayListOf("application/ogg")
    private val extraDocumentMimeTypes = arrayListOf(
        "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    private val archiveMimeTypes = arrayListOf("application/zip", "application/x-tar", "application/octet-stream", "application/json")

    override fun setupFragment(activity: SimpleActivity) {
        ensureBackgroundThread {
            getMainStorageStats(activity)

            val filesSize = getSizesByMimeType()
            val imagesSize = filesSize[IMAGES]!!
            val videosSize = filesSize[VIDEOS]!!
            val audioSize = filesSize[AUDIO]!!
            val documentsSize = filesSize[DOCUMENTS]!!
            val archivesSize = filesSize[ARCHIVES]!!
            val othersSize = filesSize[OTHERS]!!

            activity.runOnUiThread {
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

    override fun refreshFragment() {}

    override fun setupColors(textColor: Int, primaryColor: Int) {
        context.updateTextColors(storage_fragment)

        main_storage_usage_progressbar.setIndicatorColor(primaryColor)
        main_storage_usage_progressbar.trackColor = primaryColor.adjustAlpha(0.3f)

        val redColor = context.resources.getColor(R.color.md_red_700)
        images_progressbar.setIndicatorColor(redColor)
        images_progressbar.trackColor = redColor.adjustAlpha(0.3f)

        val greenColor = context.resources.getColor(R.color.md_green_700)
        videos_progressbar.setIndicatorColor(greenColor)
        videos_progressbar.trackColor = greenColor.adjustAlpha(0.3f)

        val lightBlueColor = context.resources.getColor(R.color.md_light_blue_700)
        audio_progressbar.setIndicatorColor(lightBlueColor)
        audio_progressbar.trackColor = lightBlueColor.adjustAlpha(0.3f)

        val yellowColor = context.resources.getColor(R.color.md_yellow_700)
        documents_progressbar.setIndicatorColor(yellowColor)
        documents_progressbar.trackColor = yellowColor.adjustAlpha(0.3f)

        val whiteColor = context.resources.getColor(R.color.md_grey_white)
        archives_progressbar.setIndicatorColor(whiteColor)
        archives_progressbar.trackColor = whiteColor.adjustAlpha(0.3f)

        val pinkColor = context.resources.getColor(R.color.md_pink_700)
        others_progressbar.setIndicatorColor(pinkColor)
        others_progressbar.trackColor = pinkColor.adjustAlpha(0.3f)
    }

    private fun getSizesByMimeType(): HashMap<String, Long> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
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
                    val mimeType = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault()) ?: return@queryCursor
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
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
    private fun getMainStorageStats(activity: SimpleActivity) {
        val externalDirs = activity.getExternalFilesDirs(null)
        val storageManager = activity.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

        externalDirs.forEach { file ->
            val storageVolume = storageManager.getStorageVolume(file) ?: return
            if (storageVolume.isPrimary) {
                // internal storage
                val storageStatsManager = activity.getSystemService(AppCompatActivity.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                val totalSpace = storageStatsManager.getTotalBytes(uuid)
                val freeSpace = storageStatsManager.getFreeBytes(uuid)

                activity.runOnUiThread {
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
}
