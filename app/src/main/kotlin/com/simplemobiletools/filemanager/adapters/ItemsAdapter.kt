package com.simplemobiletools.filemanager.adapters

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.formatSize
import com.simplemobiletools.filemanager.extensions.getColoredIcon
import com.simplemobiletools.filepicker.models.FileDirItem
import kotlinx.android.synthetic.main.list_item.view.*

class ItemsAdapter(context: Context, private val mItems: List<FileDirItem>) : BaseAdapter() {
    private val mInflater: LayoutInflater
    private val mFileBmp: Bitmap
    private val mDirectoryBmp: Bitmap
    private val mRes: Resources

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mRes = context.resources
        mDirectoryBmp = mRes.getColoredIcon(R.color.lightGrey, R.mipmap.directory)
        mFileBmp = mRes.getColoredIcon(R.color.lightGrey, R.mipmap.file)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item, parent, false)
            viewHolder = ViewHolder(view)
            view!!.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        val item = mItems[position]
        viewHolder.name.text = item.name

        if (item.isDirectory) {
            viewHolder.icon.setImageBitmap(mDirectoryBmp)
            viewHolder.details.text = getChildrenCnt(item)
        } else {
            viewHolder.icon.setImageBitmap(mFileBmp)
            viewHolder.details.text = item.size.formatSize()
        }

        return view
    }

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return mRes.getQuantityString(R.plurals.smtfp_items, children, children)
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(position: Int): Any {
        return mItems[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    internal class ViewHolder(view: View) {
        val name: TextView = view.item_name
        val icon: ImageView = view.item_icon
        val details: TextView = view.item_details
    }
}
