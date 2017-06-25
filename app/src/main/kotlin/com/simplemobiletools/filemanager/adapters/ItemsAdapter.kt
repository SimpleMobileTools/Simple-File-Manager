package com.simplemobiletools.filemanager.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.File
import java.util.*

class ItemsAdapter(val activity: SimpleActivity, var mItems: MutableList<FileDirItem>, val listener: ItemOperationsListener?, val itemClick: (FileDirItem) -> Unit) :
        RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val config = activity.config

    var actMode: ActionMode? = null
    var itemViews = SparseArray<View>()
    val selectedPositions = HashSet<Int>()

    var textColor = activity.config.textColor

    lateinit var folderDrawable: Drawable
    lateinit var fileDrawable: Drawable

    fun toggleItemSelection(select: Boolean, pos: Int) {
        itemViews[pos]?.item_frame?.isSelected = select

        if (select)
            selectedPositions.add(pos)
        else
            selectedPositions.remove(pos)

        if (selectedPositions.isEmpty()) {
            actMode?.finish()
            return
        }

        updateTitle(selectedPositions.size)
    }

    fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${mItems.size}"
        actMode?.invalidate()
    }

    init {
        folderDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_folder, textColor)
        folderDrawable.alpha = 180
        fileDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_file, textColor)
        fileDrawable.alpha = 180
    }

    val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun getSelectedPositions(): HashSet<Int> = selectedPositions
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_rename -> displayRenameDialog()
                R.id.cab_properties -> showProperties()
                R.id.cab_share -> shareFiles()
                R.id.cab_copy_to -> copyMoveTo(true)
                R.id.cab_move_to -> copyMoveTo(false)
                R.id.cab_select_all -> selectAll()
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
            menuItem.isVisible = selectedPositions.size <= 1
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                itemViews[it]?.isSelected = false
            }
            selectedPositions.clear()
            actMode = null
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
        if (selectedPositions.size <= 1) {
            PropertiesDialog(activity, mItems[selectedPositions.first()].path, config.shouldShowHidden)
        } else {
            val paths = ArrayList<String>()
            selectedPositions.forEach { paths.add(mItems[it].path) }
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun shareFiles() {
        val selectedItems = getSelectedMedia()
        val uris = ArrayList<Uri>(selectedItems.size)
        selectedItems.forEach {
            val file = File(it.path)
            addFileUris(file, uris)
        }

        if (uris.isEmpty()) {
            activity.toast(R.string.no_files_selected)
            return
        }

        val shareTitle = activity.resources.getString(R.string.share_via)
        Intent().apply {
            action = if (uris.size <= 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            type = getMimeType(uris)
            activity.startActivity(Intent.createChooser(this, shareTitle))
        }
    }

    private fun addFileUris(file: File, uris: ArrayList<Uri>) {
        if (file.isDirectory) {
            file.listFiles()?.filter { if (config.shouldShowHidden) true else !it.isHidden }?.forEach {
                addFileUris(it, uris)
            }
        } else {
            uris.add(Uri.fromFile(file))
        }
    }

    private fun getMimeType(uris: List<Uri>): String {
        val firstMimeType = uris.first().path.getMimeTypeFromPath()
        val firstMimeGroup = firstMimeType.substringBefore("/")

        uris.forEach {
            val mimeGroup = it.path.getMimeTypeFromPath().substringBefore("/")
            if (mimeGroup != firstMimeGroup) {
                return "*/*"
            }
        }
        return firstMimeType
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>()
        selectedPositions.forEach { files.add(File(mItems[it].path)) }

        val source = if (files[0].isFile) files[0].parent else files[0].absolutePath
        FilePickerDialog(activity, source, false, config.shouldShowHidden, true) {
            activity.copyMoveFilesTo(files, source, it, isCopyOperation, false) {
                if (!isCopyOperation) {
                    listener?.refreshItems()
                }
                actMode?.finish()
            }
        }
    }

    fun selectAll() {
        val cnt = mItems.size
        for (i in 0..cnt - 1) {
            selectedPositions.add(i)
            notifyItemChanged(i)
        }
        updateTitle(cnt)
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteFiles()
            actMode?.finish()
        }
    }

    private fun deleteFiles() {
        if (selectedPositions.isEmpty())
            return

        val files = ArrayList<File>(selectedPositions.size)
        val removeFiles = ArrayList<FileDirItem>(selectedPositions.size)

        activity.handleSAFDialog(File(mItems[selectedPositions.first()].path)) {
            selectedPositions.sortedDescending().forEach {
                val file = mItems[it]
                files.add(File(file.path))
                removeFiles.add(file)
                notifyItemRemoved(it)
                itemViews.put(it, null)
            }

            mItems.removeAll(removeFiles)
            selectedPositions.clear()
            listener?.deleteFiles(files)

            val newItems = SparseArray<View>()
            var curIndex = 0
            for (i in 0..itemViews.size() - 1) {
                if (itemViews[i] != null) {
                    newItems.put(curIndex, itemViews[i])
                    curIndex++
                }
            }

            itemViews = newItems
        }
    }

    private fun getSelectedMedia(): List<FileDirItem> {
        val selectedMedia = ArrayList<FileDirItem>(selectedPositions.size)
        selectedPositions.forEach { selectedMedia.add(mItems[it]) }
        return selectedMedia
    }

    fun updateItems(newItems: MutableList<FileDirItem>) {
        mItems = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, listener, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        itemViews.put(position, holder.bindView(mItems[position], fileDrawable, folderDrawable, textColor))
        toggleItemSelection(selectedPositions.contains(position), position)
        holder.itemView.tag = holder
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.stopLoad()
    }

    override fun getItemCount() = mItems.size

    fun selectItem(pos: Int) {
        toggleItemSelection(true, pos)
    }

    fun selectRange(from: Int, to: Int, min: Int, max: Int) {
        if (from == to) {
            (min..max).filter { it != from }
                    .forEach { toggleItemSelection(false, it) }
            return
        }

        if (to < from) {
            for (i in to..from)
                toggleItemSelection(true, i)

            if (min > -1 && min < to) {
                (min..to - 1).filter { it != from }
                        .forEach { toggleItemSelection(false, it) }
            }
            if (max > -1) {
                for (i in from + 1..max)
                    toggleItemSelection(false, i)
            }
        } else {
            for (i in from..to)
                toggleItemSelection(true, i)

            if (max > -1 && max > to) {
                (to + 1..max).filter { it != from }
                        .forEach { toggleItemSelection(false, it) }
            }

            if (min > -1) {
                for (i in min..from - 1)
                    toggleItemSelection(false, i)
            }
        }
    }

    class ViewHolder(val view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity, val multiSelectorCallback: ModalMultiSelectorCallback,
                     val multiSelector: MultiSelector, val listener: ItemOperationsListener?, val itemClick: (FileDirItem) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(fileDirItem: FileDirItem, fileDrawable: Drawable, folderDrawable: Drawable, textColor: Int): View {
            itemView.apply {
                item_name.text = fileDirItem.name
                item_name.setTextColor(textColor)
                item_details.setTextColor(textColor)

                if (fileDirItem.isDirectory) {
                    item_icon.setImageDrawable(folderDrawable)
                    item_details.text = getChildrenCnt(fileDirItem)
                } else {
                    val path = fileDirItem.path
                    Glide.with(activity).load(path).diskCacheStrategy(path.getCacheStrategy()).error(fileDrawable).centerCrop().crossFade().into(item_icon)
                    item_details.text = fileDirItem.size.formatSize()
                }

                setOnClickListener { viewClicked(fileDirItem) }
                setOnLongClickListener { viewLongClicked(); true }
            }

            return itemView
        }

        private fun getChildrenCnt(item: FileDirItem): String {
            val children = item.children
            return activity.resources.getQuantityString(R.plurals.items, children, children)
        }

        fun viewClicked(fileDirItem: FileDirItem) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(layoutPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, layoutPosition)
            } else {
                itemClick(fileDirItem)
            }
        }

        fun viewLongClicked() {
            if (listener != null) {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    adapterListener.toggleItemSelectionAdapter(true, layoutPosition)
                }

                listener.itemLongClicked(layoutPosition)
            }
        }

        fun stopLoad() {
            Glide.clear(view.item_icon)
        }
    }

    interface MyAdapterListener {
        fun toggleItemSelectionAdapter(select: Boolean, position: Int)

        fun getSelectedPositions(): HashSet<Int>
    }

    interface ItemOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)

        fun itemLongClicked(position: Int)
    }
}
