package com.simplemobiletools.filemanager.adapters

import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filepicker.extensions.formatSize
import com.simplemobiletools.filepicker.extensions.isGif
import com.simplemobiletools.filepicker.models.FileDirItem
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.File
import java.util.*

class ItemsAdapter(val activity: SimpleActivity, val mItems: List<FileDirItem>, val itemClick: (FileDirItem) -> Unit) :
        RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()
    val config = Config.newInstance(activity)

    companion object {
        var actMode: ActionMode? = null

        fun toggleItemSelection(itemView: View, select: Boolean) {
            itemView.item_frame.isSelected = select
        }
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.cab_properties -> {
                    showProperties()
                    true
                }
                else -> false
            }
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu) = true

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
        }
    }

    private fun showProperties() {
        val selections = multiSelector.selectedPositions
        if (selections.size <= 1) {
            PropertiesDialog(activity, mItems[selections[0]].path, config.showHidden)
        } else {
            val paths = ArrayList<String>()
            selections.forEach { paths.add(mItems[it].path) }
            PropertiesDialog(activity, paths, config.showHidden)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(activity, view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(multiSelectorMode, multiSelector, mItems[position]))
    }

    override fun getItemCount() = mItems.size

    class ViewHolder(val activity: SimpleActivity, view: View, val itemClick: (FileDirItem) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, fileDirItem: FileDirItem): View {
            itemView.item_name.text = fileDirItem.name

            if (fileDirItem.isDirectory) {
                Glide.with(activity).load(R.mipmap.directory).diskCacheStrategy(getCacheStrategy(fileDirItem)).centerCrop().crossFade().into(itemView.item_icon)
                itemView.item_details.text = getChildrenCnt(fileDirItem)
            } else {
                Glide.with(activity).load(fileDirItem.path).diskCacheStrategy(getCacheStrategy(fileDirItem)).error(R.mipmap.file).centerCrop().crossFade().into(itemView.item_icon)
                itemView.item_details.text = fileDirItem.size.formatSize()
            }

            itemView.setOnClickListener { viewClicked(multiSelector, fileDirItem) }
            itemView.setOnLongClickListener {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    multiSelector.setSelected(this, true)
                    actMode?.title = multiSelector.selectedPositions.size.toString()
                    toggleItemSelection(itemView, true)
                    actMode?.invalidate()
                }
                true
            }

            return itemView
        }

        private fun getCacheStrategy(item: FileDirItem) = if (File(item.path).isGif()) DiskCacheStrategy.NONE else DiskCacheStrategy.RESULT

        private fun getChildrenCnt(item: FileDirItem): String {
            val children = item.children
            return activity.resources.getQuantityString(R.plurals.items, children, children)
        }

        fun viewClicked(multiSelector: MultiSelector, fileDirItem: FileDirItem) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                toggleItemSelection(itemView, !isSelected)

                val selectedCnt = multiSelector.selectedPositions.size
                if (selectedCnt == 0) {
                    actMode?.finish()
                } else {
                    actMode?.title = selectedCnt.toString()
                }
                actMode?.invalidate()
            } else {
                itemClick(fileDirItem)
            }
        }
    }
}
