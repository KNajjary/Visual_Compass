package com.example.myapplication3.sensor

data class CameraPose(

    // زاویه‌ها
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,

    // موقعیت جغرافیایی
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,

    // دقت GPS
    val accuracy: Float? = null

)