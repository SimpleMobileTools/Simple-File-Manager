package com.simplemobiletools.filemanager.adapters

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.extensions.formatSize
import com.simplemobiletools.filepicker.extensions.isGif
import com.simplemobiletools.filepicker.models.FileDirItem
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.File

class ItemsAdapter(context: Context, private val mItems: List<FileDirItem>) : BaseAdapter() {
    private val mInflater: LayoutInflater
    private val mRes: Resources
    private val mContext: Context

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mContext = context
        mRes = context.resources
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
            Glide.with(mContext).load(R.mipmap.directory).diskCacheStrategy(getCacheStrategy(item)).centerCrop().crossFade().into(viewHolder.icon)
            viewHolder.details.text = getChildrenCnt(item)
        } else {
            Glide.with(mContext).load(item.path).diskCacheStrategy(getCacheStrategy(item)).error(R.mipmap.file).centerCrop().crossFade().into(viewHolder.icon)
            viewHolder.details.text = item.size.formatSize()
        }

        return view
    }

    private fun getCacheStrategy(item: FileDirItem) = if (File(item.path).isGif()) DiskCacheStrategy.NONE else DiskCacheStrategy.RESULT

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return mRes.getQuantityString(R.plurals.items, children, children)
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
