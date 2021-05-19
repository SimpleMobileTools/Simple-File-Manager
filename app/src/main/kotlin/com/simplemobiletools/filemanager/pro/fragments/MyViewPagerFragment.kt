package com.simplemobiletools.filemanager.pro.fragments

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    var currentPath = ""
    var isGetContentIntent = false
    var isGetRingtonePicker = false
    var isPickMultipleIntent = false

    abstract fun setupFragment(activity: SimpleActivity)

    abstract fun setupColors(textColor: Int, adjustedPrimaryColor: Int)

    abstract fun setupFontSize()

    abstract fun setupDateTimeFormat()

    abstract fun searchQueryChanged(text: String)

    abstract fun finishActMode()

    abstract fun toggleFilenameVisibility()

    abstract fun increaseColumnCount()

    abstract fun reduceColumnCount()

    abstract fun refreshItems()
}
