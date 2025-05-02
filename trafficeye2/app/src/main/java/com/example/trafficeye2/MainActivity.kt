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
        animateTitleEntrance()
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

            titleText.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    titleText.visibility = View.GONE
                }
                .start()

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


    private fun animateTitleEntrance() {
        titleText.translationY = -500f // Start above the screen
        val animator = ObjectAnimator.ofFloat(titleText, "translationY", 0f)
        animator.duration = 1000 // 1 second
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }
}
