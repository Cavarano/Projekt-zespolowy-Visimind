package com.example.trafficeye2

import androidx.fragment.app.Fragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.trafficeye2.R

class MainActivity : AppCompatActivity() {

    private lateinit var getStartedButton: Button
    private lateinit var uploadButton: Button
    private lateinit var cameraOnButton: Button
    private lateinit var titleText: TextView
    private lateinit var fragmentContainer: View

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getStartedButton = findViewById(R.id.getStartedButton)
        uploadButton = findViewById(R.id.uploadButton)
        cameraOnButton = findViewById(R.id.cameraOnButton)
        titleText = findViewById(R.id.titleText)
        fragmentContainer = findViewById(R.id.fragment_container)

        // Hide fragment container at the start
        fragmentContainer.visibility = View.GONE

        getStartedButton.setOnClickListener {
            getStartedButton.visibility = View.GONE
            titleText.visibility = View.GONE

            uploadButton.visibility = View.VISIBLE
            cameraOnButton.visibility = View.VISIBLE
        }

        uploadButton.setOnClickListener {
            showFragment(UploadFragment())
        }

        cameraOnButton.setOnClickListener {
            showFragment(CameraFragment())
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        fragmentContainer.visibility = View.VISIBLE // make fragment visible
        // Hide the option buttons
        uploadButton.visibility = View.GONE
        cameraOnButton.visibility = View.GONE
    }

    fun returnToMainMenu() {
        // Hide the fragment container
        fragmentContainer.visibility = View.GONE

        // Show the option buttons again
        uploadButton.visibility = View.VISIBLE
        cameraOnButton.visibility = View.VISIBLE
    }


}
