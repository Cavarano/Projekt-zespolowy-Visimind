package com.example.trafficeye2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tflite: Interpreter
    private lateinit var imageCapture: ImageCapture
    private lateinit var detectedSignText: TextView
    private lateinit var capturedImageView: ImageView
    private lateinit var returnButton: Button


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        val previewView = view.findViewById<PreviewView>(R.id.camera_preview)
        detectedSignText = view.findViewById(R.id.detectedSignText)
        capturedImageView = view.findViewById(R.id.capturedImageView)
        returnButton = view.findViewById(R.id.returnButton)

        detectedSignText.text = "Waiting for detection..."

        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera(previewView)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        tflite = loadModel()

        return view
    }

    private fun loadModel(): Interpreter {
        val modelFile = requireContext().assets.open("best_float32.tflite").readBytes()
        val buffer = ByteBuffer.allocateDirect(modelFile.size).apply {
            order(ByteOrder.nativeOrder())
            put(modelFile)
        }
        return Interpreter(buffer)
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(6) // 📌 Skrócenie czasu analizy do 0.5 sekundy
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { image ->
                detectTrafficSigns(image)
                image.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun detectTrafficSigns(image: ImageProxy) {
        val bitmap = convertImageProxyToBitmap(image) ?: return
        val detectedLabel = runModel(bitmap)

        if (detectedLabel != detectedSignText.text.toString()) {
            takePhoto()
            updateUI(bitmap, detectedLabel)
        }
    }

    private fun runModel(bitmap: Bitmap): String {
        try {
            val inputBuffer = preprocessBitmap(bitmap)
            val outputBuffer = ByteBuffer.allocateDirect(4 * 1).apply {
                order(ByteOrder.nativeOrder())
            }

            tflite.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            return if (outputBuffer.float > 0.5f) "Sign detected!" else "No sign detected"
        } catch (e: IllegalArgumentException) {
            println("ERROR: TensorFlow Lite input size mismatch! ${e.message}")
            return "Detection error"
        }
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val requiredSize = 300
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, requiredSize, requiredSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * requiredSize * requiredSize * 3).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(requiredSize * requiredSize)
        scaledBitmap.getPixels(intValues, 0, requiredSize, 0, 0, requiredSize, requiredSize)

        for (i in intValues) {
            // 📌 **Lepsza normalizacja kolorów**
            val r = ((i shr 16) and 0xFF) / 255.0f
            val g = ((i shr 8) and 0xFF) / 255.0f
            val b = (i and 0xFF) / 255.0f

            val enhancedR = r * 1.2f // Wzmocnienie czerwonego koloru (ważne dla znaków)
            val enhancedB = b * 0.8f // Zmniejszenie niebieskiego dla lepszej jakości

            byteBuffer.putFloat(enhancedR.coerceIn(0.0f, 1.0f))
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(enhancedB.coerceIn(0.0f, 1.0f))
        }

        return byteBuffer
    }

    private fun convertImageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        // 📌 Pobieramy kąt obrotu z `imageProxy` i używamy `Matrix`
        val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
        val matrix = Matrix().apply { postRotate(rotationDegrees) }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    private fun adjustImageOrientation(imagePath: String): Bitmap? {
        try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            val bitmap = BitmapFactory.decodeFile(imagePath)
            val matrix = Matrix()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }


    private fun updateUI(bitmap: Bitmap, detectedSign: String) {
        requireActivity().runOnUiThread {
            detectedSignText.text =
                detectedSign.ifEmpty { "No road sign detected" } // 📌 Domyślny komunikat, jeśli brak detekcji
        }
    }


    private fun sendImageToFastAPI(bitmap: Bitmap) {
        val url = "http://10.0.2.2:8000/detection/detect-signs/"

        // Konwersja bitmapy do bajtów
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "image.jpg",
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    detectedSignText.text = "Connection error"
                    capturedImageView.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body?.string()
                        val jsonObject = JSONObject(responseBody ?: "{}")

                        val signsArray = jsonObject.optJSONArray("signs") ?: JSONArray()

                        if (signsArray.length() > 0) {
                            val signObject = signsArray.getJSONObject(0)

                            val detectedSignId = signObject.optString("id", "No ID")
                            val detectedSignName = signObject.optString("name", "Unknown road sign")
                            val detectedSignDescription =
                                signObject.optString("description", "No description")
                            val detectedSignPhotoUrl = signObject.optString("photo_url", "")

                            requireActivity().runOnUiThread {
                                detectedSignText.text =
                                    "ID: $detectedSignId\n$detectedSignName\nOpis: $detectedSignDescription"

                                if (detectedSignPhotoUrl.isNotEmpty()) {
                                    Glide.with(requireContext())
                                        .load(detectedSignPhotoUrl)
                                        .into(capturedImageView)
                                    capturedImageView.visibility = View.VISIBLE
                                } else {
                                    capturedImageView.visibility = View.GONE
                                }
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                detectedSignText.text = "No road sign detected"
                                capturedImageView.visibility = View.GONE
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    requireActivity().runOnUiThread {
                        detectedSignText.text = "Server error"
                        capturedImageView.visibility = View.GONE
                    }
                }
            }
        })
    }


    private fun takePhoto() {
        if (!::imageCapture.isInitialized) {
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = convertImageProxyToBitmap(image) ?: return
                    image.close()

                    // 📌 Bezpośrednie wysyłanie obrazu do FastAPI (bez zapisu)
                    sendImageToFastAPI(bitmap)

                    // 📌 Aktualizacja UI
                    updateUI(bitmap, "Processing...")
                }

                override fun onError(exc: ImageCaptureException) {
                    exc.printStackTrace()
                }
            })
    }
}

