package com.example.trafficeye2

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import com.example.trafficeye2.models.Box
import com.surendramaran.yolov8tflite.Detector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), Detector.DetectorListener {

    // Widok podglądu kamery
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    // Elementy UI do wyświetlania informacji o znaku
    private lateinit var signLabel: TextView
    private lateinit var signName: TextView
    private lateinit var signDescription: TextView
    private lateinit var signBoxes: TextView
    private lateinit var signImage: ImageView
    private lateinit var returnButton: View

    // Detektor YOLOv8 i executor dla analizy obrazu
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService

    private var isActive = false  // Flaga do kontrolowania aktywności fragmentu

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001  // Kod zapytania o uprawnienie kamery
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Wczytaj layout XML fragmentu
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isActive = true

        // Powiązania widoków z layoutu
        previewView = view.findViewById(R.id.camera_preview)
        signLabel = view.findViewById(R.id.detectedSignText)
        signName = view.findViewById(R.id.signName)
        signDescription = view.findViewById(R.id.signDescription)
        signBoxes = view.findViewById(R.id.signBoxes)
        signImage = view.findViewById(R.id.signImage)
        returnButton = view.findViewById(R.id.returnButton)

        // Dynamiczne dodanie warstwy nakładki na obraz
        val rootLayout = view as ConstraintLayout
        overlayView = OverlayView(requireContext(), null)
        overlayView.id = View.generateViewId()
        rootLayout.addView(overlayView, ConstraintLayout.LayoutParams(0, 0))

        // Ustawienie położenia OverlayView względem PreviewView
        val constraints = ConstraintSet()
        constraints.clone(rootLayout)
        constraints.connect(overlayView.id, ConstraintSet.TOP, R.id.camera_preview, ConstraintSet.TOP)
        constraints.connect(overlayView.id, ConstraintSet.BOTTOM, R.id.camera_preview, ConstraintSet.BOTTOM)
        constraints.connect(overlayView.id, ConstraintSet.START, R.id.camera_preview, ConstraintSet.START)
        constraints.connect(overlayView.id, ConstraintSet.END, R.id.camera_preview, ConstraintSet.END)
        constraints.applyTo(rootLayout)

        // Przycisk powrotu do menu
        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        // Inicjalizacja detektora i wątku wykonawczego
        detector = Detector(requireContext(), this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Sprawdzenie i żądanie uprawnień do kamery
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // Jeżeli brak uprawnień, żądaj ich
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Po otrzymaniu wyniku żądania uprawnień
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Brak uprawnień do kamery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            if (!isAdded || requireActivity().isFinishing) return@addListener

            // Konfiguracja podglądu
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Konfiguracja analizatora obrazu
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 640))  // Rozdzielczość inputu dla modelu
                .build().also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!isActive || !isAdded) {
                            image.close()
                            return@setAnalyzer
                        }

                        val bitmap = image.toBitmap()
                        bitmap?.let { detector.detect(it) }
                        image.close()
                    }
                }

            // Użycie tylnej kamery
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Brak wykrycia — wyczyść interfejs
    override fun onEmptyDetect() {
        if (!isActive || !isAdded) return
        activity?.runOnUiThread {
            signLabel.text = "Brak wykrycia"
            signName.text = ""
            signDescription.text = ""
            signBoxes.text = ""
            signImage.visibility = View.GONE
            overlayView.setResults(emptyList())
        }
    }

    // Obsługa wykrycia znaku
    override fun onDetect(boundingBoxes: List<Box>, inferenceTime: Long) {
        if (!isActive || !isAdded) return
        activity?.runOnUiThread {
            // Filtruj tylko sensowne wykrycia (obszar > 40x40 i mieści się w 640x640)
            val bestBox = boundingBoxes
                .filter {
                    val area = (it.x2 - it.x1) * (it.y2 - it.y1)
                    area >= 1600 && it.x1 >= 5 && it.y1 >= 5 && it.x2 <= 635 && it.y2 <= 635
                }
                .maxByOrNull {
                    val area = (it.x2 - it.x1) * (it.y2 - it.y1)
                    area * it.confidence
                }

            // Jeśli znaleziono najlepszy box — aktualizuj UI
            if (bestBox != null) {
                overlayView.setResults(listOf(bestBox))
                signLabel.text = bestBox.class_id
                signName.text = bestBox.class_id
                signDescription.text = "Wykryto z pewnością %.2f%%".format(bestBox.confidence * 100)
                signBoxes.text = "(%d, %d, %d, %d)".format(bestBox.x1, bestBox.y1, bestBox.x2, bestBox.y2)
                signImage.visibility = View.VISIBLE
            } else {
                overlayView.setResults(emptyList())
            }
        }
    }

    // Czyszczenie wątku po zamknięciu widoku
    override fun onDestroyView() {
        isActive = false
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    // Konwersja ImageProxy (YUV) do bitmapy (JPEG)
    @androidx.camera.core.ExperimentalGetImage
    private fun ImageProxy.toBitmap(): Bitmap? {
        val image = this.image ?: return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21, android.graphics.ImageFormat.NV21, this.width, this.height, null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, this.width, this.height), 100, out
        )
        val yuv = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
    }
}
