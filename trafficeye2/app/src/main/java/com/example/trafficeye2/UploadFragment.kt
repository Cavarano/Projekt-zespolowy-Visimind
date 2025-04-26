package com.example.trafficeye2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class UploadFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upload, container, false)

        val cardView = view.findViewById<CardView>(R.id.cardView)
        val fullDescription = view.findViewById<TextView>(R.id.fullDescription)
        val returnButton = view.findViewById<Button>(R.id.returnButton) // Add this

        cardView.setOnClickListener {
            if (fullDescription.visibility == View.VISIBLE) {
                fullDescription.visibility = View.GONE
            } else {
                fullDescription.visibility = View.VISIBLE
            }
        }

        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        return view
    }
}
