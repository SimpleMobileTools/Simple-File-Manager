package com.simplemobiletools.filemanager.pro.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
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
import com.simplemobiletools.filemanager.pro.fragments.StorageFragment
import com.simplemobiletools.filemanager.pro.helpers.MAX_COLUMN_COUNT
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import com.simplemobiletools.filemanager.pro.interfaces.ItemOperationsListener
import com.stericson.RootTools.RootTools
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.items_fragment.*
import kotlinx.android.synthetic.main.items_fragment.view.*
import kotlinx.android.synthetic.main.recents_fragment.*
import kotlinx.android.synthetic.main.storage_fragment.*
import me.grantland.widget.AutofitHelper
import java.io.File

class MainActivity : SimpleActivity() {
    private val BACK_PRESS_TIMEOUT = 5000
    private val MANAGE_STORAGE_RC = 201
    private val PICKED_PATH = "picked_path"
    private var isSearchOpen = false
    private var wasBackJustPressed = false
    private var mIsPasswordProtectionPending = false
    private var mWasProtectionHandled = false
    private var mTabsToShow = ArrayList<Int>()
    private var searchMenuItem: MenuItem? = null

    private var storedFontSize = 0
    private var storedDateFormat = ""
    private var storedTimeFormat = ""
    private var storedShowTabs = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        mTabsToShow = getTabsList()

        if (!config.wasStorageAnalysisTabAdded && isOreoPlus()) {
            config.wasStorageAnalysisTabAdded = true
            if (config.showTabs and TAB_STORAGE_ANALYSIS == 0) {
                config.showTabs += TAB_STORAGE_ANALYSIS
            }
        }

        storeStateVariables()
        setupTabs()
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

        setupTabColors()
        setupToolbar(main_toolbar, searchMenuItem = searchMenuItem)

        getAllFragments().forEach {
            it?.onResume(getProperTextColor())
        }

        if (storedFontSize != config.fontSize) {
            getAllFragments().forEach {
                (it as? ItemOperationsListener)?.setupFontSize()
            }
        }

        if (storedDateFormat != config.dateFormat || storedTimeFormat != getTimeFormat()) {
            getAllFragments().forEach {
                (it as? ItemOperationsListener)?.setupDateTimeFormat()
            }
        }

        if (main_view_pager.adapter == null && mWasProtectionHandled) {
            initFragments()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        config.lastUsedViewPagerPage = main_view_pager.currentItem
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
    }

    private fun refreshMenuItems() {
        val currentFragment = getCurrentFragment() ?: return
        val currentViewType = config.getFolderViewType(currentFragment.currentPath)
        val favorites = config.favorites

        main_toolbar.menu.apply {
            findItem(R.id.search).isVisible = currentFragment is ItemsFragment
            findItem(R.id.sort).isVisible = currentFragment is ItemsFragment
            findItem(R.id.change_view_type).isVisible = currentFragment !is StorageFragment

            findItem(R.id.add_favorite).isVisible = currentFragment is ItemsFragment && !favorites.contains(currentFragment.currentPath)
            findItem(R.id.remove_favorite).isVisible = currentFragment is ItemsFragment && favorites.contains(currentFragment.currentPath)
            findItem(R.id.go_to_favorite).isVisible = currentFragment is ItemsFragment && favorites.isNotEmpty()

            findItem(R.id.toggle_filename).isVisible = currentViewType == VIEW_TYPE_GRID && currentFragment !is StorageFragment
            findItem(R.id.go_home).isVisible = currentFragment is ItemsFragment && currentFragment.currentPath != config.homeFolder
            findItem(R.id.set_as_home).isVisible = currentFragment is ItemsFragment && currentFragment.currentPath != config.homeFolder

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden && currentFragment !is StorageFragment
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden && currentFragment !is StorageFragment

            findItem(R.id.increase_column_count).isVisible =
                currentViewType == VIEW_TYPE_GRID && config.fileColumnCnt < MAX_COLUMN_COUNT && currentFragment !is StorageFragment
            findItem(R.id.reduce_column_count).isVisible = currentViewType == VIEW_TYPE_GRID && config.fileColumnCnt > 1 && currentFragment !is StorageFragment
        }
    }

