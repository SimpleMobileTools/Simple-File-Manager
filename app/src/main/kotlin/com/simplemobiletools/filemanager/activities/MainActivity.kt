package com.simplemobiletools.filemanager.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.checkWhatsNew
import com.simplemobiletools.commons.extensions.handleHiddenFolderPasswordProtection
import com.simplemobiletools.commons.extensions.storeStoragePaths
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.LICENSE_MULTISELECT
import com.simplemobiletools.commons.helpers.LICENSE_PATTERN
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
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
import java.util.*

class MainActivity : SimpleActivity() {
    private val BACK_PRESS_TIMEOUT = 5000
    private var wasBackJustPressed = false

    private lateinit var fragment: ItemsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storeStoragePaths()

        fragment = fragment_holder as ItemsFragment
        tryInitFileManager()
        checkWhatsNewDialog()
        checkIfRootAvailable()
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        val favorites = config.favorites
        menu.apply {
            findItem(R.id.add_favorite).isVisible = !favorites.contains(fragment.currentPath)
            findItem(R.id.remove_favorite).isVisible = favorites.contains(fragment.currentPath)
            findItem(R.id.go_to_favorite).isVisible = favorites.isNotEmpty()

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden
        }

        return true
    }

    private fun tryInitFileManager() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initRootFileManager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun initRootFileManager() {
        openPath(config.homeFolder)
    }

    private fun openPath(path: String) {
        (fragment_holder as ItemsFragment).openPath(path)
        invalidateOptionsMenu()
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
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
        invalidateOptionsMenu()
    }

    private fun removeFavorite() {
        config.removeFavorite(fragment.currentPath)
        invalidateOptionsMenu()
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
        invalidateOptionsMenu()
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_MULTISELECT or LICENSE_PATTERN, BuildConfig.VERSION_NAME)
    }

    override fun onBackPressed() {
        if (fragment.breadcrumbs.childCount <= 1) {
            if (!wasBackJustPressed) {
                wasBackJustPressed = true
                toast(R.string.press_back_again)
                Handler().postDelayed({ wasBackJustPressed = false }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                finish()
            }
        } else {
            fragment.breadcrumbs.removeBreadcrumb()
            openPath(fragment.breadcrumbs.getLastItem().path)
        }
    }

    private fun checkIfRootAvailable() {
        Thread({
            config.isRootAvailable = RootTools.isRootAvailable()
            if (config.isRootAvailable && config.enableRootAccess) {
                RootHelpers().askRootIFNeeded(this) {
                    config.enableRootAccess = it
                }
            }
        }).start()
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(26, R.string.release_26))
            add(Release(28, R.string.release_28))
            add(Release(29, R.string.release_29))
            add(Release(34, R.string.release_34))
            add(Release(35, R.string.release_35))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
