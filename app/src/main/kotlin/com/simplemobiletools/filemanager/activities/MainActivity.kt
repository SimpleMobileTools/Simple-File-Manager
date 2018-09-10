package com.simplemobiletools.filemanager.activities

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.filemanager.BuildConfig
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.dialogs.ChangeSortingDialog
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.fragments.ItemsFragment
import com.simplemobiletools.filemanager.helpers.RootHelpers
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.items_fragment.*
import java.io.File
import java.util.*

class MainActivity : SimpleActivity() {
    private var isSearchOpen = false
    private val BACK_PRESS_TIMEOUT = 5000
    private val PICKED_PATH = "picked_path"
    private var wasBackJustPressed = false
    private var searchMenuItem: MenuItem? = null

    private lateinit var fragment: ItemsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        fragment = (fragment_holder as ItemsFragment).apply {
            isGetRingtonePicker = intent.action == RingtoneManager.ACTION_RINGTONE_PICKER
            isGetContentIntent = intent.action == Intent.ACTION_GET_CONTENT
            isPickMultipleIntent = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        if (savedInstanceState == null) {
            tryInitFileManager()
            checkWhatsNewDialog()
            checkIfRootAvailable()
            checkInvalidFavorites()
        }
    }

    override fun onStop() {
        super.onStop()
        searchMenuItem?.collapseActionView()
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        setupSearch(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val favorites = config.favorites
        menu!!.apply {
            findItem(R.id.add_favorite).isVisible = !favorites.contains(fragment.currentPath)
            findItem(R.id.remove_favorite).isVisible = favorites.contains(fragment.currentPath)
            findItem(R.id.go_to_favorite).isVisible = favorites.isNotEmpty()

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.go_home -> goHome()
            R.id.go_to_favorite -> goToFavorite()
            R.id.sort -> showSortingDialog()
            R.id.add_favorite -> addFavorite()
            R.id.remove_favorite -> removeFavorite()
            R.id.set_as_home -> setAsHome()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PICKED_PATH, (fragment_holder as ItemsFragment).currentPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        openPath(savedInstanceState.getString(PICKED_PATH), true)
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(R.string.search_folder)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        fragment.searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearchOpen = true
                fragment.searchOpened()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchOpen = false
                fragment.searchClosed()
                return true
            }
        })
    }

    private fun tryInitFileManager() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initFileManager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun initFileManager() {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val data = intent.data
            if (data.scheme == "file") {
                openPath(data.path)
            } else {
                val path = getRealPathFromURI(data)
                if (path != null) {
                    openPath(path)
                } else {
                    openPath(config.homeFolder)
                }
            }
        } else {
            openPath(config.homeFolder)
        }
    }

    private fun openPath(path: String, forceRefresh: Boolean = false) {
        var newPath = path
        val file = File(path)
        if (file.exists() && !file.isDirectory) {
            newPath = file.parent
        } else if (!file.exists() && !newPath.startsWith(OTG_PATH)) {
            newPath = internalStoragePath
        }

        (fragment_holder as ItemsFragment).openPath(newPath, forceRefresh)
    }

    private fun goHome() {
        if (config.homeFolder != fragment.currentPath)
            openPath(config.homeFolder)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, fragment.currentPath) {
            fragment.refreshItems()
        }
    }

    private fun addFavorite() {
        config.addFavorite(fragment.currentPath)
    }

    private fun removeFavorite() {
        config.removeFavorite(fragment.currentPath)
    }

    private fun goToFavorite() {
        val favorites = config.favorites
        val items = ArrayList<RadioItem>(favorites.size)
        var currFavoriteIndex = -1

        favorites.forEachIndexed { index, path ->
            items.add(RadioItem(index, path, path))
            if (path == fragment.currentPath) {
                currFavoriteIndex = index
            }
        }

        RadioGroupDialog(this, items, currFavoriteIndex, R.string.go_to_favorite) {
            openPath(it.toString())
        }
    }

    private fun setAsHome() {
        config.homeFolder = fragment.currentPath
        toast(R.string.home_folder_updated)
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
        } else {
            handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        config.temporarilyShowHidden = show
        openPath(fragment.currentPath)
    }

    private fun launchAbout() {
        val licenses = LICENSE_MULTISELECT or LICENSE_GLIDE or LICENSE_PATTERN or LICENSE_REPRINT

        val faqItems = arrayListOf(
                FAQItem(R.string.faq_3_title_commons, R.string.faq_3_text_commons),
                FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons)
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    override fun onBackPressed() {
        if (fragment.breadcrumbs.childCount <= 1) {
            if (!wasBackJustPressed) {
                wasBackJustPressed = true
                toast(R.string.press_back_again)
                Handler().postDelayed({
                    wasBackJustPressed = false
                }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                finish()
            }
        } else {
            fragment.breadcrumbs.removeBreadcrumb()
            openPath(fragment.breadcrumbs.getLastItem().path)
        }
    }

    private fun checkIfRootAvailable() {
        Thread {
            config.isRootAvailable = RootTools.isRootAvailable()
            if (config.isRootAvailable && config.enableRootAccess) {
                RootHelpers(this).askRootIfNeeded {
                    config.enableRootAccess = it
                }
            }
        }.start()
    }

    private fun checkInvalidFavorites() {
        Thread {
            config.favorites.forEach {
                if (!it.startsWith(OTG_PATH) && !isPathOnSD(it) && !getDoesFilePathExist(it)) {
                    config.removeFavorite(it)
                }
            }
        }.start()
    }

    fun pickedPath(path: String) {
        val resultIntent = Intent()
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        resultIntent.setDataAndType(uri, type)
        resultIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    fun pickedRingtone(path: String) {
        val resultIntent = Intent()
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        resultIntent.setDataAndType(uri, type)
        resultIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    fun pickedPaths(paths: ArrayList<String>) {
        val newPaths = paths.map { getFilePublicUri(File(it), BuildConfig.APPLICATION_ID).toString() } as ArrayList
        val clipData = ClipData("Attachment", arrayOf(newPaths.getMimeType()), ClipData.Item(newPaths.removeAt(0)))

        newPaths.forEach {
            clipData.addItem(ClipData.Item(it))
        }

        val resultIntent = Intent()
        resultIntent.clipData = clipData
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    fun openedDirectory() {
        if (searchMenuItem != null) {
            MenuItemCompat.collapseActionView(searchMenuItem)
        }
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(26, R.string.release_26))
            add(Release(28, R.string.release_28))
            add(Release(29, R.string.release_29))
            add(Release(34, R.string.release_34))
            add(Release(35, R.string.release_35))
            add(Release(37, R.string.release_37))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