    private fun setupOptionsMenu() {
        setupSearch(main_toolbar.menu)
        main_toolbar.setOnMenuItemClickListener { menuItem ->
            if (getCurrentFragment() == null) {
                return@setOnMenuItemClickListener true
            }

            when (menuItem.itemId) {
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
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
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
        } else {
            restorePath(path)
        }
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        isAskingPermissions = false
        if (requestCode == MANAGE_STORAGE_RC && isRPlus()) {
            actionOnPermission?.invoke(Environment.isExternalStorageManager())
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
                        (getCurrentFragment() as? ItemOperationsListener)?.searchQueryChanged(newText)
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

    @SuppressLint("InlinedApi")
    private fun handleStoragePermission(callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasStoragePermission()) {
            callback(true)
        } else {
            if (isRPlus()) {
                ConfirmationAdvancedDialog(this, "", R.string.access_storage_prompt, R.string.ok, 0, false) { success ->
                    if (success) {
                        isAskingPermissions = true
                        actionOnPermission = callback
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.addCategory("android.intent.category.DEFAULT")
                            intent.data = Uri.parse("package:$packageName")
                            startActivityForResult(intent, MANAGE_STORAGE_RC)
                        } catch (e: Exception) {
                            showErrorToast(e)
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            startActivityForResult(intent, MANAGE_STORAGE_RC)
                        }
                    } else {
                        finish()
                    }
                }
            } else {
                handlePermission(PERMISSION_WRITE_STORAGE, callback)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun hasStoragePermission(): Boolean {
        return if (isRPlus()) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(PERMISSION_WRITE_STORAGE)
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
                tryOpenPathIntent(data.path!!, false, finishActivity = true)
            }
        } else {
            openPath(config.homeFolder)
        }

        val isPickRingtoneIntent = intent.action == RingtoneManager.ACTION_RINGTONE_PICKER
        val isGetContentIntent = intent.action == Intent.ACTION_GET_CONTENT || intent.action == Intent.ACTION_PICK
        val allowPickingMultipleIntent = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        val getContentMimeType = if (isGetContentIntent) {
            intent.type ?: ""
        } else {
            ""
        }

        getAllFragments().forEach {
            it?.isGetRingtonePicker = isPickRingtoneIntent
            it?.isPickMultipleIntent = allowPickingMultipleIntent
            it?.isGetContentIntent = isGetContentIntent
            it?.wantedMimeType = getContentMimeType
        }

        if (refreshRecents) {
            recents_fragment?.refreshFragment()
        }
    }

    private fun initFragments() {
        main_view_pager.adapter = ViewPagerAdapter(this, mTabsToShow)
        main_view_pager.offscreenPageLimit = 2

        main_view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                main_tabs_holder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    (it as? ItemOperationsListener)?.finishActMode()
                }
                refreshMenuItems()
            }
        })
        main_view_pager.currentItem = config.lastUsedViewPagerPage

        main_view_pager.onGlobalLayout {
            refreshMenuItems()
        }
    }

    private fun setupTabs() {
        main_tabs_holder.removeAllTabs()
        val isPickFileIntent =
            intent.action == RingtoneManager.ACTION_RINGTONE_PICKER || intent.action == Intent.ACTION_GET_CONTENT || intent.action == Intent.ACTION_PICK
        if (isPickFileIntent) {
            mTabsToShow.remove(TAB_STORAGE_ANALYSIS)
            if (mTabsToShow.none { it and config.showTabs != 0 }) {
                config.showTabs = TAB_FILES
                storedShowTabs = TAB_FILES
                mTabsToShow = arrayListOf(TAB_FILES)
            }
        }

        mTabsToShow.forEachIndexed { index, value ->
            if (config.showTabs and value != 0) {
                main_tabs_holder.newTab().setCustomView(R.layout.bottom_tablayout_item).apply {
                    customView?.findViewById<ImageView>(R.id.tab_item_icon)?.setImageDrawable(getTabIcon(index))
                    customView?.findViewById<TextView>(R.id.tab_item_label)?.text = getTabLabel(index)
                    AutofitHelper.create(customView?.findViewById(R.id.tab_item_label))
                    main_tabs_holder.addTab(this)
                }
            }
        }

        main_tabs_holder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false)
            },
            tabSelectedAction = {
                closeSearch()
                main_view_pager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true)
            }
        )

        main_tabs_holder.beGoneIf(main_tabs_holder.tabCount == 1)
    }

    private fun setupTabColors() {
        val activeView = main_tabs_holder.getTabAt(main_view_pager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true)

        getInactiveTabIndexes(main_view_pager.currentItem).forEach { index ->
            val inactiveView = main_tabs_holder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false)
        }

        val bottomBarColor = getBottomNavigationBackgroundColor()
        main_tabs_holder.setBackgroundColor(bottomBarColor)
        updateNavigationBarColor(bottomBarColor)
    }

    private fun getTabIcon(position: Int): Drawable {
        val drawableId = when (position) {
            0 -> R.drawable.ic_folder_vector
            1 -> R.drawable.ic_clock_vector
            else -> R.drawable.ic_storage_vector
        }

        return resources.getColoredDrawableWithColor(drawableId, getProperTextColor())
    }

    private fun getTabLabel(position: Int): String {
        val stringId = when (position) {
            0 -> R.string.files_tab
            1 -> R.string.recents
            else -> R.string.storage
        }

        return resources.getString(stringId)
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

    private fun closeSearch() {
        if (isSearchOpen) {
            getAllFragments().forEach {
                (it as? ItemOperationsListener)?.searchQueryChanged("")
            }
            searchMenuItem?.collapseActionView()
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
            (getCurrentFragment() as? ItemsFragment)?.refreshFragment()
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
            (it as? ItemOperationsListener)?.toggleFilenameVisibility()
        }
    }

    private fun increaseColumnCount() {
        getAllFragments().forEach {
            (it as? ItemOperationsListener)?.increaseColumnCount()
        }
    }

    private fun reduceColumnCount() {
        getAllFragments().forEach {
            (it as? ItemOperationsListener)?.reduceColumnCount()
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
                it?.refreshFragment()
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
            it?.refreshFragment()
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        closeSearch()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        closeSearch()
        val licenses = LICENSE_GLIDE or LICENSE_PATTERN or LICENSE_REPRINT or LICENSE_GESTURE_VIEWS or LICENSE_PDF_VIEWER or LICENSE_AUTOFITTEXTVIEW

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_3_title_commons, R.string.faq_3_text_commons),
            FAQItem(R.string.faq_9_title_commons, R.string.faq_9_text_commons)
        )

        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))
            faqItems.add(FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons))
            faqItems.add(FAQItem(R.string.faq_7_title_commons, R.string.faq_7_text_commons))
            faqItems.add(FAQItem(R.string.faq_10_title_commons, R.string.faq_10_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    override fun onBackPressed() {
        if (getCurrentFragment() !is ItemsFragment) {
            super.onBackPressed()
            return
        }

        if (getCurrentFragment()!!.breadcrumbs.getItemCount() <= 1) {
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

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until getTabsList().size).filter { it != activeIndex }

    private fun getAllFragments(): ArrayList<MyViewPagerFragment?> = arrayListOf(items_fragment, recents_fragment, storage_fragment)

    private fun getCurrentFragment(): MyViewPagerFragment? {
        val showTabs = config.showTabs
        val fragments = arrayListOf<MyViewPagerFragment>()
        if (showTabs and TAB_FILES != 0) {
            fragments.add(items_fragment)
        }

        if (showTabs and TAB_RECENT_FILES != 0) {
            fragments.add(recents_fragment)
        }

        if (showTabs and TAB_STORAGE_ANALYSIS != 0) {
            fragments.add(storage_fragment)
        }

        return fragments.getOrNull(main_view_pager.currentItem)
    }

    private fun getTabsList() = arrayListOf(TAB_FILES, TAB_RECENT_FILES, TAB_STORAGE_ANALYSIS)

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
