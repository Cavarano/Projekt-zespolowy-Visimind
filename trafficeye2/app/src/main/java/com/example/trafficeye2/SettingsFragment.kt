package com.example.trafficeye2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.content.Context
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import androidx.appcompat.app.AppCompatDelegate


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

        // Theme dropdown logic
        val themeDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.themeDropdown)
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        val options = listOf("Light", "Dark", "System default")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        themeDropdown.setAdapter(adapter)

        val savedMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        themeDropdown.setText(
            when (savedMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> "Light"
                AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
                else -> "System default"
            }, false
        )

        themeDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedMode = when (position) {
                0 -> AppCompatDelegate.MODE_NIGHT_NO
                1 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            prefs.edit().putInt("theme_mode", selectedMode).apply()
            AppCompatDelegate.setDefaultNightMode(selectedMode)
        }
        // Inflate the layout for this fragment
        return view

    }
}