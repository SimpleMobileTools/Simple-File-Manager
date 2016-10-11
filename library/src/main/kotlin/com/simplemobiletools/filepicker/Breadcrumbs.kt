package com.simplemobiletools.filepicker

import android.content.Context
import android.graphics.Point
import android.os.Environment
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.simplemobiletools.filepicker.models.FileDirItem

class Breadcrumbs(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs), View.OnClickListener {
    private var mDeviceWidth: Int = 0

    private var mInflater: LayoutInflater? = null
    private var mListener: BreadcrumbsListener? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val deviceDisplay = Point()
        display.getSize(deviceDisplay)
        mDeviceWidth = deviceDisplay.x
    }

    fun setListener(listener: BreadcrumbsListener) {
        mListener = listener
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val paddingTop = paddingTop
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val childRight = measuredWidth - paddingRight
        val childBottom = measuredHeight - paddingBottom
        val childHeight = childBottom - paddingTop

        val usableWidth = mDeviceWidth - paddingLeft - paddingRight
        var maxHeight = 0
        var curWidth: Int
        var curHeight: Int
        var curLeft = paddingLeft
        var curTop = paddingTop

        val cnt = childCount
        for (i in 0..cnt - 1) {
            val child = getChildAt(i)

            child.measure(MeasureSpec.makeMeasureSpec(usableWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST))
            curWidth = child.measuredWidth
            curHeight = child.measuredHeight

            if (curLeft + curWidth >= childRight) {
                curLeft = paddingLeft
                curTop += maxHeight
                maxHeight = 0
            }

            child.layout(curLeft, curTop, curLeft + curWidth, curTop + curHeight)
            if (maxHeight < curHeight)
                maxHeight = curHeight

            curLeft += curWidth
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val usableWidth = mDeviceWidth - paddingLeft - paddingRight
        var width = 0
        var rowHeight = 0
        var lines = 1

        val cnt = childCount
        for (i in 0..cnt - 1) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            width += child.measuredWidth
            rowHeight = child.measuredHeight

            if (width / usableWidth > 0) {
                lines++
                width = child.measuredWidth
            }
        }

        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val calculatedHeight = paddingTop + paddingBottom + rowHeight * lines
        setMeasuredDimension(parentWidth, calculatedHeight)
    }

    fun setInitialBreadcrumb(fullPath: String) {
        val showFullPath = false//com.simplemobiletools.filemanager.Config.newInstance(context).showFullPath
        val basePath = Environment.getExternalStorageDirectory().toString()
        var tempPath = fullPath
        var currPath = basePath
        if (!showFullPath) {
            tempPath = fullPath.replace(basePath, context.getString(R.string.smtfp_initial_breadcrumb) + "/")
        } else {
            currPath = "/"
        }

        removeAllViewsInLayout()
        val dirs = tempPath.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        for (i in dirs.indices) {
            val dir = dirs[i]
            if (i > 0) {
                currPath += dir + "/"
            } else if (showFullPath) {
                addRootFolder()
            }

            if (dir.isEmpty())
                continue

            val item = FileDirItem(currPath, dir, true, 0, 0)
            addBreadcrumb(item, i > 0 || showFullPath)
        }

        if (dirs.size == 0 && showFullPath) {
            addRootFolder()
        }
    }

    fun addBreadcrumb(item: FileDirItem, addPrefix: Boolean) {
        val view = mInflater!!.inflate(R.layout.breadcrumb_item, null, false)
        val textView = view.findViewById(R.id.breadcrumb_text) as TextView

        var textToAdd = item.name
        if (addPrefix)
            textToAdd = " -> " + textToAdd
        textView.text = textToAdd
        addView(view)
        view.setOnClickListener(this)

        view.tag = item
    }

    fun removeBreadcrumb() {
        removeView(getChildAt(childCount - 1))
    }

    private fun addRootFolder() {
        val item = FileDirItem("/", "  / ", true, 0, 0)
        addBreadcrumb(item, false)
    }

    override fun onClick(v: View) {
        val cnt = childCount
        for (i in 0..cnt - 1) {
            if (getChildAt(i) != null && getChildAt(i) == v) {
                mListener?.breadcrumbClicked(i)
            }
        }
    }

    interface BreadcrumbsListener {
        fun breadcrumbClicked(id: Int)
    }
}
