package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplemobiletools.commons.extensions.isAudioFast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.MainActivity
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.extensions.tryOpenPathIntent

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    protected var activity: SimpleActivity? = null
    protected var currentViewType = VIEW_TYPE_LIST

    var currentPath = ""
    var isGetContentIntent = false
    var isGetRingtonePicker = false
    var isPickMultipleIntent = false

    protected fun clickedPath(path: String) {
        if (isGetContentIntent) {
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

    abstract fun setupFragment(activity: SimpleActivity)

    abstract fun onResume(textColor: Int, primaryColor: Int)

    abstract fun refreshFragment()
}
