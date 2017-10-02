package com.simplemobiletools.filemanager.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.dialogs.CompressAsDialog
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.extensions.isPathOnRoot
import com.simplemobiletools.filemanager.extensions.isZipFile
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class ItemsAdapter(val activity: SimpleActivity, var mItems: MutableList<FileDirItem>, val listener: ItemOperationsListener?, val itemClick: (FileDirItem) -> Unit) :
        RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
    private val BUFFER = 2048

    val multiSelector = MultiSelector()
    val config = activity.config

    var actMode: ActionMode? = null
    var itemViews = SparseArray<View>()
    val selectedPositions = HashSet<Int>()

    var textColor = activity.config.textColor

    private val folderDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_folder, textColor)
    private val fileDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_file, textColor)

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
        folderDrawable.alpha = 180
        fileDrawable.alpha = 180
    }

    private val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun getSelectedPositions() = selectedPositions
    }

    private val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_rename -> displayRenameDialog()
                R.id.cab_properties -> showProperties()
                R.id.cab_share -> shareFiles()
                R.id.cab_copy_to -> copyMoveTo(true)
                R.id.cab_move_to -> copyMoveTo(false)
                R.id.cab_compress -> compressSelection()
                R.id.cab_decompress -> decompressSelection()
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
            menu.findItem(R.id.cab_rename).isVisible = selectedPositions.size <= 1
            menu.findItem(R.id.cab_decompress).isVisible = getSelectedMedia().map { it.path }.any { it.isZipFile() }
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
            if (uris.size <= 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            type = getMimeType(uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                activity.startActivity(Intent.createChooser(this, shareTitle))
            } catch (exception: Exception) {
                activity.showErrorToast(exception.cause.toString())
            }
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
            if (activity.isPathOnRoot(source)) {
                copyRootItems(files, it)
            } else {
                activity.copyMoveFilesTo(files, source, it, isCopyOperation, false) {
                    listener?.refreshItems()
                    actMode?.finish()
                }
            }
        }
    }

    private fun copyRootItems(files: ArrayList<File>, destinationPath: String) {
        activity.toast(R.string.copying)
        Thread({
            var fileCnt = files.count()
            files.forEach {
                if (RootTools.copyFile(it.absolutePath, destinationPath, false, true)) {
                    fileCnt--
                }
            }

            when {
                fileCnt <= 0 -> activity.toast(R.string.copying_success)
                fileCnt == files.count() -> activity.toast(R.string.copy_failed)
                else -> activity.toast(R.string.copying_success_partial)
            }

            activity.runOnUiThread {
                listener?.refreshItems()
                actMode?.finish()
            }
        }).start()
    }

    private fun compressSelection() {
        if (selectedPositions.isEmpty())
            return

        val firstPath = mItems[selectedPositions.first()].path
        CompressAsDialog(activity, firstPath) {
            activity.handleSAFDialog(File(firstPath)) {
                activity.toast(R.string.compressing)
                val paths = selectedPositions.map { mItems[it].path }
                Thread({
                    if (zipPaths(paths, it)) {
                        activity.toast(R.string.compression_successful)
                        activity.runOnUiThread {
                            listener?.refreshItems()
                            actMode?.finish()
                        }
                    } else {
                        activity.toast(R.string.compressing_failed)
                    }
                }).start()
            }
        }
    }

    private fun decompressSelection() {
        if (selectedPositions.isEmpty())
            return

        val firstPath = mItems[selectedPositions.first()].path
        activity.handleSAFDialog(File(firstPath)) {
            activity.toast(R.string.decompressing)
            val paths = selectedPositions.map { mItems[it].path }.filter { it.isZipFile() }
            Thread({
                if (unzipPaths(paths)) {
                    activity.toast(R.string.decompression_successful)
                    activity.runOnUiThread {
                        listener?.refreshItems()
                        actMode?.finish()
                    }
                } else {
                    activity.toast(R.string.decompressing_failed)
                }
            }).start()
        }
    }

    private fun unzipPaths(sourcePaths: List<String>): Boolean {
        sourcePaths.map { File(it) }
                .forEach {
                    try {
                        val zipFile = ZipFile(it)
                        val entries = zipFile.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val file = File(it.parent, entry.name)
                            if (entry.isDirectory) {
                                if (!activity.createDirectorySync(file)) {
                                    val error = String.format(activity.getString(R.string.could_not_create_file), file.absolutePath)
                                    activity.showErrorToast(error)
                                    return false
                                }
                            } else {
                                val ins = zipFile.getInputStream(entry)
                                ins.use {
                                    val fos = activity.getFileOutputStreamSync(file.absolutePath, file.getMimeType())
                                    if (fos != null)
                                        ins.copyTo(fos)
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        activity.showErrorToast(exception)
                        return false
                    }
                }
        return true
    }

    fun zipPaths(sourcePaths: List<String>, targetPath: String): Boolean {
        val queue = LinkedList<File>()
        val fos = activity.getFileOutputStreamSync(targetPath, "application/zip") ?: return false

        val zout = ZipOutputStream(fos)
        var res: Closeable = fos

        try {
            sourcePaths.forEach {
                var name: String
                var mainFile = File(it)
                val base = mainFile.parentFile.toURI()
                res = zout
                queue.push(mainFile)
                if (mainFile.isDirectory) {
                    name = "${mainFile.name.trimEnd('/')}/"
                    zout.putNextEntry(ZipEntry(name))
                }

                while (!queue.isEmpty()) {
                    mainFile = queue.pop()
                    if (mainFile.isDirectory) {
                        for (file in mainFile.listFiles()) {
                            name = base.relativize(file.toURI()).path
                            if (file.isDirectory) {
                                queue.push(file)
                                name = "${name.trimEnd('/')}/"
                                zout.putNextEntry(ZipEntry(name))
                            } else {
                                zout.putNextEntry(ZipEntry(name))
                                FileInputStream(file).copyTo(zout)
                                zout.closeEntry()
                            }
                        }
                    } else {
                        name = if (base.path == it) it.getFilenameFromPath() else base.relativize(mainFile.toURI()).path
                        zout.putNextEntry(ZipEntry(name))
                        FileInputStream(mainFile).copyTo(zout)
                        zout.closeEntry()
                    }
                }
            }
        } catch (exception: Exception) {
            activity.showErrorToast(exception)
            return false
        } finally {
            res.close()
        }
        return true
    }

    fun selectAll() {
        val cnt = mItems.size
        for (i in 0 until cnt) {
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
            for (i in 0 until itemViews.size()) {
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
                (min until to).filter { it != from }
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
                for (i in min until from)
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
                    val options = RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .error(fileDrawable)
                            .centerCrop()

                    val path = fileDirItem.path
                    Glide.with(activity).load(path).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(item_icon)
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

        private fun viewClicked(fileDirItem: FileDirItem) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(adapterPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, adapterPosition)
            } else {
                itemClick(fileDirItem)
            }
        }

        private fun viewLongClicked() {
            if (listener != null) {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    adapterListener.toggleItemSelectionAdapter(true, adapterPosition)
                }

                listener.itemLongClicked(adapterPosition)
            }
        }

        fun stopLoad() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !activity.isDestroyed) {
                Glide.with(activity).clear(view.item_icon)
            }
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
