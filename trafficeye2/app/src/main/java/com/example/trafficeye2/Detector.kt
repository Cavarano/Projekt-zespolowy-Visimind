package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.trafficeye2.MetaData
import com.example.trafficeye2.models.BoundingBox
import com.example.trafficeye2.MetaData.extractNamesFromMetadata
import com.example.trafficeye2.MetaData.extractNamesFromLabelFile
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String?,
    private val detectorListener: DetectorListener,
    private val message: (String) -> Unit
) {

    private var interpreter: Interpreter
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(0f, 255f))
        .add(CastOp(DataType.FLOAT32))
        .build()

    init {
        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate())

            } else {
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()

        labels.addAll(extractNamesFromMetadata(model))
        if (labels.isEmpty()) {
            if (labelPath == null) {
                message("Model not contains metadata, provide LABELS_PATH in Constants.kt")
                labels.addAll(MetaData.TEMP_CLASSES)
            } else {
                labels.addAll(extractNamesFromLabelFile(context, labelPath))
            }
        }

        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numChannel = outputShape[1]
            numElements = outputShape[2]
        }
    }

    fun close() {
        interpreter.close()
    }

    fun detect(frame: Bitmap) {
        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return

        val startTime = SystemClock.uptimeMillis()

        val resized = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, true)
        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resized)
        val processedImage = imageProcessor.process(tensorImage)
        val inputBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter.run(inputBuffer, output.buffer)

        val boxes = parseDetections(output.floatArray)
        val inferenceTime = SystemClock.uptimeMillis() - startTime

        if (boxes.isEmpty()) {
            detectorListener.onEmptyDetect()
        } else {
            detectorListener.onDetect(boxes, inferenceTime)
        }
    }

    private fun parseDetections(array: FloatArray): List<BoundingBox> {
        val boxes = mutableListOf<BoundingBox>()

        for (i in 0 until numElements) {
            var maxConf = -1f
            var maxIdx = -1
            var j = 4
            var arrayIdx = i + numElements * j

            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxIdx != -1 && maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels.getOrNull(maxIdx) ?: "unknown"

                val cx = array[i] / tensorWidth
                val cy = array[i + numElements] / tensorHeight
                val w = array[i + numElements * 2] / tensorWidth
                val h = array[i + numElements * 3] / tensorHeight

                val scale = 1.1f
                val adjW = w * scale
                val adjH = h * scale

                val x1 = (cx - adjW / 2f).coerceIn(0f, 1f)
                val y1 = (cy - adjH / 2f).coerceIn(0f, 1f)
                val x2 = (cx + adjW / 2f).coerceIn(0f, 1f)
                val y2 = (cy + adjH / 2f).coerceIn(0f, 1f)

                boxes.add(
                    BoundingBox(
                        x1, y1, x2, y2,
                        cx, cy, w, h,
                        maxConf, maxIdx, clsName
                    )
                )
            }
        }

        if (boxes.isEmpty()) return emptyList()

        return applyNMS(boxes).take(3)
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sorted = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selected = mutableListOf<BoundingBox>()

        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            selected.add(current)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (calculateIoU(current, next) >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selected
    }

    private fun calculateIoU(a: BoundingBox, b: BoundingBox): Float {
        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)
        val interArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val unionArea = a.w * a.h + b.w * b.h - interArea
        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.25f
        private const val IOU_THRESHOLD = 0.5f
    }
}
