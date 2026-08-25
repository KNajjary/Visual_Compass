package com.example.myapplication3.ml

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class ImagePreprocessor {

    companion object {
        private const val IMAGE_SIZE = 300
        private const val CHANNELS = 3
    }

    fun preprocess(image: ImageProxy): ByteBuffer {

        val width = image.width
        val height = image.height

        val planeY = image.planes[0]
        val planeU = image.planes[1]
        val planeV = image.planes[2]

        val yBuffer = planeY.buffer
        val uBuffer = planeU.buffer
        val vBuffer = planeV.buffer

        val yRowStride = planeY.rowStride
        val uRowStride = planeU.rowStride
        val vRowStride = planeV.rowStride

        val uPixelStride = planeU.pixelStride
        val vPixelStride = planeV.pixelStride

        /*
         * SSD MobileNet V1 quantized model:
         *
         * input:
         * [1, 300, 300, 3]
         *
         * UINT8
         */

        val output = ByteBuffer
            .allocateDirect(
                IMAGE_SIZE *
                        IMAGE_SIZE *
                        CHANNELS
            )
            .order(ByteOrder.nativeOrder())

        val scale = max(
            width.toFloat() / IMAGE_SIZE,
            height.toFloat() / IMAGE_SIZE
        )

        val scaledWidth = width / scale
        val scaledHeight = height / scale

        val cropX =
            (IMAGE_SIZE - scaledWidth) / 2f

        val cropY =
            (IMAGE_SIZE - scaledHeight) / 2f

        for (outY in 0 until IMAGE_SIZE) {

            for (outX in 0 until IMAGE_SIZE) {

                val sourceX =
                    ((outX - cropX) * scale).toInt()

                val sourceY =
                    ((outY - cropY) * scale).toInt()

                val x = min(
                    max(sourceX, 0),
                    width - 1
                )

                val y = min(
                    max(sourceY, 0),
                    height - 1
                )

                val yIndex =
                    y * yRowStride + x

                val uvX = x / 2
                val uvY = y / 2

                val uIndex =
                    uvY * uRowStride +
                            uvX * uPixelStride

                val vIndex =
                    uvY * vRowStride +
                            uvX * vPixelStride

                val yValue =
                    yBuffer.get(yIndex).toInt() and 0xFF

                val uValue =
                    uBuffer.get(uIndex).toInt() and 0xFF

                val vValue =
                    vBuffer.get(vIndex).toInt() and 0xFF

                val r =
                    (
                            yValue +
                                    1.402f *
                                    (vValue - 128)
                            ).coerceIn(0f, 255f)

                val g =
                    (
                            yValue -
                                    0.344136f *
                                    (uValue - 128) -
                                    0.714136f *
                                    (vValue - 128)
                            ).coerceIn(0f, 255f)

                val b =
                    (
                            yValue +
                                    1.772f *
                                    (uValue - 128)
                            ).coerceIn(0f, 255f)

                output.put(r.toInt().toByte())
                output.put(g.toInt().toByte())
                output.put(b.toInt().toByte())
            }
        }

        output.rewind()

        return output
    }
}