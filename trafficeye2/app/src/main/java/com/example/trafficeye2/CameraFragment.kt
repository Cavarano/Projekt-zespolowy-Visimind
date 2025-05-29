package com.example.trafficeye2

import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
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

    // Widoki interfejsu użytkownika
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var signLabel: TextView
    private lateinit var signName: TextView
    private lateinit var signDescription: TextView
    private lateinit var signBoxes: TextView
    private lateinit var signImage: ImageView
    private lateinit var returnButton: View

    // Inicjalizacja detektora i wątku kamery
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService

    // Tworzenie widoku fragmentu
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    // Konfiguracja widoków po utworzeniu
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pobranie referencji do komponentów UI
        previewView = view.findViewById(R.id.camera_preview)
        signLabel = view.findViewById(R.id.detectedSignText)
        signName = view.findViewById(R.id.signName)
        signDescription = view.findViewById(R.id.signDescription)
        signBoxes = view.findViewById(R.id.signBoxes)
        signImage = view.findViewById(R.id.signImage)
        returnButton = view.findViewById(R.id.returnButton)

        // Dynamiczne dodanie warstwy z ramkami detekcji nad podgląd kamery
        val rootLayout = view as ConstraintLayout
        overlayView = OverlayView(requireContext(), null)
        overlayView.id = View.generateViewId()
        rootLayout.addView(overlayView, ConstraintLayout.LayoutParams(0, 0))

        // Ustawienie OverlayView nad PreviewView w układzie ConstraintLayout
        val constraints = ConstraintSet()
        constraints.clone(rootLayout)
        constraints.connect(overlayView.id, ConstraintSet.TOP, R.id.camera_preview, ConstraintSet.TOP)
        constraints.connect(overlayView.id, ConstraintSet.BOTTOM, R.id.camera_preview, ConstraintSet.BOTTOM)
        constraints.connect(overlayView.id, ConstraintSet.START, R.id.camera_preview, ConstraintSet.START)
        constraints.connect(overlayView.id, ConstraintSet.END, R.id.camera_preview, ConstraintSet.END)
        constraints.applyTo(rootLayout)

        // Obsługa powrotu do ekranu głównego przez metodę MainActivity
        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        // Inicjalizacja detektora YOLOv8 i wątku analizującego obraz
        detector = Detector(requireContext(), this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Uruchomienie podglądu z kamery i analizatora
        startCamera()
    }

    // Konfiguracja kamery i analizatora obrazu
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Konfiguracja podglądu
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Konfiguracja analizatora obrazu
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 640)) // Dopasowanie do modelu
                .build().also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        val bitmap = image.toBitmap()
                        bitmap?.let { detector.detect(it) }
                        image.close()
                    }
                }

            // Ustawienie tylnej kamery
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Połączenie komponentów z cyklem życia
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Wywoływana, gdy brak wykrytych znaków
    override fun onEmptyDetect() {
        requireActivity().runOnUiThread {
            signLabel.text = "Brak wykrycia"
            signName.text = ""
            signDescription.text = ""
            signBoxes.text = ""
            signImage.visibility = View.GONE
            overlayView.setResults(emptyList()) // Usunięcie ramek
        }
    }

    // Wywoływana, gdy wykryto znaki — pokazanie najlepszego wyniku
    override fun onDetect(boundingBoxes: List<Box>, inferenceTime: Long) {
        requireActivity().runOnUiThread {
            // Filtracja ramek: zbyt małe i wychodzące poza kadr są pomijane
            val bestBox = boundingBoxes
                .filter {
                    val area = (it.x2 - it.x1) * (it.y2 - it.y1)
                    area >= 40 * 40 && it.x1 >= 5 && it.y1 >= 5 && it.x2 <= 635 && it.y2 <= 635
                }
                .maxByOrNull {
                    val area = (it.x2 - it.x1) * (it.y2 - it.y1)
                    area * it.confidence // priorytet: duży + pewny
                }

            if (bestBox != null) {
                overlayView.setResults(listOf(bestBox)) // Narysuj ramkę
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

    // Zatrzymanie wątku kamery przy zamykaniu fragmentu
    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    // Konwersja ImageProxy (z kamery) do Bitmap dla TFLite
    @androidx.camera.core.ExperimentalGetImage
    private fun ImageProxy.toBitmap(): Bitmap? {
        val image = this.image ?: return null

        // Bufory YUV
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        // Złożenie do formatu NV21
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Konwersja NV21 → JPEG → Bitmap
        val yuvImage = android.graphics.YuvImage(
            nv21, android.graphics.ImageFormat.NV21, this.width, this.height, null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, this.width, this.height), 100, out)
        val yuv = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
    }
}
