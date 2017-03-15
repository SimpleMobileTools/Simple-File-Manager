package com.simplemobiletools.filemanager.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.formatSize
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.dialogs.CopyDialog
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.File
import java.util.*

class ItemsAdapter(val activity: SimpleActivity, var mItems: MutableList<FileDirItem>, val listener: ItemOperationsListener?, val itemClick: (FileDirItem) -> Unit) :
        RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()
    val config = activity.config

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()
        var textColor = 0

        lateinit var folderDrawable: Drawable
        lateinit var fileDrawable: Drawable

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

    init {
        textColor = activity.config.textColor
        folderDrawable = activity.resources.getColoredDrawableWithColor(com.simplemobiletools.commons.R.drawable.ic_folder, textColor)
        folderDrawable.alpha = 180
        fileDrawable = activity.resources.getColoredDrawableWithColor(com.simplemobiletools.commons.R.drawable.ic_file, textColor)
        fileDrawable.alpha = 180
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_rename -> displayRenameDialog()
                R.id.cab_properties -> showProperties()
                R.id.cab_share -> shareFiles()
                R.id.cab_copy_move -> displayCopyDialog()
                R.id.cab_delete -> askConfirmDelete()
                else -> return false
            }
            return true
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

    private fun displayRenameDialog() {
        RenameItemDialog(activity, getSelectedMedia()[0].path) {
            activity.runOnUiThread {
                listener?.refreshItems()
                actMode?.finish()
            }
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
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = "*/*"
            activity.startActivity(Intent.createChooser(this, shareTitle))
        }
    }

    private fun displayCopyDialog() {
        val files = ArrayList<File>()
        val positions = multiSelector.selectedPositions
        positions.forEach { files.add(File(mItems[it].path)) }

        CopyDialog(activity, files, object : CopyMoveTask.CopyMoveListener {
            override fun copySucceeded(deleted: Boolean, copiedAll: Boolean) {
                if (deleted) {
                    activity.toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
                } else {
                    activity.toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
                }
                listener?.refreshItems()
                actMode?.finish()
            }

            override fun copyFailed() {
                activity.toast(R.string.copy_move_failed)
            }
        })
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            actMode?.finish()
            deleteFiles()
        }
    }

    private fun deleteFiles() {
        val selections = multiSelector.selectedPositions
        val files = ArrayList<File>(selections.size)
        val removeFiles = ArrayList<FileDirItem>(selections.size)

        activity.handleSAFDialog(File(mItems[selections[0]].path)) {
            selections.reverse()
            selections.forEach {
                val file = mItems[it]
                files.add(File(file.path))
                removeFiles.add(file)
                notifyItemRemoved(it)
            }

            mItems.removeAll(removeFiles)
            markedItems.clear()
            listener?.deleteFiles(files)
        }
    }

    private fun getSelectedMedia(): List<FileDirItem> {
        val positions = multiSelector.selectedPositions
        val selectedMedia = ArrayList<FileDirItem>(positions.size)
        positions.forEach { selectedMedia.add(mItems[it]) }
        return selectedMedia
    }

    fun updateItems(newItems: MutableList<FileDirItem>) {
        mItems = newItems
        notifyDataSetChanged()
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
            itemView.apply {
                item_name.text = fileDirItem.name
                item_name.setTextColor(textColor)
                item_details.setTextColor(textColor)

                toggleItemSelection(this, markedItems.contains(pos), pos)

                if (fileDirItem.isDirectory) {
                    item_icon.setImageDrawable(folderDrawable)
                    item_details.text = getChildrenCnt(fileDirItem)
                } else {
                    Glide.with(activity).load(fileDirItem.path).diskCacheStrategy(getCacheStrategy(fileDirItem)).error(fileDrawable).centerCrop().crossFade().into(item_icon)
                    item_details.text = fileDirItem.size.formatSize()
                }

                setOnClickListener { viewClicked(multiSelector, fileDirItem, pos) }
                setOnLongClickListener {
                    if (!multiSelector.isSelectable) {
                        activity.startSupportActionMode(multiSelectorCallback)
                        multiSelector.setSelected(this@ViewHolder, true)
                        actMode?.title = multiSelector.selectedPositions.size.toString()
                        toggleItemSelection(this, true, pos)
                        actMode?.invalidate()
                    }
                    true
                }
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

    interface ItemOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)
    }
}
