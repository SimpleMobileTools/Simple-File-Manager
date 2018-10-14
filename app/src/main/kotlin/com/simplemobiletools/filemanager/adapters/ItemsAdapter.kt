package com.simplemobiletools.filemanager.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.CONFLICT_OVERWRITE
import com.simplemobiletools.commons.helpers.CONFLICT_SKIP
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.dialogs.CompressAsDialog
import com.simplemobiletools.filemanager.extensions.*
import com.simplemobiletools.filemanager.helpers.*
import com.simplemobiletools.filemanager.interfaces.ItemOperationsListener
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
                   val isPickMultipleIntent: Boolean, fastScroller: FastScroller, itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private lateinit var folderDrawable: Drawable
    private lateinit var fileDrawable: Drawable
    private var currentItemsHash = fileDirItems.hashCode()
    private val hasOTGConnected = activity.hasOTGConnected()
    private var textToHighlight = ""
    var adjustedPrimaryColor = activity.getAdjustedPrimaryColor()

    init {
        setupDragListener(true)
        initDrawables()
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected()
            findItem(R.id.cab_decompress).isVisible = getSelectedFileDirItems().map { it.path }.any { it.isZipFile() }
            findItem(R.id.cab_confirm_selection).isVisible = isPickMultipleIntent
            findItem(R.id.cab_copy_path).isVisible = isOneItemSelected()
            findItem(R.id.cab_open_with).isVisible = isOneFileSelected()
            findItem(R.id.cab_open_as).isVisible = isOneFileSelected()
            findItem(R.id.cab_set_as).isVisible = isOneFileSelected()

            checkHideBtnVisibility(this)
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_confirm_selection -> confirmSelection()
            R.id.cab_rename -> displayRenameDialog()
            R.id.cab_properties -> showProperties()
            R.id.cab_share -> shareFiles()
            R.id.cab_hide -> toggleFileVisibility(true)
            R.id.cab_unhide -> toggleFileVisibility(false)
            R.id.cab_copy_path -> copyPath()
            R.id.cab_set_as -> setAs()
            R.id.cab_open_with -> openWith()
            R.id.cab_open_as -> openAs()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> copyMoveTo(false)
            R.id.cab_compress -> compressSelection()
            R.id.cab_decompress -> decompressSelection()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = fileDirItems.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = fileDirItems.getOrNull(position)?.path

    override fun getItemKeyPosition(key: String) = fileDirItems.indexOfFirst { it.path == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.list_item, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val fileDirItem = fileDirItems[position]
        holder.bindView(fileDirItem, true, true) { itemView, layoutPosition ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = fileDirItems.size

    private fun getItemWithKey(key: String): FileDirItem? = fileDirItems.firstOrNull { it.path == key }

    fun initDrawables() {
        folderDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_folder, textColor)
        fileDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_file, textColor)
        folderDrawable.alpha = 180
        fileDrawable.alpha = 180
    }

    private fun isOneFileSelected() = isOneItemSelected() && getItemWithKey(selectedKeys.first())?.isDirectory == false

    private fun checkHideBtnVisibility(menu: Menu) {
        var hiddenCnt = 0
        var unhiddenCnt = 0
        getSelectedFileDirItems().map { it.name }.forEach {
            if (it.startsWith(".")) {
                hiddenCnt++
            } else {
                unhiddenCnt++
            }
        }

        menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
        menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
    }

    private fun confirmSelection() {
        if (selectedKeys.isNotEmpty()) {
            val paths = getSelectedFileDirItems().filter { !it.isDirectory }.map { it.path } as ArrayList<String>
            listener?.selectedPaths(paths)
        }
    }

    private fun displayRenameDialog() {
        val oldPath = getFirstSelectedItemPath()
        RenameItemDialog(activity, oldPath) {
            activity.config.moveFavorite(oldPath, it)
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun showProperties() {
        if (selectedKeys.size <= 1) {
            PropertiesDialog(activity, getFirstSelectedItemPath(), activity.config.shouldShowHidden)
        } else {
            val paths = getSelectedFileDirItems().map { it.path }
            PropertiesDialog(activity, paths, activity.config.shouldShowHidden)
        }
    }

    private fun shareFiles() {
        val selectedItems = getSelectedFileDirItems()
        val paths = ArrayList<String>(selectedItems.size)
        selectedItems.forEach {
            addFileUris(it.path, paths)
        }
        activity.sharePaths(paths)
    }

    private fun toggleFileVisibility(hide: Boolean) {
        Thread {
            getSelectedFileDirItems().forEach {
                activity.toggleItemVisibility(it.path, hide)
            }
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }.start()
    }

    private fun addFileUris(path: String, paths: ArrayList<String>) {
        if (activity.getIsPathDirectory(path)) {
            val shouldShowHidden = activity.config.shouldShowHidden
            if (path.startsWith(OTG_PATH)) {
                activity.getDocumentFile(path)?.listFiles()?.filter { if (shouldShowHidden) true else !it.name!!.startsWith(".") }?.forEach {
                    addFileUris(it.uri.toString(), paths)
                }
            } else {
                File(path).listFiles()?.filter { if (shouldShowHidden) true else !it.isHidden }?.forEach {
                    addFileUris(it.absolutePath, paths)
                }
            }
        } else {
            paths.add(path)
        }
    }

    private fun copyPath() {
        val clip = ClipData.newPlainText(activity.getString(R.string.app_name), getFirstSelectedItemPath())
        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = clip
        finishActMode()
        activity.toast(R.string.path_copied)
    }

    private fun setAs() {
        activity.setAs(getFirstSelectedItemPath())
    }

    private fun openWith() {
        activity.tryOpenPathIntent(getFirstSelectedItemPath(), true)
    }

    private fun openAs() {
        val res = activity.resources
        val items = arrayListOf(
                RadioItem(OPEN_AS_TEXT, res.getString(R.string.text_file)),
                RadioItem(OPEN_AS_IMAGE, res.getString(R.string.image_file)),
                RadioItem(OPEN_AS_AUDIO, res.getString(R.string.audio_file)),
                RadioItem(OPEN_AS_VIDEO, res.getString(R.string.video_file)),
                RadioItem(OPEN_AS_OTHER, res.getString(R.string.other_file)))

        RadioGroupDialog(activity, items) {
            activity.tryOpenPathIntent(getFirstSelectedItemPath(), false, it as Int)
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = getSelectedFileDirItems()
        val firstFile = files[0]
        val source = if (firstFile.isDirectory) firstFile.path else firstFile.getParentPath()
        FilePickerDialog(activity, source, false, activity.config.shouldShowHidden, true) {
            if (activity.isPathOnRoot(it) || activity.isPathOnRoot(firstFile.path)) {
                copyMoveRootItems(files, it, isCopyOperation)
            } else {
                activity.copyMoveFilesTo(files, source, it, isCopyOperation, false, activity.config.shouldShowHidden) {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }

    private fun copyMoveRootItems(files: ArrayList<FileDirItem>, destinationPath: String, isCopyOperation: Boolean) {
        activity.toast(R.string.copying)
        Thread {
            val fileCnt = files.size
            RootHelpers(activity).copyMoveFiles(files, destinationPath, isCopyOperation) {
                when (it) {
                    fileCnt -> activity.toast(R.string.copying_success)
                    0 -> activity.toast(R.string.copy_failed)
                    else -> activity.toast(R.string.copying_success_partial)
                }

                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }.start()
    }

    private fun compressSelection() {
        val firstPath = getFirstSelectedItemPath()
        if (firstPath.startsWith(OTG_PATH)) {
            activity.toast(R.string.unknown_error_occurred)
            return
        }

        CompressAsDialog(activity, firstPath) {
            activity.handleSAFDialog(firstPath) {
                activity.toast(R.string.compressing)
                val paths = getSelectedFileDirItems().map { it.path }
                Thread {
                    if (compressPaths(paths, it)) {
                        activity.runOnUiThread {
                            activity.toast(R.string.compression_successful)
                            listener?.refreshItems()
                            finishActMode()
                        }
                    } else {
                        activity.toast(R.string.compressing_failed)
                    }
                }.start()
            }
        }
    }

    private fun decompressSelection() {
        val firstPath = getFirstSelectedItemPath()
        if (firstPath.startsWith(OTG_PATH)) {
            activity.toast(R.string.unknown_error_occurred)
            return
        }

        activity.handleSAFDialog(firstPath) {
            val paths = getSelectedFileDirItems().asSequence().map { it.path }.filter { it.isZipFile() }.toList()
            tryDecompressingPaths(paths) {
                if (it) {
                    activity.toast(R.string.decompression_successful)
                    activity.runOnUiThread {
                        listener?.refreshItems()
                        finishActMode()
                    }
                } else {
                    activity.toast(R.string.decompressing_failed)
                }
            }
        }
    }

    private fun tryDecompressingPaths(sourcePaths: List<String>, callback: (success: Boolean) -> Unit) {
        sourcePaths.forEach {
            try {
                val zipFile = ZipFile(it)
                val entries = zipFile.entries()
                val fileDirItems = ArrayList<FileDirItem>()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val currPath = if (entry.isDirectory) it else "${it.getParentPath()}${entry.name}"
                    val fileDirItem = FileDirItem(currPath, entry.name, entry.isDirectory, 0, entry.size)
                    fileDirItems.add(fileDirItem)
                }

                val destinationPath = fileDirItems.first().getParentPath().trimEnd('/')
                activity.checkConflicts(fileDirItems, destinationPath, 0, LinkedHashMap()) {
                    Thread {
                        decompressPaths(sourcePaths, it, callback)
                    }.start()
                }
            } catch (exception: Exception) {
                activity.showErrorToast(exception)
            }
        }
    }

    private fun decompressPaths(paths: List<String>, conflictResolutions: LinkedHashMap<String, Int>, callback: (success: Boolean) -> Unit) {
        paths.forEach {
            try {
                val zipFile = ZipFile(it)
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    var parentPath = it.getParentPath()
                    if (parentPath != OTG_PATH) {
                        parentPath = "${parentPath.trimEnd('/')}/"
                    }

                    val newPath = "$parentPath${entry.name.trimEnd('/')}"

                    val resolution = getConflictResolution(conflictResolutions, newPath)
                    val doesPathExist = activity.getDoesFilePathExist(newPath)
                    if (doesPathExist && resolution == CONFLICT_OVERWRITE) {
                        val fileDirItem = FileDirItem(newPath, newPath.getFilenameFromPath(), entry.isDirectory)
                        if (activity.getIsPathDirectory(it)) {
                            activity.deleteFolderBg(fileDirItem, false) {
                                if (it) {
                                    extractEntry(newPath, entry, zipFile)
                                } else {
                                    callback(false)
                                }
                            }
                        } else {
                            activity.deleteFileBg(fileDirItem, false) {
                                if (it) {
                                    extractEntry(newPath, entry, zipFile)
                                } else {
                                    callback(false)
                                }
                            }
                        }
                    } else if (!doesPathExist) {
                        extractEntry(newPath, entry, zipFile)
                    }
                }
                callback(true)
            } catch (e: Exception) {
                activity.showErrorToast(e)
                callback(false)
            }
        }
    }

    private fun extractEntry(newPath: String, entry: ZipEntry, zipFile: ZipFile) {
        if (entry.isDirectory) {
            if (!activity.createDirectorySync(newPath) && !activity.getDoesFilePathExist(newPath)) {
                val error = String.format(activity.getString(R.string.could_not_create_file), newPath)
                activity.showErrorToast(error)
            }
        } else {
            val ins = zipFile.getInputStream(entry)
            ins.use {
                val fos = activity.getFileOutputStreamSync(newPath, newPath.getMimeType())
                if (fos != null) {
                    ins.copyTo(fos)
                }
            }
        }
    }

    private fun getConflictResolution(conflictResolutions: LinkedHashMap<String, Int>, path: String): Int {
        return if (conflictResolutions.size == 1 && conflictResolutions.containsKey("")) {
            conflictResolutions[""]!!
        } else if (conflictResolutions.containsKey(path)) {
            conflictResolutions[path]!!
        } else {
            CONFLICT_SKIP
        }
    }

    private fun compressPaths(sourcePaths: List<String>, targetPath: String): Boolean {
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
        val selectionSize = selectedKeys.size
        val items = resources.getQuantityString(R.plurals.delete_items, selectionSize, selectionSize)
        val question = String.format(resources.getString(R.string.deletion_confirmation), items)
        ConfirmationDialog(activity, question) {
            deleteFiles()
        }
    }

    private fun deleteFiles() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val SAFPath = getFirstSelectedItemPath()
        if (activity.isPathOnRoot(SAFPath) && !RootTools.isRootAvailable()) {
            activity.toast(R.string.rooted_device_only)
            return
        }

        activity.handleSAFDialog(SAFPath) {
            val files = ArrayList<FileDirItem>(selectedKeys.size)
            val positions = ArrayList<Int>()
            selectedKeys.forEach {
                activity.config.removeFavorite(it)
                val key = it
                val position = fileDirItems.indexOfFirst { it.path == key }
                if (position != -1) {
                    positions.add(position)
                    files.add(fileDirItems[position])
                }
            }

            positions.sortDescending()
            removeSelectedItems(positions)
            listener?.deleteFiles(files)
            positions.forEach {
                fileDirItems.removeAt(it)
            }
        }
    }

    private fun getFirstSelectedItemPath() = getSelectedFileDirItems().first().path

    private fun getSelectedFileDirItems(): ArrayList<FileDirItem> {
        val selectedFileDirItems = ArrayList<FileDirItem>(selectedKeys.size)
        selectedKeys.forEach {
            getItemWithKey(it)?.apply {
                selectedFileDirItems.add(this)
            }
        }
        return selectedFileDirItems
    }

    fun updateItems(newItems: ArrayList<FileDirItem>, highlightText: String = "") {
        if (newItems.hashCode() != currentItemsHash) {
            currentItemsHash = newItems.hashCode()
            textToHighlight = highlightText
            fileDirItems = newItems.clone() as ArrayList<FileDirItem>
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
        fastScroller?.measureRecyclerView()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed) {
            Glide.with(activity).clear(holder.itemView.item_icon!!)
        }
    }

    private fun setupView(view: View, fileDirItem: FileDirItem) {
        val isSelected = isKeySelected(fileDirItem.path)
        view.apply {
            item_frame.isSelected = isSelected
            val fileName = fileDirItem.name
            item_name.text = if (textToHighlight.isEmpty()) fileName else fileName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
            item_name.setTextColor(textColor)
            item_details.setTextColor(textColor)

            if (fileDirItem.isDirectory) {
                item_icon.setImageDrawable(folderDrawable)
                item_details.text = getChildrenCnt(fileDirItem)
            } else {
                item_details.text = fileDirItem.size.formatSize()
                val path = fileDirItem.path
                val options = RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(fileDrawable)
                        .centerCrop()

                var itemToLoad = if (fileDirItem.name.endsWith(".apk", true)) {
                    val packageInfo = context.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
                    if (packageInfo != null) {
                        val appInfo = packageInfo.applicationInfo
                        appInfo.sourceDir = path
                        appInfo.publicSourceDir = path
                        appInfo.loadIcon(context.packageManager)
                    } else {
                        path
                    }
                } else {
                    path
                }

                if (!activity.isDestroyed) {
                    if (hasOTGConnected && itemToLoad is String && itemToLoad.startsWith(OTG_PATH)) {
                        itemToLoad = itemToLoad.getOTGPublicPath(activity)
                    }
                    Glide.with(activity).load(itemToLoad).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(item_icon)
                }
            }
        }
    }

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return activity.resources.getQuantityString(R.plurals.items, children, children)
    }
}
