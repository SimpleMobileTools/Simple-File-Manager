package com.simplemobiletools.filemanager.pro.activities

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.WindowManager
import android.widget.RelativeLayout
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.shockwave.pdfium.PdfPasswordException
import com.simplemobiletools.commons.dialogs.EnterPasswordDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.hideSystemUI
import com.simplemobiletools.filemanager.pro.extensions.showSystemUI
import com.simplemobiletools.filemanager.pro.helpers.PdfDocumentAdapter
import kotlinx.android.synthetic.main.activity_pdf_viewer.pdf_viewer
import kotlinx.android.synthetic.main.activity_pdf_viewer.pdf_viewer_appbar
import kotlinx.android.synthetic.main.activity_pdf_viewer.pdf_viewer_toolbar
import kotlinx.android.synthetic.main.activity_pdf_viewer.top_shadow

class PDFViewerActivity : SimpleActivity() {
    private var realFilePath = ""
    private var isFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        showTransparentTop = true

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        if (checkAppSideloading()) {
            return
        }

        checkNotchSupport()
        pdf_viewer_toolbar.apply {
            setTitleTextColor(Color.WHITE)
            overflowIcon = resources.getColoredDrawableWithColor(R.drawable.ic_three_dots_vector, Color.WHITE)
            navigationIcon = resources.getColoredDrawableWithColor(R.drawable.ic_arrow_left_vector, Color.WHITE)
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            realFilePath = intent.extras?.get(REAL_FILE_PATH)?.toString() ?: ""
            pdf_viewer_toolbar.title = realFilePath.getFilenameFromPath()
        }

        setupMenu()
        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun setupMenu() {
        (pdf_viewer_appbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        pdf_viewer_toolbar.menu.apply {
            findItem(R.id.menu_print).isVisible = realFilePath.isNotEmpty()
            findItem(R.id.menu_print).setOnMenuItemClickListener {
                printText()
                true
            }
        }

        pdf_viewer_toolbar.setNavigationOnClickListener {
            finish()
        }

        if (!portrait && navigationBarOnSide && navigationBarWidth > 0) {
            pdf_viewer_appbar.setPadding(0, 0, navigationBarWidth, 0)
        } else {
            pdf_viewer_appbar.setPadding(0, 0, 0, 0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (pdf_viewer_appbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        if (!portrait && navigationBarOnSide && navigationBarWidth > 0) {
            pdf_viewer_appbar.setPadding(0, 0, navigationBarWidth, 0)
        } else {
            pdf_viewer_appbar.setPadding(0, 0, 0, 0)
        }
    }

    private fun checkIntent() {
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        loadPdfViewer(uri)
    }

    private fun loadPdfViewer(uri: Uri, filePassword: String? = null) {
        val primaryColor = getProperPrimaryColor()
        pdf_viewer.setBackgroundColor(getProperBackgroundColor())
        pdf_viewer.fromUri(uri)
            .password(filePassword)
            .scrollHandle(DefaultScrollHandle(this, primaryColor.getContrastColor(), primaryColor))
            .spacing(15)
            .onTap { toggleFullScreen() }
            .onError {
                if (it is PdfPasswordException) {
                    // Already entered a password and it was wrong
                    if (filePassword != null) {
                        showErrorToast(getString(R.string.invalid_password))
                        finish()
                    } else {
                        EnterPasswordDialog(
                            this,
                            callback = { password -> loadPdfViewer(uri, password) },
                            cancelCallback = { finish() }
                        )
                    }
                } else {
                    showErrorToast(it.localizedMessage?.toString() ?: getString(R.string.unknown_error_occurred))
                    finish()
                }
            }
            .load()

        showSystemUI(true)

        val filename = getFilenameFromUri(uri)
        if (filename.isNotEmpty()) {
            pdf_viewer_toolbar.title = filename
        }
    }

    private fun printText() {
        val adapter = PdfDocumentAdapter(this, realFilePath)

        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.apply {
            print(realFilePath.getFilenameFromPath(), adapter, PrintAttributes.Builder().build())
        }
    }

    private fun toggleFullScreen(): Boolean {
        isFullScreen = !isFullScreen
        val newAlpha: Float
        if (isFullScreen) {
            newAlpha = 0f
            hideSystemUI(true)
        } else {
            newAlpha = 1f
            showSystemUI(true)
        }

        top_shadow.animate().alpha(newAlpha).start()
        pdf_viewer_appbar.animate().alpha(newAlpha).withStartAction {
            if (newAlpha == 1f) {
                pdf_viewer_appbar.beVisible()
            }
        }.withEndAction {
            if (newAlpha == 0f) {
                pdf_viewer_appbar.beGone()
            }
        }.start()

        // return false to also toggle scroll handle
        return true
    }

    private fun checkNotchSupport() {
        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }
}
