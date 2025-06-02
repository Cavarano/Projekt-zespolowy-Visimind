package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.trafficeye2.models.Box
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import kotlin.math.exp
import kotlin.math.roundToInt

class Detector(
    private val context: Context,
    private val detectorListener: DetectorListener
) {

    private val modelPath = "best_float32.tflite"
    private val labelPath = "labels.txt"

    private var interpreter: Interpreter
    private var labels = listOf<String>()

    // Przetwarzanie obrazu wejściowego: normalizacja i konwersja do float32
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(0f, 255f)) // Skala 0–1
        .add(CastOp(DataType.FLOAT32))
        .build()

    init {
        // Inicjalizacja interpretera TFLite z CPU
        val options = Interpreter.Options().apply {
            setNumThreads(4) // lub mniej
        }


        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), options)
        labels = FileUtil.loadLabels(context, labelPath)

        Log.d(TAG, "Loaded labels: ${labels.size}")
    }

    /**
     * Główna funkcja wywoływana przy każdym nowym obrazie do analizy.
     */
    fun detect(frame: Bitmap) {
        // Przeskalowanie bitmapy do 640x640 – zgodnie z wymiarem wejścia modelu
        val resized = Bitmap.createScaledBitmap(frame, 640, 640, true)

        // Konwersja do formatu TensorImage
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resized)
        val inputBuffer = imageProcessor.process(tensorImage).buffer

        // Przygotowanie struktury do przechwycenia wyjścia modelu
        val outputRaw = Array(1) { Array(104) { FloatArray(8400) } }
        interpreter.run(inputBuffer, outputRaw)

        // Transpozycja danych wyjściowych: [1][104][8400] → [8400][104]
        val detections = Array(8400) { FloatArray(104) }
        for (i in 0 until 104) {
            for (j in 0 until 8400) {
                detections[j][i] = outputRaw[0][i][j]
            }
        }

        // Pomiar czasu wnioskowania (inference)
        val startTime = SystemClock.uptimeMillis()
        val boxes = parseDetections(detections)
        val inferenceTime = SystemClock.uptimeMillis() - startTime

        // Zwrot do UI przez listener
        if (boxes.isEmpty()) {
            detectorListener.onEmptyDetect()
        } else {
            detectorListener.onDetect(boxes, inferenceTime)
        }
    }

    /**
     * Funkcja aktywująca sigmoidalną aktywację.
     */
    private fun sigmoid(x: Float): Float = (1f / (1f + exp(-x)))

    /**
     * Przekształcenie wyników modelu do listy Boxów (detekcji).
     */
    private fun parseDetections(data: Array<FloatArray>): List<Box> {
        val boxes = mutableListOf<Box>()

        for (i in data.indices) {
            val row = data[i]
            if (row.size < 6) continue // Pomijamy niepełne dane

            // Rozpakowanie współrzędnych i pewności
            val cx = row[0]
            val cy = row[1]
            val w = row[2]
            val h = row[3]
            val confidence = sigmoid(row[4])
            if (confidence < CONFIDENCE_THRESHOLD) continue

            // Skorygowane klasy i ich score
            val classScores = row.copyOfRange(5, row.size).map { sigmoid(it) }
            val maxIdx = classScores.indices.maxByOrNull { classScores[it] } ?: continue
            val score = classScores[maxIdx] * confidence
            if (score < CONFIDENCE_THRESHOLD) continue

            // Przeskalowanie współrzędnych (relatywnych) do obrazu
            val scaledCx = cx / 640f
            val scaledCy = cy / 640f
            val scaledW = w / 640f
            val scaledH = h / 640f

            // Obliczenie ramki (box) z punktu środkowego i szerokości/wysokości
            val x1 = ((scaledCx - scaledW / 2f) * 640).roundToInt().coerceIn(0, 639)
            val y1 = ((scaledCy - scaledH / 2f) * 640).roundToInt().coerceIn(0, 639)
            val x2 = ((scaledCx + scaledW / 2f) * 640).roundToInt().coerceIn(0, 639)
            val y2 = ((scaledCy + scaledH / 2f) * 640).roundToInt().coerceIn(0, 639)

            val area = (x2 - x1) * (y2 - y1)
            if (area < MIN_AREA_THRESHOLD) continue // Zbyt mała detekcja

            if (maxIdx >= labels.size) continue

            // Dodanie wykrytego obiektu do listy
            boxes.add(
                Box(
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    class_id = labels[maxIdx],
                    confidence = score,
                    timeDetected = null
                )
            )

            Log.d(TAG, "Detected: ${labels[maxIdx]} (${String.format("%.2f", score * 100)}%)")
        }

        Log.d(TAG, "Total valid detections: ${boxes.size}")
        return applyNMS(boxes).take(10) // Maksymalnie 10 najlepszych ramek po NMS
    }

    /**
     * Algorytm Non-Maximum Suppression: usuwa nakładające się ramki.
     */
    private fun applyNMS(boxes: List<Box>): List<Box> {
        val selected = mutableListOf<Box>()
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()

        while (sorted.isNotEmpty()) {
            val chosen = sorted.removeAt(0)
            selected.add(chosen)

            val iter = sorted.iterator()
            while (iter.hasNext()) {
                val next = iter.next()
                val iou = calculateIoU(chosen, next)
                if (iou > IOU_THRESHOLD) iter.remove()
            }
        }
        return selected
    }

    /**
     * Obliczenie współczynnika pokrycia (IoU) pomiędzy dwiema ramkami.
     */
    private fun calculateIoU(a: Box, b: Box): Float {
        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)
        val inter = maxOf(0, x2 - x1) * maxOf(0, y2 - y1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        val union = areaA + areaB - inter
        return if (union <= 0f) 0f else inter.toFloat() / union.toFloat()
    }

    /**
     * Interfejs callbacków do komunikacji z UI.
     */
    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<Box>, inferenceTime: Long)
    }

    companion object {
        private const val TAG = "Detector"

        // Minimalna pewność detekcji, by była brana pod uwagę
        private const val CONFIDENCE_THRESHOLD = 0.25f

        // Próg IoU dla usuwania nakładających się ramek
        private const val IOU_THRESHOLD = 0.45f

        // Minimalny rozmiar powierzchni obiektu (30x30 px)
        private const val MIN_AREA_THRESHOLD = 900
    }
}
