package com.example.trafficeye2

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trafficeye2.adapters.SignAdapter
import com.example.trafficeye2.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface // Zastąpiono android.media.ExifInterface
import java.util.Locale // Dodano dla String.format

class UploadFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upload, container, false)

        val gson = Gson()
        val signsJson = arguments?.getString("signs") ?: "[]"
        val boxesJson = arguments?.getString("boxes") ?: "[]"
        val signs: List<RoadSign> = gson.fromJson(signsJson, object : TypeToken<List<RoadSign>>() {}.type)
        val boxes: List<Box> = gson.fromJson(boxesJson, object : TypeToken<List<Box>>() {}.type)

        val imageView = view.findViewById<ImageView>(R.id.imageView2)
        val returnButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.returnButton)
        val recyclerView = view.findViewById<RecyclerView>(R.id.signsRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = SignAdapter(groupBoxesBySign(signs, boxes))

        val imagePath = arguments?.getString("uploaded_image")
        val imageUrl = arguments?.getString("file_url") ?: ""

        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                try {
                    // Obsługa EXIF dla rotacji
                    val exif = ExifInterface(imagePath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val rotatedBitmap = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                        else -> bitmap
                    }
                    val bitmapWithBoxes = drawBoxesOnBitmap(rotatedBitmap, boxes, signs)
                    imageView.setImageBitmap(bitmapWithBoxes)
                } catch (e: Exception) {
                    Log.e("UploadFragment", "drawBoxesOnBitmap failed: ${e.message}")
                    imageView.setImageBitmap(bitmap) // fallback
                }
            } else {
                Log.e("UploadFragment", "Nieprawidłowy bitmap z pliku: $imagePath")
                Toast.makeText(requireContext(), "Nie udało się załadować obrazu", Toast.LENGTH_SHORT).show()
            }
        } else if (imageUrl.isNotEmpty()) {
            Thread {
                try {
                    val input = URL(imageUrl).openStream()
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        // Pobranie EXIF z oryginalnego pliku (jeśli dostępny lokalnie)
                        val exif = if (!imagePath.isNullOrEmpty()) ExifInterface(imagePath) else null
                        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
                        val rotatedBitmap = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                            else -> bitmap
                        }
                        activity?.runOnUiThread {
                            val bitmapWithBoxes = drawBoxesOnBitmap(rotatedBitmap, boxes, signs)
                            imageView.setImageBitmap(bitmapWithBoxes)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UploadFragment", "Błąd podczas pobierania obrazu: ${e.message}")
                }
            }.start()
        }

        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        return view
    }

    private fun groupBoxesBySign(signs: List<RoadSign>, boxes: List<Box>): List<SignWithBoxes> {
        return signs.map { sign ->
            val relatedBoxes = boxes.filter { it.class_id == sign.id }
            SignWithBoxes(sign, relatedBoxes)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun drawBoxesOnBitmap(original: Bitmap, boxes: List<Box>, signs: List<RoadSign>): Bitmap {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val imageWidth = original.width
        val imageHeight = original.height

        val colors = listOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.YELLOW,
            Color.rgb(255, 165, 0), Color.rgb(128, 0, 128), Color.rgb(0, 128, 128),
            Color.rgb(0, 100, 0), Color.rgb(255, 20, 147), Color.rgb(70, 130, 180),
            Color.rgb(255, 215, 0), Color.rgb(139, 0, 0), Color.rgb(85, 107, 47),
            Color.rgb(199, 21, 133), Color.rgb(25, 25, 112), Color.rgb(210, 105, 30),
            Color.rgb(255, 105, 180), Color.rgb(30, 144, 255)
        )

        val minDim = minOf(imageWidth, imageHeight).toFloat()
        val resolutionFactor = minDim / 800f

        boxes.forEachIndexed { index, box ->
            try {
                val x1 = box.x1.coerceIn(0, imageWidth - 1)
                val y1 = box.y1.coerceIn(0, imageHeight - 1)
                val x2 = box.x2.coerceIn(0, imageWidth - 1)
                val y2 = box.y2.coerceIn(0, imageHeight - 1)
                if (x1 >= x2 || y1 >= y2) return@forEachIndexed

                val color = colors[index % colors.size]

                val boxPaint = Paint().apply {
                    this.color = color
                    strokeWidth = (2f * resolutionFactor).coerceIn(2f, 6f)
                    style = Paint.Style.STROKE
                }

                canvas.drawRect(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), boxPaint)

                val boxHeight = (y2 - y1).toFloat()
                val rawTextSize = boxHeight * 0.25f

                val finalTextSize = when {
                    resolutionFactor < 0.8f -> rawTextSize.coerceIn(10f, 18f) * 0.9f
                    resolutionFactor < 1.2f -> rawTextSize.coerceIn(12f, 22f)
                    else -> rawTextSize.coerceIn(16f, 32f) * resolutionFactor.coerceAtMost(2.0f)
                }

                val textPaint = Paint().apply {
                    this.color = color
                    textSize = finalTextSize
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    isSubpixelText = true
                    typeface = Typeface.DEFAULT_BOLD
                    strokeWidth = 1f
                    isFakeBoldText = true
                }

                // Dostosowanie do class_id jako String
                val signLabel = signs.find { it.id == box.class_id }?.id ?: box.class_id
                val confidenceText = String.format(Locale.US, "%.1f", box.confidence * 100) + "%" // Dodano Locale.US

                val centerY = (y1 + y2) / 2f
                val textOffset = textPaint.textSize / 3

                canvas.drawText(
                    confidenceText,
                    x1.toFloat() - textPaint.measureText(confidenceText) - 8f,
                    centerY + textOffset,
                    textPaint
                )
                canvas.drawText(
                    signLabel,
                    x2.toFloat() + 8f,
                    centerY + textOffset,
                    textPaint
                )

            } catch (e: Exception) {
                Log.e("drawBoxesOnBitmap", "Error drawing box: ${e.message}", e)
            }
        }

        return mutableBitmap
    }
}