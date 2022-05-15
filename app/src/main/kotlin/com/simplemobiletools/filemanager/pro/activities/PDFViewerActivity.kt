package com.simplemobiletools.filemanager.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.checkAppSideloading
import com.simplemobiletools.commons.extensions.getFilenameFromUri
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.filemanager.pro.R
import kotlinx.android.synthetic.main.activity_pdf_viewer.*

class PDFViewerActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        if (checkAppSideloading()) {
            return
        }

        checkIntent()
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
}
