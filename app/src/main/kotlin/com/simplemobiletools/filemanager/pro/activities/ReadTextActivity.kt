package com.simplemobiletools.filemanager.pro.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.SAVE_DISCARD_PROMPT_INTERVAL
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.views.MyEditText
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.dialogs.SaveAsDialog
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.openPath
import com.simplemobiletools.filemanager.pro.views.GestureEditText
import kotlinx.android.synthetic.main.activity_read_text.*
import java.io.File
import java.io.OutputStream

class ReadTextActivity : SimpleActivity() {
    private val SELECT_SAVE_FILE_INTENT = 1
    private val SELECT_SAVE_FILE_AND_EXIT_INTENT = 2

    private var filePath = ""
    private var originalText = ""
    private var searchIndex = 0
    private var lastSavePromptTS = 0L
    private var searchMatches = emptyList<Int>()
    private var isSearchActive = false

    private lateinit var searchQueryET: MyEditText
    private lateinit var searchPrevBtn: ImageView
    private lateinit var searchNextBtn: ImageView
    private lateinit var searchClearBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_text)
        setupOptionsMenu()
        updateMaterialActivityViews(read_text_coordinator, read_text_view, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(read_text_holder, read_text_toolbar)

        searchQueryET = findViewById(R.id.search_query)
        searchPrevBtn = findViewById(R.id.search_previous)
        searchNextBtn = findViewById(R.id.search_next)
        searchClearBtn = findViewById(R.id.search_clear)
        read_text_view.setTextColor(config.textColor)

        if (checkAppSideloading()) {
            return
        }

        val uri = if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            Uri.fromFile(File(intent.extras?.get(REAL_FILE_PATH).toString()))
        } else {
            intent.data
        }

        if (uri == null) {
            finish()
            return
        }

        val filename = getFilenameFromUri(uri)
        if (filename.isNotEmpty()) {
            read_text_toolbar.title = Uri.decode(filename)
        }

        read_text_view.onGlobalLayout {
            ensureBackgroundThread {
                checkIntent(uri)
            }
        }

        setupSearchButtons()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(read_text_toolbar, NavigationIcon.Arrow)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == SELECT_SAVE_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)

            val shouldExitAfterSaving = requestCode == SELECT_SAVE_FILE_AND_EXIT_INTENT

            val selectedFilePath = getRealPathFromURI(intent.data!!)
            val shouldOverwriteOriginalText = selectedFilePath == filePath

            saveTextContent(outputStream, shouldExitAfterSaving, shouldOverwriteOriginalText)
        }
    }

    override fun onBackPressed() {
        val hasUnsavedChanges = originalText != read_text_view.text.toString()
        when {
            isSearchActive -> closeSearch()
            hasUnsavedChanges && System.currentTimeMillis() - lastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL -> {
                lastSavePromptTS = System.currentTimeMillis()
                ConfirmationAdvancedDialog(this, "", R.string.save_before_closing, R.string.save, R.string.discard) {
                    if (it) {
                        saveText(true)
                    } else {
                        super.onBackPressed()
                    }
                }
            }
            else -> super.onBackPressed()
        }
    }

    private fun setupOptionsMenu() {
        read_text_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_search -> openSearch()
                R.id.menu_save -> saveText()
                R.id.menu_open_with -> openPath(intent.dataString!!, true)
                R.id.menu_print -> printText()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun openSearch() {
        isSearchActive = true
        search_wrapper.beVisible()
        showKeyboard(searchQueryET)

        read_text_view.requestFocus()
        read_text_view.setSelection(0)

        searchQueryET.postDelayed({
            searchQueryET.requestFocus()
        }, 250)
    }

    private fun saveText(shouldExitAfterSaving: Boolean = false) {
        if (filePath.isEmpty()) {
            filePath = getRealPathFromURI(intent.data!!) ?: ""
        }

        if (filePath.isEmpty()) {
            SaveAsDialog(this, filePath, true) { _, filename ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, filename)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    val requestCode = if (shouldExitAfterSaving) {
                        SELECT_SAVE_FILE_AND_EXIT_INTENT
                    } else {
                        SELECT_SAVE_FILE_INTENT
                    }
                    startActivityForResult(this, requestCode)
                }
            }
        } else {
            SaveAsDialog(this, filePath, false) { path, _ ->
                if (hasStoragePermission()) {
                    val file = File(path)
                    getFileOutputStream(file.toFileDirItem(this), true) {
                        val shouldOverwriteOriginalText = path == filePath
                        saveTextContent(it, shouldExitAfterSaving, shouldOverwriteOriginalText)
                    }
                } else {
                    toast(R.string.no_storage_permissions)
                }
            }
        }
    }

    private fun saveTextContent(outputStream: OutputStream?, shouldExitAfterSaving: Boolean, shouldOverwriteOriginalText: Boolean) {
        if (outputStream != null) {
            val currentText = read_text_view.text.toString()
            outputStream.bufferedWriter().use { it.write(currentText) }
            toast(R.string.file_saved)
            hideKeyboard()

            if (shouldOverwriteOriginalText) {
                originalText = currentText
            }

            if (shouldExitAfterSaving) {
                super.onBackPressed()
            }
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun printText() {
        try {
            val webView = WebView(this)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

                override fun onPageFinished(view: WebView, url: String) {
                    createWebPrintJob(view)
                }
            }

            webView.loadData(read_text_view.text.toString(), "text/plain", "UTF-8")
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun createWebPrintJob(webView: WebView) {
        val jobName = if (filePath.isNotEmpty()) {
            filePath.getFilenameFromPath()
        } else {
            getString(R.string.app_name)
        }

        val printAdapter = webView.createPrintDocumentAdapter(jobName)

        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.apply {
            print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    private fun checkIntent(uri: Uri) {
        originalText = if (uri.scheme == "file") {
            filePath = uri.path!!
            val file = File(filePath)
            if (file.exists()) {
                try {
                    file.readText()
                } catch (e: Exception) {
                    showErrorToast(e)
                    ""
                }
            } else {
                toast(R.string.unknown_error_occurred)
                ""
            }
        } else {
            try {
                contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            } catch (e: OutOfMemoryError) {
                showErrorToast(e.toString())
                return
            } catch (e: Exception) {
                showErrorToast(e)
                finish()
                return
            }
        }

        runOnUiThread {
            read_text_view.setText(originalText)
            if (originalText.isNotEmpty()) {
                hideKeyboard()
            } else {
                showKeyboard(read_text_view)
            }
        }
    }

    private fun setupSearchButtons() {
        searchQueryET.onTextChangeListener {
            searchTextChanged(it)
        }

        searchPrevBtn.setOnClickListener {
            goToPrevSearchResult()
        }

        searchNextBtn.setOnClickListener {
            goToNextSearchResult()
        }

        searchClearBtn.setOnClickListener {
            closeSearch()
        }

        searchQueryET.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchNextBtn.performClick()
                return@OnEditorActionListener true
            }

            false
        })

        search_wrapper.setBackgroundColor(getProperPrimaryColor())
        val contrastColor = getProperPrimaryColor().getContrastColor()
        arrayListOf(searchPrevBtn, searchNextBtn, searchClearBtn).forEach {
            it.applyColorFilter(contrastColor)
        }
    }

    private fun searchTextChanged(text: String) {
        read_text_view.text?.clearBackgroundSpans()

        if (text.isNotBlank() && text.length > 1) {
            searchMatches = read_text_view.value.searchMatches(text)
            read_text_view.highlightText(text, getProperPrimaryColor())
        }

        if (searchMatches.isNotEmpty()) {
            read_text_view.requestFocus()
            read_text_view.setSelection(searchMatches.getOrNull(searchIndex) ?: 0)
        }

        searchQueryET.postDelayed({
            searchQueryET.requestFocus()
        }, 50)
    }

    private fun goToPrevSearchResult() {
        if (searchIndex > 0) {
            searchIndex--
        } else {
            searchIndex = searchMatches.lastIndex
        }

        selectSearchMatch(read_text_view)
    }

    private fun goToNextSearchResult() {
        if (searchIndex < searchMatches.lastIndex) {
            searchIndex++
        } else {
            searchIndex = 0
        }

        selectSearchMatch(read_text_view)
    }

    private fun closeSearch() {
        searchQueryET.text?.clear()
        isSearchActive = false
        search_wrapper.beGone()
    }

    private fun selectSearchMatch(editText: GestureEditText) {
        if (searchMatches.isNotEmpty()) {
            editText.requestFocus()
            editText.setSelection(searchMatches.getOrNull(searchIndex) ?: 0)
        } else {
            hideKeyboard()
        }
    }
}
