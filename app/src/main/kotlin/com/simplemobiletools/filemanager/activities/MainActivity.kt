package com.simplemobiletools.filemanager.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.dialogs.StoragePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.LICENSE_MULTISELECT
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.views.Breadcrumbs
import com.simplemobiletools.filemanager.BuildConfig
import com.simplemobiletools.filemanager.PATH
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.SCROLL_STATE
import com.simplemobiletools.filemanager.dialogs.ChangeSortingDialog
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.fragments.ItemsFragment
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : SimpleActivity(), ItemsFragment.ItemInteractionListener, Breadcrumbs.BreadcrumbsListener {
    private val STORAGE_PERMISSION = 1
    private val BACK_PRESS_TIMEOUT = 5000

    private var latestFragment: ItemsFragment? = null
    private var scrollStates = HashMap<String, Parcelable>()
    private var storedTextColor = 0
    private var currentPath = ""
    private var wasBackJustPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        breadcrumbs.setListener(this)
        tryInitFileManager()
        storeStoragePaths()
        checkWhatsNewDialog()
        checkIfRootAvailable()
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(main_screen)
        if (storedTextColor != config.textColor) {
            storedTextColor = config.textColor
            breadcrumbs.setTextColor(storedTextColor)
            openPath(currentPath)
        }
        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        storedTextColor = config.textColor
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
    }

    private fun tryInitFileManager() {
        if (hasWriteStoragePermission()) {
            initRootFileManager()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    private fun initRootFileManager() {
        openPath(config.homeFolder)
    }

    private fun openPath(path: String) {
        val realPath = if (path.length > 1) path.trimEnd('/') else path
        breadcrumbs.setBreadcrumb(realPath)
        val bundle = Bundle()
        bundle.putString(PATH, realPath)

        if (scrollStates.containsKey(realPath)) {
            bundle.putParcelable(SCROLL_STATE, scrollStates[realPath])
        }

        if (latestFragment != null) {
            scrollStates.put(latestFragment!!.mPath.trimEnd('/'), latestFragment!!.getScrollState())
        }

        latestFragment = ItemsFragment().apply {
            arguments = bundle
            setListener(this@MainActivity)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, this).addToBackStack(realPath).commitAllowingStateLoss()
        }
        currentPath = realPath
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        val favorites = config.favorites
        menu.apply {
            findItem(R.id.add_favorite).isVisible = !favorites.contains(currentPath)
            findItem(R.id.remove_favorite).isVisible = favorites.contains(currentPath)
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
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun goHome() {
        if (config.homeFolder != currentPath)
            openPath(config.homeFolder)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, currentPath) {
            if (latestFragment != null) {
                latestFragment!!.fillItems()
            } else {
                openPath(currentPath)
            }
        }
    }

    private fun addFavorite() {
        config.addFavorite(currentPath)
        invalidateOptionsMenu()
    }

    private fun removeFavorite() {
        config.removeFavorite(currentPath)
        invalidateOptionsMenu()
    }

    private fun goToFavorite() {
        val favorites = config.favorites
        val items = ArrayList<RadioItem>(favorites.size)
        var currFavoriteIndex = -1

        favorites.forEachIndexed { index, path ->
            items.add(RadioItem(index, path, path))
            if (path == currentPath) {
                currFavoriteIndex = index
            }
        }

        RadioGroupDialog(this, items, currFavoriteIndex, R.string.go_to_favorite) {
            openPath(it.toString())
        }
    }

    private fun setAsHome() {
        config.homeFolder = currentPath
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
        openPath(currentPath)
        invalidateOptionsMenu()
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_MULTISELECT, BuildConfig.VERSION_NAME)
    }

    override fun onBackPressed() {
        if (breadcrumbs.childCount <= 1) {
            if (!wasBackJustPressed) {
                wasBackJustPressed = true
                toast(R.string.press_back_again)
                Handler().postDelayed({ wasBackJustPressed = false }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                finish()
            }
        } else {
            breadcrumbs.removeBreadcrumb()
            openPath(breadcrumbs.lastItem.path)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initRootFileManager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun itemClicked(item: FileDirItem) {
        openPath(item.path)
    }

    override fun breadcrumbClicked(id: Int) {
        if (id == 0) {
            StoragePickerDialog(this@MainActivity, currentPath) {
                openPath(it)
            }
        } else {
            val item = breadcrumbs.getChildAt(id).tag as FileDirItem
            openPath(item.path)
        }
    }

    private fun checkIfRootAvailable() {
        Thread({
            config.isRootAvailable = RootTools.isRootAvailable()
        }).start()
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(26, R.string.release_26))
            add(Release(28, R.string.release_28))
            add(Release(29, R.string.release_29))
            add(Release(34, R.string.release_34))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
