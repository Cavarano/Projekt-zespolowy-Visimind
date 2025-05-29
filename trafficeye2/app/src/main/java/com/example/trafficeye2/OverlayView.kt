package com.example.trafficeye2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.trafficeye2.models.Box

// Widok odpowiedzialny za rysowanie ramek wykrytych obiektów oraz podpisów
class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // Lista aktualnych wyników detekcji
    private var results = listOf<Box>()

    // Narzędzia graficzne do rysowania: ramka, tło tekstu, tekst
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    // Pomocniczy prostokąt do obliczania wymiarów tekstu
    private var bounds = Rect()

    init {
        initPaints() // Inicjalizacja farb
    }

    // Czyści wykrycia i resetuje farby
    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    // Ustawia parametry farb: kolory, style, rozmiary
    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        // Kolor ramki wczytywany z zasobów kolorów (np. res/values/colors.xml)
        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    // Nadpisuje metodę rysowania widoku
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            // Przeskalowanie współrzędnych wykrycia (model działa w przestrzeni 640x640)
            val left = it.x1.toFloat() / 640 * width
            val top = it.y1.toFloat() / 640 * height
            val right = it.x2.toFloat() / 640 * width
            val bottom = it.y2.toFloat() / 640 * height

            // Rysowanie prostokąta ramki
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Tworzenie etykiety z klasą i procentem pewności
            val confidencePercent = String.format("%.2f", it.confidence * 100)
            val drawableText = "${it.class_id} (${confidencePercent}%)"

            // Obliczenie wymiarów tekstu
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            // Rysowanie tła pod tekstem
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Rysowanie tekstu na górze ramki
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    // Ustawia nową listę wykryć i odświeża widok
    fun setResults(boundingBoxes: List<Box>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        // Odstęp w pikselach między tekstem a tłem
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
