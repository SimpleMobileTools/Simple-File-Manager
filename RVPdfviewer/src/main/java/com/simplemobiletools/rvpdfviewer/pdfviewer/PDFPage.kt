package com.simplemobiletools.rvpdfviewer.pdfviewer

import android.graphics.*
import android.graphics.pdf.PdfRenderer

class PDFPage(renderer: PdfRenderer, pageNumber: Int) {
    private val mPage: PdfRenderer.Page
    val bitmap: Bitmap
    private val DEFAULT_QUALITY = 2.0f

    init {
        val params: PdfRendererParams? = extractPdfParamsFromFirstPage(renderer)
        val bitmapContainer = SimpleBitmapPool(params)

        mPage = renderer.openPage(pageNumber)
        bitmap = bitmapContainer.get(pageNumber)
        mPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        mPage.close()
    }

    fun draw(canvas: Canvas, matrix: Matrix, paint: Paint) {
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun extractPdfParamsFromFirstPage(renderer: PdfRenderer?): PdfRendererParams? {
        val samplePage: PdfRenderer.Page = renderer?.openPage(0) ?: return null
        val params = PdfRendererParams()
        params.width = (samplePage.width * DEFAULT_QUALITY).toInt()
        params.height = (samplePage.height * DEFAULT_QUALITY).toInt()
        samplePage.close()
        return params
    }
}
