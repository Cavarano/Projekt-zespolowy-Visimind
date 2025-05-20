package com.example.trafficeye2

import androidx.fragment.app.Fragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.trafficeye2.R
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.trafficeye2.models.Box
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.trafficeye2.models.RoadSign
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageButton: ImageButton
    private lateinit var imageViewIcon: ImageView
    private lateinit var getStartedButton: Button
    private lateinit var uploadButton: Button
    private lateinit var cameraOnButton: Button
    private lateinit var titleText: TextView
    private lateinit var fragmentContainer: View
    private val PICK_IMAGE_REQUEST = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageButton   = findViewById(R.id.imageButton)
        imageViewIcon   = findViewById(R.id.imageViewIcon)
        getStartedButton = findViewById(R.id.getStartedButton)
        uploadButton = findViewById(R.id.uploadButton)
        cameraOnButton = findViewById(R.id.cameraOnButton)
        titleText = findViewById(R.id.titleText)
        fragmentContainer = findViewById(R.id.fragment_container)
        animateXEntrance(titleText)
        animateXEntrance(imageViewIcon)
        // Hide fragment container at the start
        fragmentContainer.visibility = View.GONE

        getStartedButton.setOnClickListener {
            getStartedButton.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    getStartedButton.visibility = View.GONE
                }
                .start()
            animateXSlideUp(titleText)
            animateXSlideUp(imageViewIcon)

            /*
            titleText.animate()
                //.alpha(0f)
                .setDuration(500)
                .withEndAction {
                    //titleText.visibility = View.GONE
                }
                .start()
             */
            imageButton.alpha = 0f
            imageButton.visibility = View.VISIBLE
            uploadButton.alpha = 0f
            uploadButton.visibility = View.VISIBLE
            cameraOnButton.alpha = 0f
            cameraOnButton.visibility = View.VISIBLE
            imageButton.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
            uploadButton.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
            cameraOnButton.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

        }
        uploadButton.setOnClickListener {
            showLoadingIndicator()
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

//        uploadButton.setOnClickListener {
//            showFragment(UploadFragment())
//        }

        cameraOnButton.setOnClickListener {
            showFragment(CameraFragment())
        }

        imageButton.setOnClickListener {
            showFragment(SettingsFragment())
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Ensure fragmentContainer is visible before animation
        fragmentContainer.visibility = View.VISIBLE

        // Start off-screen at bottom
        fragmentContainer.post {
            fragmentContainer.translationY = fragmentContainer.height.toFloat()
            fragmentContainer.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }


        uploadButton.visibility = View.GONE
        cameraOnButton.visibility = View.GONE

    }


    fun returnToMainMenu() {
        hideLoadingIndicator()
        fragmentContainer.animate()
            .translationY(fragmentContainer.height.toFloat()) // Slide down out
            .setDuration(300)
            .withEndAction {
                fragmentContainer.visibility = View.GONE
                uploadButton.visibility = View.VISIBLE
                cameraOnButton.visibility = View.VISIBLE
            }
            .start()
    }

    private fun animateXEntrance(view: View) {
        view.translationY = -1000f
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f)
        animator.duration = 1200
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    private fun animateXSlideUp(view: View) {
        view.translationY = 0f
        val animator = ObjectAnimator.ofFloat(view, "translationY", -400f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data

            Log.d("MainActivity", "onActivityResult called with imageUri: $imageUri")

            // Wysyłanie zdjęć na serwer
            uploadImageToServer(imageUri)
        }
    }

    // Pokaż wskaźnik
    fun showLoadingIndicator() {
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
    }

    // Ukryj wskaźnik
    fun hideLoadingIndicator() {
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
    }

    private fun saveImageLocally(imageUri: Uri, context: Context): String? {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val fileName = "uploaded_image.jpg"
        val file = File(context.filesDir, fileName)  // Zapisz plik w pamięci wewnętrznej

        try {
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            return file.absolutePath  // Zwraca ścieżkę do zapisanego pliku
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun uploadImageToServer(imageUri: Uri?) {
        if (imageUri == null) return

        val savedImagePath = saveImageLocally(imageUri, this)

        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()

        if (bytes != null) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "upload.jpg",
                    RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
                )
                .build()

            val request = Request.Builder()
                .url("http://10.0.2.2:8000/detection/detect-signs/")
//                .url("http://ТВОЯ_АДРЕСА:8000/route_яка_приймає_фото")  // <- wstaw tutaj adres FastAPI
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)  // Limit czasu połączenia
                .writeTimeout(45, TimeUnit.SECONDS)    // Przerwa na nagrywanie
                .readTimeout(45, TimeUnit.SECONDS)     // Czas na czytanie
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()

                    // Jeśli wystąpi błąd, zatrzymaj wskaźnik
                    runOnUiThread {
                        hideLoadingIndicator()
                        // Możesz wyświetlić komunikat o błędzie
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val responseBody = response.body?.string()
                            Log.d("MainActivity", "onResponse called")
                            Log.d("UploadFragment", "responseBody: $responseBody")

                            // Parsowanie JSON (możesz użyć org.json lub Gson, zależy to od tego, czy chcesz używać Gson)
                            val jsonObject = JSONObject(responseBody)

                            val signsArray = jsonObject.getJSONArray("signs")
                            val signsList = mutableListOf<RoadSign>()
                            for (i in 0 until signsArray.length()) {
                                val signObject = signsArray.getJSONObject(i)
                                val roadSign = RoadSign(
                                    id = signObject.getString("id"),
                                    name = signObject.getString("name"),
                                    description = signObject.getString("description"),
                                    photo_url = signObject.getString("photo_url")
                                )
                                signsList.add(roadSign)
                            }

                            val totalBoxes = jsonObject.getInt("total_boxes")
                            val imageUrl = jsonObject.getString("image_url")
                            val boxesArray = jsonObject.getJSONArray("boxes")
                            val boxesList = mutableListOf<Box>()
                            for (i in 0 until boxesArray.length()) {
                                val boxObject = boxesArray.getJSONObject(i)
                                val box = Box(
                                    x1 = boxObject.getInt("x1"),
                                    y1 = boxObject.getInt("y1"),
                                    x2 = boxObject.getInt("x2"),
                                    y2 = boxObject.getInt("y2"),
                                    class_id = boxObject.getString("class_id")

                                )
                                boxesList.add(box)
                            }

                            // Przekazywanie danych do UploadFragment przez Bundle
                            val fragment = UploadFragment()

                            val gson = Gson()
                            val signsJson = gson.toJson(signsList)
                            val boxesJson = gson.toJson(boxesList)

                            val bundle = Bundle()
                            bundle.putString("signs", signsJson)
                            bundle.putInt("total_boxes", totalBoxes)
                            bundle.putString("image_url", imageUrl)
                            bundle.putString("uploaded_image", savedImagePath)
                            bundle.putString("boxes", boxesJson)

                            fragment.arguments = bundle

                            runOnUiThread {
                                showFragment(fragment)
                            }
                        } catch (e: Exception) {
                            Log.e("UploadFragment", "JSON parsing error: ${e.message}")
                        }
                    }  else {
                        Log.e("MainActivity", "Wasie nie succesfull")
                    }
                }
            })
        }
    }
}
