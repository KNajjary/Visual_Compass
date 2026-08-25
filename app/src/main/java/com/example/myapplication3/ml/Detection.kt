package com.example.myapplication3.ml

data class Detection(
    val label: String,
    val confidence: Float,

    // مختصات Bounding Box
    // به صورت نرمال‌شده: 0.0 تا 1.0
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)