package com.simplemobiletools.filemanager.pro.activities

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.commons.helpers.isSPlus
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.config
import com.simplemobiletools.filemanager.pro.extensions.getUiMode
import com.simplemobiletools.filemanager.pro.extensions.hideSystemUI
import com.simplemobiletools.filemanager.pro.extensions.showSystemUI
import com.simplemobiletools.filemanager.pro.helpers.PdfDocumentAdapter
import kotlinx.android.synthetic.main.activity_pdf_viewer.*


class PDFViewerActivity : SimpleActivity() {
    private var realFilePath = ""

    private var systemUiVisible = true
    private var pdfViewerHeight = -1
    private var positionOffset = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        if (checkAppSideloading()) {
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            realFilePath = intent.extras?.get(REAL_FILE_PATH)?.toString() ?: ""
        }

        checkIntent()
        setupFullScreenView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        menu.apply {
            findItem(R.id.menu_print).isVisible = realFilePath.isNotEmpty()
        }

        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_print -> printText()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun checkIntent() {
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val filename = getFilenameFromUri(uri)
        if (filename.isNotEmpty()) {
            title = filename
        }

        val primaryColor = getProperPrimaryColor()
        pdf_viewer.setBackgroundColor(getProperBackgroundColor())
        pdf_viewer.fromUri(uri)
            .scrollHandle(DefaultScrollHandle(this, primaryColor.getContrastColor(), primaryColor))
            .spacing(15)
            .load()
    }

    private fun printText() {
        val adapter = PdfDocumentAdapter(this, realFilePath)

        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.apply {
            print(realFilePath.getFilenameFromPath(), adapter, PrintAttributes.Builder().build())
        }
    }

    private fun setupFullScreenView() {
        pdf_viewer.setOnClickListener {
            if (systemUiVisible) enterFullScreen() else exitFullScreen()
            systemUiVisible = !systemUiVisible
        }
        setupNotch()
    }

    private fun enterFullScreen() {
        if (pdfViewerHeight == -1) {
            pdfViewerHeight = pdf_viewer.height
        }
        positionOffset = pdf_viewer.positionOffset
        hideSystemUI(true)

        pdf_viewer.updateLayoutParams<RelativeLayout.LayoutParams> {
            // hack to workaround pdf viewer height glitch
            this.height = pdf_viewer_wrapper.height + statusBarHeight + actionBarHeight
        }
    }

    private fun exitFullScreen() {
        showSystemUI(true)
        pdf_viewer.updateLayoutParams<RelativeLayout.LayoutParams> {
            this.height = pdfViewerHeight
        }
        pdf_viewer.post { pdf_viewer.positionOffset = positionOffset }

        @Suppress("DEPRECATION")
        // use light status bar on material you
        if (isSPlus() && config.isUsingSystemTheme && getUiMode() == UI_MODE_NIGHT_NO) {
            val flags = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }


    private fun setupNotch() {
        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }
}
