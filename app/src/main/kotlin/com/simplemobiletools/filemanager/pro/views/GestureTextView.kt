package com.simplemobiletools.filemanager.pro.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.alexvasilkov.gestures.views.interfaces.GestureView
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.filemanager.pro.extensions.config

// taken from
// https://github.com/alexvasilkov/GestureViews/blob/f0a4c266e31dcad23bd0d9013531bc1c501b9c9f/sample/src/main/java/com/alexvasilkov/gestures/sample/ex/custom/text/GestureTextView.java
class GestureTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : AppCompatTextView(context, attrs, defStyle), GestureView {
    private val controller: GestureController = GestureController(this)
    private var origSize = 0f
    private var size = 0f

    init {
        controller.settings.setOverzoomFactor(1f).isPanEnabled = false
        controller.settings.initFromAttributes(context, attrs)
        controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateChanged(state: State) {
                applyState(state)
            }

            override fun onStateReset(oldState: State, newState: State) {
                applyState(newState)
            }
        })

        origSize = textSize
        setTextColor(context.config.textColor)
        setLinkTextColor(context.getAdjustedPrimaryColor())
    }

    override fun getController() = controller

    override fun onTouchEvent(event: MotionEvent) = controller.onTouch(this, event)

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        origSize = textSize
        applyState(controller.state)
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        origSize = textSize
        applyState(controller.state)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        controller.settings.setViewport(width, height).setImage(width, height)
        controller.updateState()
    }

    private fun applyState(state: State) {
        var size = origSize * state.zoom
        val maxSize = origSize * controller.stateController.getMaxZoom(state)
        size = Math.max(origSize, Math.min(size, maxSize))

        size = Math.round(size).toFloat()
        if (!State.equals(this.size, size)) {
            this.size = size
            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        }
    }
}
