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
import com.simplemobiletools.filemanager.extensions.config
import com.simplemobiletools.filemanager.fragments.ItemsFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : SimpleActivity(), ItemsFragment.ItemInteractionListener, Breadcrumbs.BreadcrumbsListener {
    var latestFragment: ItemsFragment? = null
    var mScrollStates = HashMap<String, Parcelable>()
    var mStoredTextColor = 0
    var currentPath = ""

    companion object {
        private val STORAGE_PERMISSION = 1
        private val BACK_PRESS_TIMEOUT = 5000

        private var mWasBackJustPressed: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        breadcrumbs.setListener(this)
        tryInitFileManager()
        storeStoragePaths()
        checkWhatsNewDialog()
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(main_screen)
        if (mStoredTextColor != config.textColor) {
            mStoredTextColor = config.textColor
            breadcrumbs.setTextColor(mStoredTextColor)
            openPath(currentPath)
        }
        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        mStoredTextColor = config.textColor
    }

    override fun onDestroy() {
        super.onDestroy()
        config.isFirstRun = false
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
        val realPath = path.trimEnd('/')
        breadcrumbs.setBreadcrumb(realPath)
        val bundle = Bundle()
        bundle.putString(PATH, realPath)

        if (mScrollStates.containsKey(realPath)) {
            bundle.putParcelable(SCROLL_STATE, mScrollStates[realPath])
        }

        if (latestFragment != null) {
            mScrollStates.put(latestFragment!!.mPath.trimEnd('/'), latestFragment!!.getScrollState())
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
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.go_home -> goHome()
            R.id.add_favorite -> addFavorite()
            R.id.remove_favorite -> removeFavorite()
            R.id.go_to_favorite -> goToFavorite()
            R.id.set_as_home -> setAsHome()
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun goHome() {
        openPath(config.homeFolder)
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

        RadioGroupDialog(this, items, currFavoriteIndex) {
            openPath(it.toString())
        }
    }

    private fun setAsHome() {
        config.homeFolder = currentPath
        toast(R.string.home_folder_updated)
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_MULTISELECT, BuildConfig.VERSION_NAME)
    }

    override fun onBackPressed() {
        if (breadcrumbs.childCount <= 1) {
            if (!mWasBackJustPressed) {
                mWasBackJustPressed = true
                toast(R.string.press_back_again)
                Handler().postDelayed({ mWasBackJustPressed = false }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                finish()
            }
        } else {
            breadcrumbs.removeBreadcrumb()
            val item = breadcrumbs.lastItem
            openPath(item.path)
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

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(26, R.string.release_26))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
