package com.simplemobiletools.filemanager.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.checkAppSideloading
import com.simplemobiletools.filemanager.pro.R

class PDFViewerActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        if (checkAppSideloading()) {
            return
        }
    }
}
