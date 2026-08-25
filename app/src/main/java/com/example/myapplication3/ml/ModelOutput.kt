package com.example.myapplication3.ml

data class ModelOutput(
    val detections: List<Detection>,

    val imageWidth: Int,
    val imageHeight: Int
)