package com.example.trafficeye2.models
import com.example.trafficeye2.models.RoadSign
import com.example.trafficeye2.models.Box

data class SignWithBoxes(
    val sign: RoadSign,
    val boxes: List<Box>
)