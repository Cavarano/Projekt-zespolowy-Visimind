package com.example.trafficeye2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.trafficeye2.models.BoundingBox
import com.example.trafficeye2.Constants
import com.surendramaran.yolov8tflite.Detector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), Detector.DetectorListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService

    private var isActive = false
    private var isFrontCamera = false

    private lateinit var inferenceTime: TextView
    private lateinit var returnButton: View
    private lateinit var signImage: ImageView
    private lateinit var signName: TextView
    private lateinit var signDescription: TextView
    private lateinit var signBoxes: TextView
    private lateinit var detectedSignText: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isActive = true

        previewView = view.findViewById(R.id.camera_preview)
        inferenceTime = view.findViewById(R.id.inferenceTime)
        returnButton = view.findViewById(R.id.returnButton)
        signImage = view.findViewById(R.id.signImage)
        signName = view.findViewById(R.id.signName)
        signDescription = view.findViewById(R.id.signDescription)
        signBoxes = view.findViewById(R.id.signBoxes)
        detectedSignText = view.findViewById(R.id.detectedSignText)

        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        val rootLayout = view as ConstraintLayout
        overlayView = OverlayView(requireContext(), null).apply { id = View.generateViewId() }
        rootLayout.addView(overlayView, ConstraintLayout.LayoutParams(0, 0))

        val constraints = ConstraintSet()
        constraints.clone(rootLayout)
        constraints.connect(overlayView.id, ConstraintSet.TOP, R.id.camera_preview, ConstraintSet.TOP)
        constraints.connect(overlayView.id, ConstraintSet.BOTTOM, R.id.camera_preview, ConstraintSet.BOTTOM)
        constraints.connect(overlayView.id, ConstraintSet.START, R.id.camera_preview, ConstraintSet.START)
        constraints.connect(overlayView.id, ConstraintSet.END, R.id.camera_preview, ConstraintSet.END)
        constraints.applyTo(rootLayout)

        detector = Detector(requireContext(), Constants.MODEL_PATH, Constants.LABELS_PATH, this) {
            toast(it)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            toast("Brak uprawnień do kamery")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val rotation = previewView.display.rotation

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                val bitmap = Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
                imageProxy.use { bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    if (isFrontCamera) {
                        postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                    }
                }

                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                detector.detect(rotated)
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onEmptyDetect() {
        if (!isAdded) return
        activity?.runOnUiThread {
            overlayView.clear()
            inferenceTime.text = "0ms"
            detectedSignText.text = "Brak detekcji"
            signName.text = ""
            signDescription.text = ""
            signBoxes.text = ""
            signImage.visibility = View.GONE
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTimeMs: Long) {
        if (!isAdded) return
        activity?.runOnUiThread {
            overlayView.setResults(boundingBoxes)
            inferenceTime.text = "${inferenceTimeMs}ms"

            // Wyświetl nazwy wszystkich 3 wykrytych znaków
            val signNames = boundingBoxes.take(3).joinToString(", ") { it.clsName }
            detectedSignText.text = if (signNames.isNotEmpty()) signNames else "Brak detekcji"

            // Wyświetl szczegóły tylko dla pierwszego znaku (opcjonalnie można rozszerzyć)
            val top = boundingBoxes.firstOrNull()
            if (top != null) {
                signName.text = top.clsName
                signDescription.text = "Wykryto z %.1f%%".format(top.cnf * 100)
                signBoxes.text = "(%.2f, %.2f, %.2f, %.2f)".format(top.x1, top.y1, top.x2, top.y2)
                signImage.visibility = View.VISIBLE
            } else {
                signName.text = ""
                signDescription.text = ""
                signBoxes.text = ""
                signImage.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        isActive = false
        detector.close()
        cameraExecutor.shutdown()
        super.onDestroyView()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}