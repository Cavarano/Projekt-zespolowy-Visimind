package com.example.trafficeye2.models

data class Box(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val class_id: String,
    val confidence: Float,
    val timeDetected: Float? = null
)