package com.example.myapplication3.camera

import androidx.camera.core.ImageProxy

class FrameChangeDetector {

    private var previousSignature: Long? = null

    fun hasChanged(image: ImageProxy): Boolean {

        val currentSignature = calculateSignature(image)

        val previous = previousSignature

        previousSignature = currentSignature

        if (previous == null) {
            return true
        }

        return currentSignature != previous
    }

    private fun calculateSignature(image: ImageProxy): Long {

        val plane = image.planes[0]

        val buffer = plane.buffer

        var signature = 0L

        val step = maxOf(buffer.remaining() / 100, 1)

        var index = buffer.position()

        while (index < buffer.limit()) {

            signature = signature * 31 + buffer.get(index)

            index += step
        }

        return signature
    }
}