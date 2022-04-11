package com.simplemobiletools.filemanager.pro.adapters

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.TAB_FILES
import com.simplemobiletools.commons.helpers.TAB_RECENT_FILES
import com.simplemobiletools.commons.helpers.TAB_STORAGE_ANALYSIS
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.fragments.MyViewPagerFragment
import com.simplemobiletools.filemanager.pro.helpers.tabsList

class ViewPagerAdapter(val activity: SimpleActivity) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = getFragment(position)
        val view = activity.layoutInflater.inflate(layout, container, false)
        container.addView(view)

        (view as MyViewPagerFragment).apply {
            setupFragment(activity)
            onResume(activity.getProperTextColor())
        }

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }

    override fun getCount() = tabsList.filter { it and activity.config.showTabs != 0 }.size

    override fun isViewFromObject(view: View, item: Any) = view == item

    private fun getFragment(position: Int): Int {
        val showTabs = activity.config.showTabs
        val fragments = arrayListOf<Int>()
        if (showTabs and TAB_FILES != 0) {
            fragments.add(R.layout.items_fragment)
        }

        if (showTabs and TAB_RECENT_FILES != 0) {
            fragments.add(R.layout.recents_fragment)
        }

        if (showTabs and TAB_STORAGE_ANALYSIS != 0) {
            fragments.add(R.layout.storage_fragment)
        }

        return fragments[position]
    }
}
