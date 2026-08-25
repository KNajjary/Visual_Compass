package com.example.myapplication3.overlay

import com.example.myapplication3.sensor.CameraPose
data class DebugInfo(
    val frameNumber: Long = 0,
    val fps: Float? = null,
    val pose: CameraPose
)