package com.example.trafficeye2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trafficeye2.R
import com.example.trafficeye2.models.RoadSign
import com.bumptech.glide.Glide
import com.example.trafficeye2.models.Box
import com.example.trafficeye2.models.SignWithBoxes

class SignAdapter(private val signsWithBoxes: List<SignWithBoxes>) :
    RecyclerView.Adapter<SignAdapter.SignViewHolder>() {

    class SignViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val signName: TextView = itemView.findViewById(R.id.signName)
        val signDescription: TextView = itemView.findViewById(R.id.signDescription)
        val signImage: ImageView = itemView.findViewById(R.id.signImage)
        val signBoxes: TextView = itemView.findViewById(R.id.signBoxes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sign, parent, false)
        return SignViewHolder(view)
    }

    override fun onBindViewHolder(holder: SignViewHolder, position: Int) {
        val item = signsWithBoxes[position]
        val sign = item.sign
        val boxes = item.boxes

        holder.signName.text = sign.name
        holder.signDescription.text = sign.description
        holder.signBoxes.text = boxes.joinToString("\n") { box ->
            "x1: ${box.x1}, y1: ${box.y1}, x2: ${box.x2}, y2: ${box.y2}, class_id: ${box.class_id}, confidence: ${box.confidence}"
        }

        Glide.with(holder.itemView)
            .load(sign.photo_url)
            .into(holder.signImage)
    }

    override fun getItemCount(): Int = signsWithBoxes.size
}

//class SignAdapter(private val signs: List<RoadSign>, private val boxes: List<Box>) :
//    RecyclerView.Adapter<SignAdapter.SignViewHolder>() {
//
//    class SignViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val signName: TextView = itemView.findViewById(R.id.signName)
//        val signDescription: TextView = itemView.findViewById(R.id.signDescription)
//        val signImage: ImageView = itemView.findViewById(R.id.signImage)
//        val signBox: TextView = itemView.findViewById(R.id.signBoxes)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_sign, parent, false)
//        return SignViewHolder(view)
//    }
//
//
//    override fun onBindViewHolder(holder: SignViewHolder, position: Int) {
//        val sign = signs[position]
//        val box = boxes[position]
//        holder.signName.text = sign.name
//        holder.signDescription.text = sign.description
//        holder.signBox.text = "x1: ${box.x1}, y1: ${box.y1}, x2: ${box.x2}, y2: ${box.y2}, class_id: ${box.class_id}"
//
////        val fullImageUrl = "http://10.0.2.2:8000" + sign.photo_url
//        val fullImageUrl = sign.photo_url
//
//
//
//
//
//        Glide.with(holder.itemView)
//            .load(fullImageUrl)
////            .placeholder(R.drawable.placeholder) // якщо хочеш
////            .error(R.drawable.error) // якщо картинка не завантажилась
//            .into(holder.signImage)
//    }
//
//    override fun getItemCount(): Int = signs.size
//}