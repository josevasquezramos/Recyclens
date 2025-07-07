package com.episi.recyclens.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.episi.recyclens.R
import com.episi.recyclens.domain.camera.BoundingBox

class CameraOverlayView(
    context: Context?,
    attrs: AttributeSet?
): View(context, attrs) {

    private var results = listOf<BoundingBox>()

    private val boxPaint = Paint().apply {
        color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 35f
        style = Paint.Style.FILL
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        textSize = 35f
    }

    private val bounds = Rect()

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    fun clear() {
        results = emptyList()
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (box in results) {
            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height

            canvas.drawRect(left, top, right, bottom, boxPaint)

            val confidencePercent = (box.cnf * 100).toInt()
            val text = "${box.clsName} ${confidencePercent}%"
            textBackgroundPaint.getTextBounds(text, 0, text.length, bounds)

            val textWidth = bounds.width()
            val textHeight = bounds.height()

            canvas.drawRect(
                left,
                top,
                left + textWidth + TEXT_PADDING,
                top + textHeight + TEXT_PADDING,
                textBackgroundPaint
            )

            canvas.drawText(text, left, top + textHeight, textPaint)
        }
    }

    companion object {
        private const val TEXT_PADDING = 8
    }
}