package com.simplemobiletools.rvpdfviewer.pdfviewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import kotlin.math.max
import kotlin.math.min

class PDFPageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val mPaint: Paint = Paint()
    private val mMatrix: Matrix = Matrix()
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var mScaleFactor = 1f
    private val mScaleGestureDetector: ScaleGestureDetector
    private var mPage: PDFPage? = null

    init {
        mScaleGestureDetector = ScaleGestureDetector(context!!, ScaleListener())
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPage?.draw(canvas, mMatrix, mPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = x
                mLastTouchY = y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY
                mPosX += dx
                mPosY += dy
                mMatrix.setTranslate(mPosX, mPosY)
                invalidate()
                mLastTouchX = x
                mLastTouchY = y
            }
        }
        return true
    }

    fun setPage(page: PDFPage?) {
        mPage = page
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            mScaleFactor *= scaleFactor
            mScaleFactor = max(1f, min(mScaleFactor, 5f))
            mMatrix.setScale(mScaleFactor, mScaleFactor)
            invalidate()
            return true
        }
    }
}
