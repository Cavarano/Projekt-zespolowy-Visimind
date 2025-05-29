package com.example.trafficeye2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class SettingsFragment : Fragment() {
    private lateinit var returnButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        returnButton = view.findViewById(R.id.returnButton)

        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }
        // Inflate the layout for this fragment
        return view

    }
}