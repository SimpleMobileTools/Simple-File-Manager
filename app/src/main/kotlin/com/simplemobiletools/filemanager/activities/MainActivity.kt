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
import com.simplemobiletools.commons.dialogs.StoragePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.LICENSE_MULTISELECT
import com.simplemobiletools.commons.models.FileDirItem
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
    var mBasePath = getInternalStoragePath()
    var latestFragment: ItemsFragment? = null
    var mScrollStates = HashMap<String, Parcelable>()
    var mStoredTextColor = 0

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
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(main_screen)
        if (mStoredTextColor != config.textColor) {
            mStoredTextColor = config.textColor
            breadcrumbs.setTextColor(mStoredTextColor)
            initRootFileManager()
        }
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
            checkWhatsNewDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    private fun initRootFileManager() {
        openPath(mBasePath)
    }

    private fun openPath(path: String) {
        breadcrumbs.setBreadcrumb(path)
        val bundle = Bundle()
        bundle.putString(PATH, path)

        if (mScrollStates.containsKey(path.trimEnd('/'))) {
            bundle.putParcelable(SCROLL_STATE, mScrollStates[path.trimEnd('/')])
        }

        if (latestFragment != null) {
            mScrollStates.put(latestFragment!!.mPath.trimEnd('/'), latestFragment!!.getScrollState())
        }

        latestFragment = ItemsFragment().apply {
            arguments = bundle
            setListener(this@MainActivity)
            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, this).addToBackStack(path).commitAllowingStateLoss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.go_home -> goHome()
            R.id.set_as_home -> setAsHome()
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun goHome() {

    }

    private fun setAsHome() {

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
            StoragePickerDialog(this@MainActivity, mBasePath) {
                changePath(it)
            }
        } else {
            val item = breadcrumbs.getChildAt(id).tag as FileDirItem
            openPath(item.path)
        }
    }

    private fun changePath(pickedPath: String) {
        mBasePath = pickedPath
        openPath(pickedPath)
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(26, R.string.release_26))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
