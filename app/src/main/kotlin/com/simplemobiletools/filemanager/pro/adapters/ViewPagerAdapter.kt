package com.simplemobiletools.filemanager.pro.adapters

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.fragments.ItemsFragment

class ViewPagerAdapter(val activity: SimpleActivity) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = getFragment(position)
        val view = activity.layoutInflater.inflate(layout, container, false)
        container.addView(view)

        (view as ItemsFragment).apply {
            setupFragment(activity)
            setupColors(activity.config.textColor, activity.getAdjustedPrimaryColor())
        }

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }

    override fun getCount() = 1

    override fun isViewFromObject(view: View, item: Any) = view == item

    private fun getFragment(position: Int) = R.layout.items_fragment
}
