package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.view.ScrollingView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.MainActivity
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.extensions.tryOpenPathIntent
import kotlinx.android.synthetic.main.items_fragment.view.*

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    protected var activity: SimpleActivity? = null
    protected var currentViewType = VIEW_TYPE_LIST

    var currentPath = ""
    var isGetContentIntent = false
    var isGetRingtonePicker = false
    var isPickMultipleIntent = false
    var wantedMimeType = ""
    protected var isCreateDocumentIntent = false

    protected fun clickedPath(path: String) {
        if (isGetContentIntent || isCreateDocumentIntent) {
            (activity as MainActivity).pickedPath(path)
        } else if (isGetRingtonePicker) {
            if (path.isAudioFast()) {
                (activity as MainActivity).pickedRingtone(path)
            } else {
                activity?.toast(R.string.select_audio_file)
            }
        } else {
            activity?.tryOpenPathIntent(path, false)
        }
    }

    fun updateIsCreateDocumentIntent(isCreateDocumentIntent: Boolean) {
        val iconId = if (isCreateDocumentIntent) {
            R.drawable.ic_check_vector
        } else {
            R.drawable.ic_plus_vector
        }

        this.isCreateDocumentIntent = isCreateDocumentIntent
        val fabIcon = context.resources.getColoredDrawableWithColor(iconId, context.getProperPrimaryColor().getContrastColor())
        items_fab?.setImageDrawable(fabIcon)
    }

    protected fun isProperMimeType(wantedMimeType: String, path: String, isDirectory: Boolean): Boolean {
        return if (wantedMimeType.isEmpty() || wantedMimeType == "*/*" || isDirectory) {
            true
        } else {
            val fileMimeType = path.getMimeType()
            if (wantedMimeType.endsWith("/*")) {
                fileMimeType.substringBefore("/").equals(wantedMimeType.substringBefore("/"), true)
            } else {
                fileMimeType.equals(wantedMimeType, true)
            }
        }
    }

    abstract fun setupFragment(activity: SimpleActivity)

    abstract fun onResume(textColor: Int)

    abstract fun refreshFragment()

    abstract fun getScrollingView(): ScrollingView?
}
