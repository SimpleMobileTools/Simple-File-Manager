package com.simplemobiletools.filemanager.pro.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
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

    private var filePath = ""
    private var originalText = ""
    private var searchIndex = 0
    private var searchMatches = emptyList<Int>()
    private var isSearchActive = false

    private lateinit var searchQueryET: MyEditText
    private lateinit var searchPrevBtn: ImageView
    private lateinit var searchNextBtn: ImageView
    private lateinit var searchClearBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_text)
        searchQueryET = findViewById(R.id.search_query)
        searchPrevBtn = findViewById(R.id.search_previous)
        searchNextBtn = findViewById(R.id.search_next)
        searchClearBtn = findViewById(R.id.search_clear)

        if (checkAppSideloading()) {
            return
        }

        read_text_view.onGlobalLayout {
            checkIntent()
        }

        setupSearchButtons()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> openSearch()
            R.id.menu_save -> saveText()
            R.id.menu_open_with -> openPath(intent.dataString!!, true)
            R.id.menu_print -> printText()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == SELECT_SAVE_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            saveTextContent(outputStream)
        }
    }

    override fun onBackPressed() {
        if (isSearchActive) {
            closeSearch()
        } else {
            super.onBackPressed()
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

    private fun saveText() {
        if (filePath.isEmpty()) {
            filePath = getRealPathFromURI(intent.data!!) ?: ""
        }

        if (filePath.isEmpty()) {
            SaveAsDialog(this, filePath, true) { path, filename ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, filename)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    startActivityForResult(this, SELECT_SAVE_FILE_INTENT)
                }
            }
        } else {
            SaveAsDialog(this, filePath, false) { path, filename ->
                handlePermission(PERMISSION_WRITE_STORAGE) {
                    if (it) {
                        val file = File(path)
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            saveTextContent(it)
                        }
                    }
                }
            }
        }
    }

    private fun saveTextContent(outputStream: OutputStream?) {
        if (outputStream != null) {
            outputStream.bufferedWriter().use { it.write(read_text_view.text.toString()) }
            toast(R.string.file_saved)
            hideKeyboard()
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

    private fun checkIntent() {
        read_text_view.setTextColor(config.textColor)
        val uri = if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            Uri.fromFile(File(intent.extras?.get(REAL_FILE_PATH).toString()))
        } else {
            intent.data
        }

        if (uri == null) {
            finish()
            return
        }

        ensureBackgroundThread {
            originalText = if (uri.scheme == "file") {
                filePath = uri.path!!
                val file = File(filePath)
                if (file.exists()) {
                    file.readText()
                } else {
                    toast(R.string.unknown_error_occurred)
                    ""
                }
            } else {
                try {
                    contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    showErrorToast(e)
                    finish()
                    return@ensureBackgroundThread
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

        search_wrapper.setBackgroundColor(config.primaryColor)
        val contrastColor = config.primaryColor.getContrastColor()
        arrayListOf(searchPrevBtn, searchNextBtn, searchClearBtn).forEach {
            it.applyColorFilter(contrastColor)
        }
    }

    private fun searchTextChanged(text: String) {
        read_text_view.text?.clearBackgroundSpans()

        if (text.isNotBlank() && text.length > 1) {
            searchMatches = read_text_view.value.searchMatches(text)
            read_text_view.highlightText(text, config.primaryColor)
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
        searchQueryET.text.clear()
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
