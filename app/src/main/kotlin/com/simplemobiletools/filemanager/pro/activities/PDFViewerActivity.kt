package com.simplemobiletools.filemanager.pro.activities

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.checkAppSideloading
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getFilenameFromUri
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.helpers.PdfDocumentAdapter
import kotlinx.android.synthetic.main.activity_pdf_viewer.*

class PDFViewerActivity : SimpleActivity() {
    var realFilePath = ""

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

        pdf_viewer.setBackgroundColor(getProperBackgroundColor())
        pdf_viewer.fromUri(uri)
            .spacing(15)
            .load()
    }

    private fun printText() {
        val adapter = PdfDocumentAdapter(this, realFilePath)

        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.apply {
            print(realFilePath.getFilenameFromPath(), adapter, PrintAttributes.Builder().build())
        }
    }
}
