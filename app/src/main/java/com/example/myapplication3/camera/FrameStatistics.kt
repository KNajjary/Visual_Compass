package com.example.myapplication3.camera

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FrameStatistics {

    private var lastTimestamp: Long = 0L

    private val _frameNumber = MutableStateFlow(0L)
    val frameNumber: StateFlow<Long> = _frameNumber.asStateFlow()

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    fun onFrame() {

        _frameNumber.value++

        val now = System.nanoTime()

        if (lastTimestamp != 0L) {

            val elapsedSeconds =
                (now - lastTimestamp) / 1_000_000_000f

            if (elapsedSeconds > 0f) {
                _fps.value = 1f / elapsedSeconds
            }
        }

        lastTimestamp = now
    }
}