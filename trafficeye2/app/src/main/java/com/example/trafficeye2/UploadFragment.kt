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
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trafficeye2.models.RoadSign
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.trafficeye2.adapters.SignAdapter
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import com.example.trafficeye2.models.Box
import com.example.trafficeye2.models.SignWithBoxes
import java.net.URL

class UploadFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upload, container, false)

        val signsJson = arguments?.getString("signs") ?: "[]"
        val gson = Gson()
        val type = object : TypeToken<List<RoadSign>>() {}.type
        val signs: List<RoadSign> = gson.fromJson(signsJson, type)

        val boxesJson = arguments?.getString("boxes") ?: "[]"
        val gson_box = Gson()
        val type_box = object : TypeToken<List<Box>>() {}.type
        val boxes: List<Box> = gson_box.fromJson(boxesJson, type_box)

        val cardView = view.findViewById<CardView>(R.id.cardView)
        val fullDescription = view.findViewById<TextView>(R.id.fullDescription)
        val returnButton = view.findViewById<Button>(R.id.returnButton)

        val recyclerView = view.findViewById<RecyclerView>(R.id.signsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val groupedSigns = groupBoxesBySign(signs, boxes)
        recyclerView.adapter = SignAdapter(groupedSigns)
//        recyclerView.adapter = SignAdapter(signs, boxes)

        val totalBoxes = arguments?.getInt("total_boxes") ?: 0
        val imageView = view.findViewById<ImageView>(R.id.imageView2)

        cardView.setOnClickListener {
            if (fullDescription.visibility == View.VISIBLE) {
                fullDescription.visibility = View.GONE
            } else {
                fullDescription.visibility = View.VISIBLE
            }
        }

        val imagePath = arguments?.getString("uploaded_image")
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(bitmap)
        } else {
            val imageUrl = arguments?.getString("image_url") ?: ""
            if (imageUrl.isNotEmpty()) {
                Thread {
                    try {
                        val input = URL(imageUrl).openStream()
                        val drawable = Drawable.createFromStream(input, "src")
                        activity?.runOnUiThread {
                            imageView.setImageDrawable(drawable)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }

        returnButton.setOnClickListener {
            (activity as? MainActivity)?.returnToMainMenu()
        }

        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val bitmapWithBoxes = drawBoxesOnBitmap(bitmap, boxes)
            imageView.setImageBitmap(bitmapWithBoxes)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = SignAdapter(groupBoxesBySign(signs, boxes))

        return view
    }

    fun groupBoxesBySign(signs: List<RoadSign>, boxes: List<Box>): List<SignWithBoxes> {
        return signs.map { sign ->
            val relatedBoxes = boxes.filter { it.class_id == sign.id }
            SignWithBoxes(sign, relatedBoxes)
        }
    }

    fun drawBoxesOnBitmap(
        original: Bitmap,
        boxes: List<Box>
    ): Bitmap {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutableBitmap)

        val boxPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            strokeWidth = 5f
            style = android.graphics.Paint.Style.STROKE
        }

        boxes.forEach { box ->
            canvas.drawRect(
                box.x1.toFloat(),
                box.y1.toFloat(),
                box.x2.toFloat(),
                box.y2.toFloat(),
                boxPaint
            )
        }

        return mutableBitmap
    }

}
