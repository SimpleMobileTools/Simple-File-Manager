package com.simplemobiletools.filemanager.pro.adapters

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.CONFLICT_OVERWRITE
import com.simplemobiletools.commons.helpers.CONFLICT_SKIP
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.activities.SplashActivity
import com.simplemobiletools.filemanager.pro.dialogs.CompressAsDialog
import com.simplemobiletools.filemanager.pro.extensions.*
import com.simplemobiletools.filemanager.pro.helpers.*
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.simplemobiletools.filemanager.pro.models.ListItem
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.item_list_file_dir.view.*
import kotlinx.android.synthetic.main.item_list_section.view.*
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ItemsAdapter(activity: SimpleActivity, var listItems: MutableList<ListItem>, val listener: ItemOperationsListener?, recyclerView: MyRecyclerView,
                   val isPickMultipleIntent: Boolean, fastScroller: FastScroller, itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private val TYPE_FILE_DIR = 1
    private val TYPE_SECTION = 2
    private lateinit var folderDrawable: Drawable
    private lateinit var fileDrawable: Drawable
    private var currentItemsHash = listItems.hashCode()
    private var textToHighlight = ""
    private val hasOTGConnected = activity.hasOTGConnected()
    var adjustedPrimaryColor = activity.getAdjustedPrimaryColor()

    init {
        setupDragListener(true)
        initDrawables()
    }

    override fun getActionMenuId() = R.menu.cab

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_decompress).isVisible = getSelectedFileDirItems().map { it.path }.any { it.isZipFile() }
            findItem(R.id.cab_confirm_selection).isVisible = isPickMultipleIntent
            findItem(R.id.cab_copy_path).isVisible = isOneItemSelected()
            findItem(R.id.cab_open_with).isVisible = isOneFileSelected()
            findItem(R.id.cab_open_as).isVisible = isOneFileSelected()
            findItem(R.id.cab_set_as).isVisible = isOneFileSelected()
            findItem(R.id.cab_create_shortcut).isVisible = isOreoPlus() && isOneItemSelected()

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
            R.id.cab_create_shortcut -> createShortcut()
            R.id.cab_copy_path -> copyPath()
            R.id.cab_set_as -> setAs()
            R.id.cab_open_with -> openWith()
            R.id.cab_open_as -> openAs()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> tryMoveFiles()
            R.id.cab_compress -> compressSelection()
            R.id.cab_decompress -> decompressSelection()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = listItems.filter { !it.isSectionTitle }.size

    override fun getIsItemSelectable(position: Int) = !listItems[position].isSectionTitle

    override fun getItemSelectionKey(position: Int) = listItems.getOrNull(position)?.path?.hashCode()

    override fun getItemKeyPosition(key: Int) = listItems.indexOfFirst { it.path.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun getItemViewType(position: Int): Int {
        return if (listItems[position].isSectionTitle) {
            TYPE_SECTION
        } else {
            TYPE_FILE_DIR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_SECTION) R.layout.item_list_section else R.layout.item_list_file_dir
        return createViewHolder(layout, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val fileDirItem = listItems[position]
        holder.bindView(fileDirItem, !fileDirItem.isSectionTitle, !fileDirItem.isSectionTitle) { itemView, layoutPosition ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    private fun getItemWithKey(key: Int): FileDirItem? = listItems.firstOrNull { it.path.hashCode() == key }

    fun initDrawables() {
        folderDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_folder_vector, textColor)
        fileDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_file_vector, textColor)
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
            val paths = getSelectedFileDirItems().asSequence().filter { !it.isDirectory }.map { it.path }.toMutableList() as ArrayList<String>
            if (paths.isEmpty()) {
                finishActMode()
            } else {
                listener?.selectedPaths(paths)
            }
        }
    }

    private fun displayRenameDialog() {
        val fileDirItems = getSelectedFileDirItems()
        val paths = fileDirItems.asSequence().map { it.path }.toMutableList() as ArrayList<String>
        when {
            paths.size == 1 -> {
                val oldPath = paths.first()
                RenameItemDialog(activity, oldPath) {
                    activity.config.moveFavorite(oldPath, it)
                    activity.runOnUiThread {
                        listener?.refreshItems()
                        finishActMode()
                    }
                }
            }
            fileDirItems.any { it.isDirectory } -> RenameItemsDialog(activity, paths) {
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
            else -> RenameDialog(activity, paths) {
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
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

    @SuppressLint("NewApi")
    private fun createShortcut() {
        val manager = activity.getSystemService(ShortcutManager::class.java)
        if (manager.isRequestPinShortcutSupported) {
            val path = getFirstSelectedItemPath()
            val drawable = resources.getDrawable(R.drawable.shortcut_folder).mutate()
            getShortcutImage(path, drawable) {
                val intent = Intent(activity, SplashActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
                intent.data = Uri.fromFile(File(path))

                val shortcut = ShortcutInfo.Builder(activity, path)
                        .setShortLabel(path.getFilenameFromPath())
                        .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                        .setIntent(intent)
                        .build()

                manager.requestPinShortcut(shortcut, null)
            }
        }
    }

    private fun getShortcutImage(path: String, drawable: Drawable, callback: () -> Unit) {
        val appIconColor = baseConfig.appIconColor
        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_folder_background).applyColorFilter(appIconColor)
        if (activity.getIsPathDirectory(path)) {
            callback()
        } else {
            Thread {
                val options = RequestOptions()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .fitCenter()

                val size = activity.resources.getDimension(R.dimen.shortcut_size).toInt()
                val builder = Glide.with(activity)
                        .asDrawable()
                        .load(getImagePathToLoad(path))
                        .apply(options)
                        .centerCrop()
                        .into(size, size)

                try {
                    val bitmap = builder.get()
                    drawable.findDrawableByLayerId(R.id.shortcut_folder_background).applyColorFilter(0)
                    drawable.setDrawableByLayerId(R.id.shortcut_folder_image, bitmap)
                } catch (e: Exception) {
                    val fileIcon = activity.resources.getDrawable(R.drawable.ic_file_vector)
                    drawable.setDrawableByLayerId(R.id.shortcut_folder_image, fileIcon)
                }

                activity.runOnUiThread {
                    callback()
                }
            }.start()
        }
    }

    private fun addFileUris(path: String, paths: ArrayList<String>) {
        if (activity.getIsPathDirectory(path)) {
            val shouldShowHidden = activity.config.shouldShowHidden
            if (activity.isPathOnOTG(path)) {
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
        (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
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

    private fun tryMoveFiles() {
        activity.handleDeletePasswordProtection {
            copyMoveTo(false)
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = getSelectedFileDirItems()
        val firstFile = files[0]
        val source = if (firstFile.isDirectory) firstFile.path else firstFile.getParentPath()
        FilePickerDialog(activity, source, false, activity.config.shouldShowHidden, true, true) {
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
        if (activity.isPathOnOTG(firstPath)) {
            activity.toast(R.string.unknown_error_occurred)
            return
        }

        CompressAsDialog(activity, firstPath) {
            val destination = it
            activity.handleSAFDialog(firstPath) {
                if (!it) {
                    return@handleSAFDialog
                }

                activity.toast(R.string.compressing)
                val paths = getSelectedFileDirItems().map { it.path }
                Thread {
                    if (compressPaths(paths, destination)) {
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
        if (activity.isPathOnOTG(firstPath)) {
            activity.toast(R.string.unknown_error_occurred)
            return
        }

        activity.handleSAFDialog(firstPath) {
            if (!it) {
                return@handleSAFDialog
            }

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
                    val currPath = if (entry.isDirectory) it else "${it.getParentPath().trimEnd('/')}/${entry.name}"
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
                val zipFileName = it.getFilenameFromPath()
                val newFolderName = zipFileName.subSequence(0, zipFileName.length - 4)
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val parentPath = it.getParentPath()
                    val newPath = "$parentPath/$newFolderName/${entry.name.trimEnd('/')}"

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
                if (activity.getIsPathDirectory(mainFile.absolutePath)) {
                    name = "${mainFile.name.trimEnd('/')}/"
                    zout.putNextEntry(ZipEntry(name))
                }

                while (!queue.isEmpty()) {
                    mainFile = queue.pop()
                    if (activity.getIsPathDirectory(mainFile.absolutePath)) {
                        for (file in mainFile.listFiles()) {
                            name = base.relativize(file.toURI()).path
                            if (activity.getIsPathDirectory(file.absolutePath)) {
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
        activity.handleDeletePasswordProtection {
            val selectionSize = selectedKeys.size
            val items = resources.getQuantityString(R.plurals.delete_items, selectionSize, selectionSize)
            val question = String.format(resources.getString(R.string.deletion_confirmation), items)
            ConfirmationDialog(activity, question) {
                deleteFiles()
            }
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
            if (!it) {
                return@handleSAFDialog
            }

            val files = ArrayList<FileDirItem>(selectedKeys.size)
            val positions = ArrayList<Int>()
            selectedKeys.forEach {
                activity.config.removeFavorite(getItemWithKey(it)?.path ?: "")
                val key = it
                val position = listItems.indexOfFirst { it.path.hashCode() == key }
                if (position != -1) {
                    positions.add(position)
                    files.add(listItems[position])
                }
            }

            positions.sortDescending()
            removeSelectedItems(positions)
            listener?.deleteFiles(files)
            positions.forEach {
                listItems.removeAt(it)
            }
        }
    }

    private fun getFirstSelectedItemPath() = getSelectedFileDirItems().first().path

    private fun getSelectedFileDirItems() = listItems.filter { selectedKeys.contains(it.path.hashCode()) } as ArrayList<FileDirItem>

    fun updateItems(newItems: ArrayList<ListItem>, highlightText: String = "") {
        if (newItems.hashCode() != currentItemsHash) {
            currentItemsHash = newItems.hashCode()
            textToHighlight = highlightText
            listItems = newItems.clone() as ArrayList<ListItem>
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
        if (!activity.isDestroyed && !activity.isFinishing) {
            val icon = holder.itemView.item_icon
            if (icon != null) {
                Glide.with(activity).clear(icon)
            }
        }
    }

    private fun setupView(view: View, listItem: ListItem) {
        val isSelected = selectedKeys.contains(listItem.path.hashCode())
        view.apply {
            if (listItem.isSectionTitle) {
                item_section.text = listItem.mName
                item_section.setTextColor(textColor)
            } else {
                item_frame.isSelected = isSelected
                val fileName = listItem.name
                item_name.text = if (textToHighlight.isEmpty()) fileName else fileName.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                item_name.setTextColor(textColor)
                item_details.setTextColor(textColor)
                item_date.setTextColor(textColor)

                if (listItem.isDirectory) {
                    item_icon.setImageDrawable(folderDrawable)
                    item_details.text = getChildrenCnt(listItem)
                    item_date.beGone()
                } else {
                    item_details.text = listItem.size.formatSize()
                    item_date.beVisible()
                    item_date.text = listItem.modified.formatDate(activity)

                    val options = RequestOptions()
                            .signature(listItem.mPath.getFileSignature())
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .error(fileDrawable)
                            .centerCrop()

                    val itemToLoad = getImagePathToLoad(listItem.path)
                    if (!activity.isDestroyed) {
                        Glide.with(activity).load(itemToLoad).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(item_icon)
                    }
                }
            }
        }
    }

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return activity.resources.getQuantityString(R.plurals.items, children, children)
    }

    private fun getOTGPublicPath(itemToLoad: String) = "${baseConfig.OTGTreeUri}/document/${baseConfig.OTGPartition}%3A${itemToLoad.substring(baseConfig.OTGPath.length).replace("/", "%2F")}"

    private fun getImagePathToLoad(path: String): Any {
        var itemToLoad = if (path.endsWith(".apk", true)) {
            val packageInfo = activity.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                appInfo.loadIcon(activity.packageManager)
            } else {
                path
            }
        } else {
            path
        }

        if (hasOTGConnected && itemToLoad is String && activity.isPathOnOTG(itemToLoad) && baseConfig.OTGTreeUri.isNotEmpty() && baseConfig.OTGPartition.isNotEmpty()) {
            itemToLoad = getOTGPublicPath(itemToLoad)
        }

        return itemToLoad
    }
}
