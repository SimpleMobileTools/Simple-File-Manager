package com.simplemobiletools.filemanager.pro.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.EnterPasswordDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.adapters.DecompressItemsAdapter
import com.simplemobiletools.filemanager.pro.databinding.ActivityDecompressBinding
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.models.ListItem
import com.simplemobiletools.filemanager.pro.services.CompressionService
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.exception.ZipException.Type
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.LocalFileHeader
import java.io.BufferedInputStream

class DecompressActivity : SimpleActivity() {
    companion object {
        private const val PASSWORD = "password"
    }

    private val binding by viewBinding(ActivityDecompressBinding::inflate)
    private val allFiles = ArrayList<ListItem>()
    private var currentPath = ""
    private var uri: Uri? = null
    private var password: String? = null
    private var passwordDialog: EnterPasswordDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        binding.apply {
            updateMaterialActivityViews(decompressCoordinator, decompressList, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(decompressList, decompressToolbar)
        }

        uri = intent.data
        if (uri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        password = savedInstanceState?.getString(PASSWORD, null)

        val realPath = getRealPathFromURI(uri!!)
        binding.decompressToolbar.title = realPath?.getFilenameFromPath() ?: Uri.decode(uri.toString().getFilenameFromPath())
        setupFilesList()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.decompressToolbar, NavigationIcon.Arrow)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PASSWORD, password)
    }

    private fun setupOptionsMenu() {
        binding.decompressToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.decompress -> decompressFiles()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupFilesList() {
        fillAllListItems(uri!!)
        updateCurrentPath("")
    }

    override fun onBackPressed() {
        if (currentPath.isEmpty()) {
            super.onBackPressed()
        } else {
            val newPath = if (currentPath.contains("/")) currentPath.getParentPath() else ""
            updateCurrentPath(newPath)
        }
    }

    private fun updateCurrentPath(path: String) {
        currentPath = path
        try {
            val listItems = getFolderItems(currentPath)
            val existingAdapter = (binding.decompressList.adapter as? DecompressItemsAdapter)
            if (existingAdapter != null) {
                existingAdapter.updateItems(listItems)
            } else {
                DecompressItemsAdapter(this, listItems, binding.decompressList) {
                    if ((it as ListItem).isDirectory) {
                        updateCurrentPath(it.path)
                    }
                }.apply {
                    binding.decompressList.adapter = this
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun decompressFiles() {
        val defaultFolder = getRealPathFromURI(uri!!) ?: internalStoragePath
        FilePickerDialog(this, defaultFolder, false, config.showHidden, true, true, showFavoritesButton = true) { destination ->
            handleSAFDialog(destination) {
                if (it) {
                    Intent(this, CompressionService::class.java).apply {
                        action = CompressionService.ACTION_DECOMPRESS
                        putExtra(CompressionService.EXTRA_URI, uri!!)
                        putExtra(CompressionService.EXTRA_PASSWORD, password)
                        putExtra(CompressionService.EXTRA_DESTINATION, destination)
                        startService(this)
                    }
                }
            }
        }
    }

    private fun getFolderItems(parent: String): ArrayList<ListItem> {
        return allFiles.filter {
            val fileParent = if (it.path.contains("/")) {
                it.path.getParentPath()
            } else {
                ""
            }

            fileParent == parent
        }.sortedWith(compareBy({ !it.isDirectory }, { it.mName })).toMutableList() as ArrayList<ListItem>
    }

    @SuppressLint("NewApi")
    private fun fillAllListItems(uri: Uri) {
        binding.progressBar.beVisible()
        ensureBackgroundThread {
            val inputStream = try {
                contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                runOnUiThread {
                    showErrorToast(e)
                    binding.progressBar.beGone()
                }
                return@ensureBackgroundThread
            }

            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
            if (password != null) {
                zipInputStream.setPassword(password?.toCharArray())
            }
            var zipEntry: LocalFileHeader?
            while (true) {
                try {
                    zipEntry = zipInputStream.nextEntry
                } catch (passwordException: ZipException) {
                    if (passwordException.type == Type.WRONG_PASSWORD) {
                        if (password != null) {
                            runOnUiThread {
                                toast(getString(R.string.invalid_password))
                                binding.progressBar.beGone()
                                if (passwordDialog == null) {
                                    askForPassword()
                                } else {
                                    passwordDialog?.clearPassword()
                                }
                            }
                        } else {
                            runOnUiThread {
                                binding.progressBar.beGone()
                                askForPassword()
                            }
                        }
                        return@ensureBackgroundThread
                    } else {
                        break
                    }
                } catch (ignored: Exception) {
                    break
                }

                if (zipEntry == null) {
                    break
                }

                val lastModified = if (isOreoPlus()) zipEntry.lastModifiedTime else 0
                val filename = zipEntry.fileName.removeSuffix("/")
                val listItem = ListItem(filename, filename.getFilenameFromPath(), zipEntry.isDirectory, 0, 0L, lastModified, false, false)
                runOnUiThread {
                    allFiles.add(listItem)
                    passwordDialog?.dismiss(notify = false)
                    passwordDialog = null
                    updateCurrentPath(currentPath)
                }
            }
            runOnUiThread {
                binding.progressBar.beGone()
                passwordDialog?.dismiss(notify = false)
                passwordDialog = null
            }
        }
    }

    private fun askForPassword() {
        passwordDialog = EnterPasswordDialog(
            this,
            callback = { newPassword ->
                password = newPassword
                setupFilesList()
            },
            cancelCallback = {
                finish()
            }
        )
    }
}
