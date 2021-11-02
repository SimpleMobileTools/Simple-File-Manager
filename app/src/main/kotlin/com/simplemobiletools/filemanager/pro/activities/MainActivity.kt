package com.simplemobiletools.filemanager.pro.activities

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.filemanager.pro.BuildConfig
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.adapters.ViewPagerAdapter
import com.simplemobiletools.filemanager.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.filemanager.pro.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.tryOpenPathIntent
import com.simplemobiletools.filemanager.pro.fragments.ItemsFragment
import com.simplemobiletools.filemanager.pro.fragments.MyViewPagerFragment
import com.simplemobiletools.filemanager.pro.helpers.MAX_COLUMN_COUNT
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import com.simplemobiletools.filemanager.pro.helpers.tabsList
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.items_fragment.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import kotlinx.android.synthetic.main.recents_fragment.*
import java.io.File
import java.lang.Exception
import java.util.*

class MainActivity : SimpleActivity() {
    private val BACK_PRESS_TIMEOUT = 5000
    private val MANAGE_STORAGE_RC = 201
    private val PICKED_PATH = "picked_path"
    private var isSearchOpen = false
    private var wasBackJustPressed = false
    private var mIsPasswordProtectionPending = false
    private var mWasProtectionHandled = false
    private var searchMenuItem: MenuItem? = null

    private var storedFontSize = 0
    private var storedDateFormat = ""
    private var storedTimeFormat = ""
    private var storedShowTabs = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupTabColors(config.lastUsedViewPagerPage)
        storeStateVariables()
        mIsPasswordProtectionPending = config.isAppPasswordProtectionOn

