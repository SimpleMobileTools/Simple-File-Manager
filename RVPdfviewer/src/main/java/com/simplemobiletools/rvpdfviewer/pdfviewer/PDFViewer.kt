package com.simplemobiletools.rvpdfviewer.pdfviewer

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import java.net.URI

class PDFViewer(private val context: Context, pdfPath: String, onPDFError: (Exception) -> Unit = {}) {
    private var pages = mutableListOf<PDFPage>()

    init {
        try {
            // Open the PDF file and create a renderer for it
            val renderer: PdfRenderer = if (pdfPath.startsWith("content://")) {
                val fileDescriptor = context.contentResolver.openFileDescriptor(Uri.parse(pdfPath), "r")!!
                PdfRenderer(fileDescriptor)
            } else {
                PdfRenderer(getSeekableFileDescriptor(pdfPath)!!)
            }
            val pageCount = renderer.pageCount
            // Add each page of the PDF file to the adapter
            for (i in 0 until pageCount) {
                val page = PDFPage(renderer, i)
                pages.add(page)
            }

            // Close the renderer
            renderer.close()
        } catch (e: IOException) {
            e.printStackTrace()
            onPDFError(e)
        }
    }

    @Throws(IOException::class)
    private fun getSeekableFileDescriptor(path: String): ParcelFileDescriptor? {
        val parcelFileDescriptor: ParcelFileDescriptor?
        var pdfCopy = File(path)
        if (pdfCopy.exists()) {
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfCopy, ParcelFileDescriptor.MODE_READ_ONLY)
            return parcelFileDescriptor
        }
        if (isAnAsset(path)) {
            pdfCopy = File(context.cacheDir, path)
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfCopy, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            val uri = URI.create(String.format("file://%s", path))
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(Uri.parse(uri.toString()), "rw")
        }
        return parcelFileDescriptor
    }

    private fun isAnAsset(path: String?): Boolean {
        return path?.startsWith("/") != true
    }

    fun getPages() = pages
}
