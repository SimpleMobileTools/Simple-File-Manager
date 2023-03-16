package com.simplemobiletools.filemanager.pro.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.extensions.hideSystemUI
import com.simplemobiletools.filemanager.pro.extensions.showSystemUI
import com.simplemobiletools.filemanager.pro.helpers.PdfDocumentAdapter
import com.simplemobiletools.filemanager.pro.helpers.pdfviewer.PDFRendererAdapter
import com.simplemobiletools.filemanager.pro.helpers.pdfviewer.PDFViewer
import kotlinx.android.synthetic.main.activity_pdf_viewer.*


open class PDFViewerActivity : SimpleActivity() {
    private lateinit var mAdapter: PDFRendererAdapter
    private var realFilePath = ""

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
            realFilePath = intent.extras?.getString(REAL_FILE_PATH) ?: ""
            pdf_viewer_toolbar.title = realFilePath.getFilenameFromPath()
        }

        setupMenu()
        mAdapter = PDFRendererAdapter(this, onPageClick = {
            toggleFullScreen()
        })
        pdf_recycler_view
            .adapter = mAdapter
        pdf_recycler_view
            .layoutManager = LinearLayoutManager(this)

        pdf_recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val firstVisiblePosition = layoutManager!!.findFirstVisibleItemPosition()
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                val centerPosition = (firstVisiblePosition + lastVisiblePosition) / 2
                updatePageCounter(centerPosition)
            }
        })

        checkIntent()
        updatePageCounter(0)
        page_counter.beVisible()
        showSystemUI(true)

    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter.close()
    }

    private fun checkIntent() {
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val pdfPath = uri.toString()
        val viewer =  PDFViewer(this, pdfPath)
        viewer.getPages().forEach {
            mAdapter.addPage(it)
        }
    }

    private var isFullScreen = false

    override fun onResume() {
        super.onResume()
        window.navigationBarColor = Color.TRANSPARENT
        pdf_fastscroller.updateColors(getProperPrimaryColor())
    }

    private fun setupMenu() {
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

        setupViewOffsets()
        val primaryColor = getProperPrimaryColor()
        page_counter.background?.applyColorFilter(primaryColor)
        page_counter.setTextColor(primaryColor.getContrastColor())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupViewOffsets()
    }

    private fun setupViewOffsets() {
        val pageCounterMargin = resources.getDimension(R.dimen.normal_margin).toInt()
        (pdf_viewer_appbar.layoutParams as ConstraintLayout.LayoutParams).topMargin = statusBarHeight
        if (!portrait && navigationBarOnSide && navigationBarWidth > 0) {
            pdf_viewer_appbar.setPadding(0, 0, navigationBarWidth, 0)
        } else {
            pdf_viewer_appbar.setPadding(0, 0, 0, 0)
        }

        (page_counter.layoutParams as ConstraintLayout.LayoutParams).apply {
            rightMargin = navigationBarWidth + pageCounterMargin
            bottomMargin = navigationBarHeight + pageCounterMargin
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePageCounter(position: Int) {
        page_counter.text = "${position + 1} / ${pdf_recycler_view.adapter?.itemCount ?: 0}"
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

        page_counter.animate().alpha(newAlpha).start()
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