        if (savedInstanceState == null) {
            handleAppPasswordProtection {
                mWasProtectionHandled = it
                if (it) {
                    initFragments()
                    mIsPasswordProtectionPending = false
                    tryInitFileManager()
                    checkWhatsNewDialog()
                    checkIfRootAvailable()
                    checkInvalidFavorites()
                } else {
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (storedShowTabs != config.showTabs) {
            config.lastUsedViewPagerPage = 0
            System.exit(0)
            return
        }

        getAllFragments().forEach {
            it?.setupColors(config.textColor, config.primaryColor)
        }

        if (storedFontSize != config.fontSize) {
            getAllFragments().forEach {
                it?.setupFontSize()
            }
        }

        if (storedDateFormat != config.dateFormat || storedTimeFormat != getTimeFormat()) {
            getAllFragments().forEach {
                it?.setupDateTimeFormat()
            }
        }

        getInactiveTabIndexes(main_view_pager.currentItem).forEach {
            main_tabs_holder.getTabAt(it)?.icon?.applyColorFilter(config.textColor)
        }

        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        main_tabs_holder.background = ColorDrawable(config.backgroundColor)
        main_tabs_holder.setSelectedTabIndicatorColor(adjustedPrimaryColor)
        main_tabs_holder.getTabAt(main_view_pager.currentItem)?.icon?.applyColorFilter(adjustedPrimaryColor)

        if (main_view_pager.adapter == null && mWasProtectionHandled) {
            initFragments()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        config.lastUsedViewPagerPage = main_view_pager.currentItem
        config.temporarilyShowHidden = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        setupSearch(menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val currentFragment = getCurrentFragment() ?: return true
        val favorites = config.favorites

        menu!!.apply {
            findItem(R.id.search).isVisible = currentFragment is ItemsFragment
            findItem(R.id.sort).isVisible = currentFragment is ItemsFragment

            findItem(R.id.add_favorite).isVisible = currentFragment is ItemsFragment && !favorites.contains(currentFragment.currentPath)
            findItem(R.id.remove_favorite).isVisible = currentFragment is ItemsFragment && favorites.contains(currentFragment.currentPath)
            findItem(R.id.go_to_favorite).isVisible = currentFragment is ItemsFragment && favorites.isNotEmpty()

            findItem(R.id.toggle_filename).isVisible = config.getFolderViewType(currentFragment.currentPath) == VIEW_TYPE_GRID
            findItem(R.id.go_home).isVisible = currentFragment is ItemsFragment && currentFragment.currentPath != config.homeFolder
            findItem(R.id.set_as_home).isVisible = currentFragment is ItemsFragment && currentFragment.currentPath != config.homeFolder

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.increase_column_count).isVisible =
                config.getFolderViewType(currentFragment.currentPath) == VIEW_TYPE_GRID && config.fileColumnCnt < MAX_COLUMN_COUNT
            findItem(R.id.reduce_column_count).isVisible = config.getFolderViewType(currentFragment.currentPath) == VIEW_TYPE_GRID && config.fileColumnCnt > 1
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getCurrentFragment() == null) {
            return true
        }

        when (item.itemId) {
            R.id.go_home -> goHome()
            R.id.go_to_favorite -> goToFavorite()
            R.id.sort -> showSortingDialog()
            R.id.add_favorite -> addFavorite()
            R.id.remove_favorite -> removeFavorite()
            R.id.toggle_filename -> toggleFilenameVisibility()
            R.id.set_as_home -> setAsHome()
            R.id.change_view_type -> changeViewType()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PICKED_PATH, items_fragment?.currentPath ?: "")
        outState.putBoolean(WAS_PROTECTION_HANDLED, mWasProtectionHandled)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWasProtectionHandled = savedInstanceState.getBoolean(WAS_PROTECTION_HANDLED, false)
        val path = savedInstanceState.getString(PICKED_PATH) ?: internalStoragePath

        if (main_view_pager.adapter == null) {
            main_view_pager.onGlobalLayout {
                restorePath(path)
            }
            updateTabColors()
        } else {
            restorePath(path)
        }
    }

    private fun restorePath(path: String) {
        if (!mWasProtectionHandled) {
            handleAppPasswordProtection {
                mWasProtectionHandled = it
                if (it) {
                    mIsPasswordProtectionPending = false
                    openPath(path, true)
                } else {
                    finish()
                }
            }
        } else {
            openPath(path, true)
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(R.string.search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        getCurrentFragment()?.searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                isSearchOpen = true
                (getCurrentFragment() as? ItemsFragment)?.searchOpened()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchOpen = false
                (getCurrentFragment() as? ItemsFragment)?.searchClosed()
                return true
            }
        })
    }

    private fun storeStateVariables() {
        config.apply {
            storedFontSize = fontSize
            storedDateFormat = dateFormat
            storedTimeFormat = context.getTimeFormat()
            storedShowTabs = showTabs
        }
    }

    private fun tryInitFileManager() {
        val hadPermission = hasStoragePermission()
        handleStoragePermission {
            checkOTGPath()
            if (it) {
                if (main_view_pager.adapter == null) {
                    initFragments()
                }

                main_view_pager.onGlobalLayout {
                    initFileManager(!hadPermission)
                }
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun handleStoragePermission(callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasStoragePermission()) {
            callback(true)
        } else {
            if (isRPlus()) {
                isAskingPermissions = true
                actionOnPermission = callback
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, MANAGE_STORAGE_RC)
                } catch (e: Exception) {
                    e.printStackTrace()
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivityForResult(intent, MANAGE_STORAGE_RC)
                }
            } else {
                handlePermission(PERMISSION_WRITE_STORAGE, callback)
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (isRPlus()) Environment.isExternalStorageManager() else hasPermission(PERMISSION_WRITE_STORAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        isAskingPermissions = false
        if (requestCode == MANAGE_STORAGE_RC && isRPlus()) {
            actionOnPermission?.invoke(Environment.isExternalStorageManager())
        }
    }

    private fun initFileManager(refreshRecents: Boolean) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val data = intent.data
            if (data?.scheme == "file") {
                openPath(data.path!!)
            } else {
                val path = getRealPathFromURI(data!!)
                if (path != null) {
                    openPath(path)
                } else {
                    openPath(config.homeFolder)
                }
            }

            if (!File(data.path!!).isDirectory) {
                tryOpenPathIntent(data.path!!, false)
            }
        } else {
            openPath(config.homeFolder)
        }

        getAllFragments().forEach {
            it?.isGetRingtonePicker = intent.action == RingtoneManager.ACTION_RINGTONE_PICKER
            it?.isGetContentIntent = intent.action == Intent.ACTION_GET_CONTENT
            it?.isPickMultipleIntent = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        if (refreshRecents) {
            recents_fragment?.refreshItems()
        }
    }

    private fun initFragments() {
        main_view_pager.adapter = ViewPagerAdapter(this)
        main_view_pager.currentItem = config.lastUsedViewPagerPage
        main_view_pager.onPageChangeListener {
            main_tabs_holder.getTabAt(it)?.select()
            invalidateOptionsMenu()
        }

        val tabToOpen = config.lastUsedViewPagerPage
        main_view_pager.currentItem = tabToOpen
        main_tabs_holder.onTabSelectionChanged(
            tabUnselectedAction = {
                it.icon?.applyColorFilter(config.textColor)
            },
            tabSelectedAction = {
                main_view_pager.currentItem = it.position
                it.icon?.applyColorFilter(getAdjustedPrimaryColor())
            }
        )

        main_view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (isSearchOpen) {
                    getCurrentFragment()?.searchQueryChanged("")
                    searchMenuItem?.collapseActionView()
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                main_tabs_holder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
                invalidateOptionsMenu()
            }
        })

        main_tabs_holder.removeAllTabs()
        var skippedTabs = 0
        tabsList.forEachIndexed { index, value ->
            if (config.showTabs and value == 0) {
                skippedTabs++
            } else {
                val tab = main_tabs_holder.newTab().setIcon(getTabIcon(index))
                main_tabs_holder.addTab(tab, index - skippedTabs, config.lastUsedViewPagerPage == index - skippedTabs)
            }
        }

        // selecting the proper tab sometimes glitches, add an extra selector to make sure we have it right
        main_tabs_holder.onGlobalLayout {
            Handler().postDelayed({
                main_tabs_holder.getTabAt(config.lastUsedViewPagerPage)?.select()
                invalidateOptionsMenu()
            }, 100L)
        }

        main_tabs_holder.beVisibleIf(skippedTabs < tabsList.size - 1)
    }

    private fun setupTabColors(lastUsedTab: Int) {
        main_tabs_holder.apply {
            background = ColorDrawable(config.backgroundColor)
            setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            getTabAt(lastUsedTab)?.apply {
                select()
                icon?.applyColorFilter(getAdjustedPrimaryColor())
            }

            getInactiveTabIndexes(lastUsedTab).forEach {
                getTabAt(it)?.icon?.applyColorFilter(config.textColor)
            }
        }
    }

    private fun updateTabColors() {
        getInactiveTabIndexes(main_view_pager.currentItem).forEach {
            main_tabs_holder.getTabAt(it)?.icon?.applyColorFilter(config.textColor)
        }
        main_tabs_holder.getTabAt(main_view_pager.currentItem)?.icon?.applyColorFilter(getAdjustedPrimaryColor())
    }

    private fun getTabIcon(position: Int): Drawable {
        val drawableId = when (position) {
            0 -> R.drawable.ic_folder_vector
            else -> R.drawable.ic_clock_vector
        }

        return resources.getColoredDrawableWithColor(drawableId, config.textColor)
    }

    private fun checkOTGPath() {
        ensureBackgroundThread {
            if (!config.wasOTGHandled && hasPermission(PERMISSION_WRITE_STORAGE) && hasOTGConnected() && config.OTGPath.isEmpty()) {
                getStorageDirectories().firstOrNull { it.trimEnd('/') != internalStoragePath && it.trimEnd('/') != sdCardPath }?.apply {
                    config.wasOTGHandled = true
                    config.OTGPath = trimEnd('/')
                }
            }
        }
    }

    private fun openPath(path: String, forceRefresh: Boolean = false) {
        if (mIsPasswordProtectionPending && !mWasProtectionHandled) {
            return
        }

        var newPath = path
        val file = File(path)
        if (config.OTGPath.isNotEmpty() && config.OTGPath == path.trimEnd('/')) {
            newPath = path
        } else if (file.exists() && !file.isDirectory) {
            newPath = file.parent
        } else if (!file.exists() && !isPathOnOTG(newPath)) {
            newPath = internalStoragePath
        }

        items_fragment?.openPath(newPath, forceRefresh)
    }

    private fun goHome() {
        if (config.homeFolder != getCurrentFragment()!!.currentPath) {
            openPath(config.homeFolder)
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, getCurrentFragment()!!.currentPath) {
            (getCurrentFragment() as? ItemsFragment)?.refreshItems()
        }
    }

    private fun addFavorite() {
        config.addFavorite(getCurrentFragment()!!.currentPath)
    }

    private fun removeFavorite() {
        config.removeFavorite(getCurrentFragment()!!.currentPath)
    }

    private fun toggleFilenameVisibility() {
        config.displayFilenames = !config.displayFilenames
        getAllFragments().forEach {
            it?.toggleFilenameVisibility()
        }
    }

    private fun increaseColumnCount() {
        getAllFragments().forEach {
            it?.increaseColumnCount()
        }
    }

    private fun reduceColumnCount() {
        getAllFragments().forEach {
            it?.reduceColumnCount()
        }
    }

    private fun goToFavorite() {
        val favorites = config.favorites
        val items = ArrayList<RadioItem>(favorites.size)
        var currFavoriteIndex = -1

        favorites.forEachIndexed { index, path ->
            val visiblePath = humanizePath(path).replace("/", " / ")
            items.add(RadioItem(index, visiblePath, path))
            if (path == getCurrentFragment()!!.currentPath) {
                currFavoriteIndex = index
            }
        }

        RadioGroupDialog(this, items, currFavoriteIndex, R.string.go_to_favorite) {
            openPath(it.toString())
        }
    }

    private fun setAsHome() {
        config.homeFolder = getCurrentFragment()!!.currentPath
        toast(R.string.home_folder_updated)
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(this, getCurrentFragment()!!.currentPath, getCurrentFragment() is ItemsFragment) {
            getAllFragments().forEach {
                it?.refreshItems()
            }
        }
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
        getAllFragments().forEach {
            it?.refreshItems()
        }
    }

    private fun launchAbout() {
        val licenses = LICENSE_GLIDE or LICENSE_PATTERN or LICENSE_REPRINT or LICENSE_GESTURE_VIEWS

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_3_title_commons, R.string.faq_3_text_commons),
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
            FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons),
            FAQItem(R.string.faq_7_title_commons, R.string.faq_7_text_commons),
            FAQItem(R.string.faq_9_title_commons, R.string.faq_9_text_commons),
            FAQItem(R.string.faq_10_title_commons, R.string.faq_10_text_commons)
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    override fun onBackPressed() {
        if (getCurrentFragment() !is ItemsFragment) {
            super.onBackPressed()
            return
        }

        if (getCurrentFragment()!!.breadcrumbs.itemsCount <= 1) {
            if (!wasBackJustPressed && config.pressBackTwice) {
                wasBackJustPressed = true
                toast(R.string.press_back_again)
                Handler().postDelayed({
                    wasBackJustPressed = false
                }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                finish()
            }
        } else {
            getCurrentFragment()!!.breadcrumbs.removeBreadcrumb()
            openPath(getCurrentFragment()!!.breadcrumbs.getLastItem().path)
        }
    }

    private fun checkIfRootAvailable() {
        ensureBackgroundThread {
            config.isRootAvailable = RootTools.isRootAvailable()
            if (config.isRootAvailable && config.enableRootAccess) {
                RootHelpers(this).askRootIfNeeded {
                    config.enableRootAccess = it
                }
            }
        }
    }

    private fun checkInvalidFavorites() {
        ensureBackgroundThread {
            config.favorites.forEach {
                if (!isPathOnOTG(it) && !isPathOnSD(it) && !File(it).exists()) {
                    config.removeFavorite(it)
                }
            }
        }
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
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        Intent().apply {
            setDataAndType(uri, type)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    fun pickedPaths(paths: ArrayList<String>) {
        val newPaths = paths.map { getFilePublicUri(File(it), BuildConfig.APPLICATION_ID) } as ArrayList
        val clipData = ClipData("Attachment", arrayOf(paths.getMimeType()), ClipData.Item(newPaths.removeAt(0)))

        newPaths.forEach {
            clipData.addItem(ClipData.Item(it))
        }

        Intent().apply {
            this.clipData = clipData
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    fun openedDirectory() {
        if (searchMenuItem != null) {
            MenuItemCompat.collapseActionView(searchMenuItem)
        }
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until tabsList.size).filter { it != activeIndex }

    private fun getAllFragments(): ArrayList<MyViewPagerFragment?> = arrayListOf(items_fragment, recents_fragment)

    private fun getCurrentFragment(): MyViewPagerFragment? {
        val showTabs = config.showTabs
        val fragments = arrayListOf<MyViewPagerFragment>()
        if (showTabs and TAB_FILES != 0) {
            fragments.add(items_fragment)
        }

        if (showTabs and TAB_RECENT_FILES != 0) {
            fragments.add(recents_fragment)
        }

        return fragments.getOrNull(main_view_pager.currentItem)
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(26, R.string.release_26))
            add(Release(28, R.string.release_28))
            add(Release(29, R.string.release_29))
            add(Release(34, R.string.release_34))
            add(Release(35, R.string.release_35))
            add(Release(37, R.string.release_37))
            add(Release(71, R.string.release_71))
            add(Release(75, R.string.release_75))
            add(Release(96, R.string.release_96))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
