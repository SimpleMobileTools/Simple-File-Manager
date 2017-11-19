package com.simplemobiletools.filemanager.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.SparseArray
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.dialogs.CompressAsDialog
import com.simplemobiletools.filemanager.extensions.*
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ItemsAdapter(activity: SimpleActivity, var fileDirItems: MutableList<FileDirItem>, val listener: ItemOperationsListener?, recyclerView: MyRecyclerView,
                   val isPickMultipleIntent: Boolean, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val config = activity.config
    lateinit private var folderDrawable: Drawable
    lateinit private var fileDrawable: Drawable

    init {
        selectableItemCount = fileDirItems.count()
        initDrawables()
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected()
            findItem(R.id.cab_decompress).isVisible = getSelectedMedia().map { it.path }.any { it.isZipFile() }
            findItem(R.id.cab_confirm_selection).isVisible = isPickMultipleIntent
            findItem(R.id.cab_copy_path).isVisible = isOneItemSelected()
            findItem(R.id.cab_open_with).isVisible = isOneFileSelected()
            findItem(R.id.cab_set_as).isVisible = isOneFileSelected()
        }
    }

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.item_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_confirm_selection -> confirmSelection()
            R.id.cab_rename -> displayRenameDialog()
            R.id.cab_properties -> showProperties()
            R.id.cab_share -> shareFiles()
            R.id.cab_copy_path -> copyPath()
            R.id.cab_set_as -> setAs()
            R.id.cab_open_with -> openWith()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> copyMoveTo(false)
            R.id.cab_compress -> compressSelection()
            R.id.cab_decompress -> decompressSelection()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = createViewHolder(R.layout.list_item, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val fileDirItem = fileDirItems[position]
        val view = holder.bindView(fileDirItem) {
            setupView(it, fileDirItem)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = fileDirItems.size

    fun initDrawables() {
        folderDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_folder, textColor)
        fileDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_file, textColor)
        folderDrawable.alpha = 180
        fileDrawable.alpha = 180
    }

    private fun isOneItemSelected() = selectedPositions.size == 1

    private fun isOneFileSelected() = isOneItemSelected() && !fileDirItems[selectedPositions.first()].isDirectory

    private fun confirmSelection() {
        val paths = getSelectedMedia().filter { !it.isDirectory }.map { it.path } as ArrayList<String>
        listener?.selectedPaths(paths)
    }

    private fun displayRenameDialog() {
        RenameItemDialog(activity, getSelectedMedia()[0].path) {
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun showProperties() {
        if (selectedPositions.size <= 1) {
            PropertiesDialog(activity, fileDirItems[selectedPositions.first()].path, config.shouldShowHidden)
        } else {
            val paths = ArrayList<String>()
            selectedPositions.forEach { paths.add(fileDirItems[it].path) }
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun shareFiles() {
        val selectedItems = getSelectedMedia()
        val uris = ArrayList<Uri>(selectedItems.size)
        selectedItems.forEach {
            addFileUris(File(it.path), uris)
        }
        activity.shareUris(uris)
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

    private fun copyPath() {
        val path = getSelectedMedia().first().path
        val clip = ClipData.newPlainText(activity.getString(R.string.app_name), path)
        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = clip
        finishActMode()
        activity.toast(R.string.path_copied)
    }

    private fun setAs() {
        val file = File(getSelectedMedia().first().path)
        activity.setAs(Uri.fromFile(file))
    }

    private fun openWith() {
        val file = File(getSelectedMedia().first().path)
        activity.openFile(Uri.fromFile(file), true)
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>()
        selectedPositions.forEach { files.add(File(fileDirItems[it].path)) }

        val source = if (files[0].isFile) files[0].parent else files[0].absolutePath
        FilePickerDialog(activity, source, false, config.shouldShowHidden, true) {
            if (activity.isPathOnRoot(source)) {
                copyRootItems(files, it)
            } else {
                activity.copyMoveFilesTo(files, source, it, isCopyOperation, false) {
                    listener?.refreshItems()
                    finishActMode()
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
                finishActMode()
            }
        }).start()
    }

    private fun compressSelection() {
        if (selectedPositions.isEmpty())
            return

        val firstPath = fileDirItems[selectedPositions.first()].path
        CompressAsDialog(activity, firstPath) {
            activity.handleSAFDialog(File(firstPath)) {
                activity.toast(R.string.compressing)
                val paths = selectedPositions.map { fileDirItems[it].path }
                Thread({
                    if (zipPaths(paths, it)) {
                        activity.toast(R.string.compression_successful)
                        activity.runOnUiThread {
                            listener?.refreshItems()
                            finishActMode()
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

        val firstPath = fileDirItems[selectedPositions.first()].path
        activity.handleSAFDialog(File(firstPath)) {
            activity.toast(R.string.decompressing)
            val paths = selectedPositions.map { fileDirItems[it].path }.filter { it.isZipFile() }
            Thread({
                if (unzipPaths(paths)) {
                    activity.toast(R.string.decompression_successful)
                    activity.runOnUiThread {
                        listener?.refreshItems()
                        finishActMode()
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

    private fun zipPaths(sourcePaths: List<String>, targetPath: String): Boolean {
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

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteFiles()
            finishActMode()
        }
    }

    private fun deleteFiles() {
        if (selectedPositions.isEmpty())
            return

        val files = ArrayList<File>(selectedPositions.size)
        val removeFiles = ArrayList<FileDirItem>(selectedPositions.size)

        activity.handleSAFDialog(File(fileDirItems[selectedPositions.first()].path)) {
            selectedPositions.sortedDescending().forEach {
                val file = fileDirItems[it]
                files.add(File(file.path))
                removeFiles.add(file)
                notifyItemRemoved(it)
                itemViews.put(it, null)
            }

            fileDirItems.removeAll(removeFiles)
            selectedPositions.clear()
            listener?.deleteFiles(files)

            val newItems = SparseArray<View>()
            (0 until itemViews.size())
                    .filter { itemViews[it] != null }
                    .forEachIndexed { curIndex, i -> newItems.put(curIndex, itemViews[i]) }

            itemViews = newItems
            selectableItemCount = fileDirItems.size
        }
    }

    private fun getSelectedMedia(): List<FileDirItem> {
        val selectedMedia = ArrayList<FileDirItem>(selectedPositions.size)
        selectedPositions.forEach { selectedMedia.add(fileDirItems[it]) }
        return selectedMedia
    }

    fun updateItems(newItems: MutableList<FileDirItem>) {
        fileDirItems = newItems
        selectableItemCount = fileDirItems.size
        notifyDataSetChanged()
        finishActMode()
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        if (!activity.isActivityDestroyed()) {
            Glide.with(activity).clear(holder?.itemView?.item_icon)
        }
    }

    private fun setupView(view: View, fileDirItem: FileDirItem) {
        view.apply {
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
        }
    }

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return activity.resources.getQuantityString(R.plurals.items, children, children)
    }

    interface ItemOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)

        fun selectedPaths(paths: ArrayList<String>)
    }
}
