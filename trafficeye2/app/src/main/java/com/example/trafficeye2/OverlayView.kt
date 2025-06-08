package com.example.trafficeye2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.trafficeye2.models.BoundingBox


class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()

    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        Log.d("OverlayView", "draw() called with ${results.size} boxes")

        results.forEach {
            val left = (it.x1 * width).coerceIn(0f, width.toFloat())
            val top = (it.y1 * height).coerceIn(0f, height.toFloat())
            val right = (it.x2 * width).coerceIn(0f, width.toFloat())
            val bottom = (it.y2 * height).coerceIn(0f, height.toFloat())

            canvas.drawRect(left, top, right, bottom, boxPaint)

            val label = "${it.clsName} (${String.format("%.1f", it.cnf * 100)}%)"

            textBackgroundPaint.getTextBounds(label, 0, label.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            val textX = (left + right) / 2f - textWidth / 2f
            val textY = (top - 8f).coerceAtLeast(textHeight.toFloat() + 4f)

            canvas.drawRect(
                textX - 6,
                textY - textHeight - 6,
                textX + textWidth + 6,
                textY + 6,
                textBackgroundPaint
            )

            canvas.drawText(label, textX, textY, textPaint)

            Log.d("OverlayView", "Drawing label '$label' at x=${textX.toInt()} y=${textY.toInt()}")
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        Log.d("OverlayView", "setResults() called with ${boundingBoxes.size} results")
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
