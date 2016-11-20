package com.simplemobiletools.filemanager.adapters

import android.content.Intent
import android.net.Uri
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
import com.simplemobiletools.filepicker.extensions.toast
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
        val markedItems = HashSet<Int>()

        fun toggleItemSelection(itemView: View, select: Boolean, pos: Int = -1) {
            itemView.item_frame.isSelected = select
            if (pos == -1)
                return

            if (select)
                markedItems.add(pos)
            else
                markedItems.remove(pos)
        }
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.cab_properties -> {
                    showProperties()
                    true
                }
                R.id.cab_share -> {
                    shareFiles()
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

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu): Boolean {
            val menuItem = menu.findItem(R.id.cab_rename)
            menuItem.isVisible = multiSelector.selectedPositions.size <= 1

            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
            markedItems.clear()
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

    private fun shareFiles() {
        val selectedItems = getSelectedMedia().filterNot { it.isDirectory }
        val uris = ArrayList<Uri>(selectedItems.size)
        selectedItems.mapTo(uris) { Uri.fromFile(File(it.path)) }

        if (uris.isEmpty()) {
            activity.toast(R.string.no_files_selected)
            return
        }

        val shareTitle = activity.resources.getString(R.string.share_via)
        Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putExtra(Intent.EXTRA_SUBJECT, activity.resources.getString(R.string.shared_files))
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*"
            activity.startActivity(Intent.createChooser(this, shareTitle))
        }
    }

    private fun getSelectedMedia(): List<FileDirItem> {
        val positions = multiSelector.selectedPositions
        val selectedMedia = ArrayList<FileDirItem>(positions.size)
        positions.forEach { selectedMedia.add(mItems[it]) }
        return selectedMedia
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(activity, view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(multiSelectorMode, multiSelector, mItems[position], position))
    }

    override fun getItemCount() = mItems.size

    class ViewHolder(val activity: SimpleActivity, view: View, val itemClick: (FileDirItem) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, fileDirItem: FileDirItem, pos: Int): View {
            itemView.item_name.text = fileDirItem.name
            toggleItemSelection(itemView, markedItems.contains(pos), pos)

            if (fileDirItem.isDirectory) {
                Glide.with(activity).load(R.mipmap.directory).diskCacheStrategy(getCacheStrategy(fileDirItem)).centerCrop().crossFade().into(itemView.item_icon)
                itemView.item_details.text = getChildrenCnt(fileDirItem)
            } else {
                Glide.with(activity).load(fileDirItem.path).diskCacheStrategy(getCacheStrategy(fileDirItem)).error(R.mipmap.file).centerCrop().crossFade().into(itemView.item_icon)
                itemView.item_details.text = fileDirItem.size.formatSize()
            }

            itemView.setOnClickListener { viewClicked(multiSelector, fileDirItem, pos) }
            itemView.setOnLongClickListener {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    multiSelector.setSelected(this, true)
                    actMode?.title = multiSelector.selectedPositions.size.toString()
                    toggleItemSelection(itemView, true, pos)
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

        fun viewClicked(multiSelector: MultiSelector, fileDirItem: FileDirItem, pos: Int) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                toggleItemSelection(itemView, !isSelected, pos)

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
