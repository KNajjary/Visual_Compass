package com.example.myapplication3.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import android.util.Log

class ImagePreprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int
) {

    data class PreprocessResult(
        val buffer: ByteBuffer,

        val originalWidth: Int,
        val originalHeight: Int,

        val rotationDegrees: Int,

        val rotatedWidth: Int,
        val rotatedHeight: Int,

        val cropLeft: Float,
        val cropTop: Float,
        val cropSize: Float,

        val modelWidth: Int,
        val modelHeight: Int
    )

    fun preprocess(
        image: ImageProxy
    ): PreprocessResult {

        val originalWidth =
            image.width

        val originalHeight =
            image.height

        val rotationDegrees =
            image.imageInfo.rotationDegrees

        /*
         * ------------------------------------------
         * 1. ImageProxy -> Bitmap
         * ------------------------------------------
         */
        val bitmap =
            imageProxyToBitmap(image)

        /*
         * ------------------------------------------
         * 2. Correct camera rotation
         * ------------------------------------------
         */
        val rotatedBitmap =
            rotateBitmap(
                bitmap,
                rotationDegrees
            )

        /*
         * ------------------------------------------
         * 3. Center crop
         * ------------------------------------------
         *
         * We crop the largest possible square.
         *
         * Example:
         *
         * 1920 x 1080
         *
         * crop:
         *
         * x = 420
         * y = 0
         * size = 1080
         */
        val cropSize =
            min(
                rotatedBitmap.width,
                rotatedBitmap.height
            ).toFloat()

        val cropLeft =
            (
                    rotatedBitmap.width -
                            cropSize
                    ) / 2f

        val cropTop =
            (
                    rotatedBitmap.height -
                            cropSize
                    ) / 2f

        val croppedBitmap =
            Bitmap.createBitmap(
                rotatedBitmap,
                cropLeft.toInt(),
                cropTop.toInt(),
                cropSize.toInt(),
                cropSize.toInt()
            )

        /*
         * ------------------------------------------
         * 4. Resize to model input
         * ------------------------------------------
         */
        val resizedBitmap =
            Bitmap.createScaledBitmap(
                croppedBitmap,
                inputWidth,
                inputHeight,
                true
            )
        Log.d(
            "ImagePreprocessor",
            "Final bitmap = ${resizedBitmap.width}x${resizedBitmap.height}"
        )
        /*
         * ------------------------------------------
         * 5. RGB UINT8 tensor
         * ------------------------------------------
         */
        val inputBuffer =
            bitmapToRgbBuffer(
                resizedBitmap
            )

        /*
         * ------------------------------------------
         * Cleanup
         * ------------------------------------------
         */
        if (bitmap !== rotatedBitmap) {
            bitmap.recycle()
        }

        if (rotatedBitmap !== croppedBitmap) {
            rotatedBitmap.recycle()
        }

        if (croppedBitmap !== resizedBitmap) {
            croppedBitmap.recycle()
        }

        resizedBitmap.recycle()

        val rotatedWidth =
            if (
                rotationDegrees == 90 ||
                rotationDegrees == 270
            ) {
                originalHeight
            } else {
                originalWidth
            }

        val rotatedHeight =
            if (
                rotationDegrees == 90 ||
                rotationDegrees == 270
            ) {
                originalWidth
            } else {
                originalHeight
            }

        return PreprocessResult(
            buffer = inputBuffer,

            originalWidth = originalWidth,
            originalHeight = originalHeight,

            rotationDegrees = rotationDegrees,

            rotatedWidth = rotatedWidth,
            rotatedHeight = rotatedHeight,

            cropLeft = cropLeft,
            cropTop = cropTop,
            cropSize = cropSize,

            modelWidth = inputWidth,
            modelHeight = inputHeight
        )
    }

    /*
     * ==========================================
     * ImageProxy -> Bitmap
     * ==========================================
     */
    private fun imageProxyToBitmap(
        image: ImageProxy
    ): Bitmap {

        require(
            image.format ==
                    ImageFormat.YUV_420_888
        ) {
            "Expected YUV_420_888 but got ${image.format}"
        }

        val planes =
            image.planes

        val yPlane =
            planes[0]

        val uPlane =
            planes[1]

        val vPlane =
            planes[2]

        val yBuffer =
            yPlane.buffer

        val uBuffer =
            uPlane.buffer

        val vBuffer =
            vPlane.buffer

        val yBytes =
            ByteArray(
                yBuffer.remaining()
            )

        yBuffer.get(yBytes)

        val uBytes =
            ByteArray(
                uBuffer.remaining()
            )

        uBuffer.get(uBytes)

        val vBytes =
            ByteArray(
                vBuffer.remaining()
            )

        vBuffer.get(vBytes)

        val width =
            image.width

        val height =
            image.height

        /*
         * Build NV21 while respecting pixelStride.
         */
        val nv21 =
            ByteArray(
                width * height * 3 / 2
            )

        /*
         * Y
         */
        copyPlane(
            source = yBytes,
            destination = nv21,
            width = width,
            height = height,
            rowStride = yPlane.rowStride,
            pixelStride = yPlane.pixelStride,
            offset = 0
        )

        /*
         * VU
         */
        val chromaWidth =
            width / 2

        val chromaHeight =
            height / 2

        var outputOffset =
            width * height

        for (row in 0 until chromaHeight) {

            val rowStart =
                row *
                        vPlane.rowStride

            for (col in 0 until chromaWidth) {

                val pixelOffset =
                    rowStart +
                            col *
                            vPlane.pixelStride

                nv21[outputOffset++] =
                    vBytes[pixelOffset]

                nv21[outputOffset++] =
                    uBytes[
                        row *
                                uPlane.rowStride +
                                col *
                                uPlane.pixelStride
                    ]
            }
        }

        val yuvImage =
            YuvImage(
                nv21,
                ImageFormat.NV21,
                width,
                height,
                null
            )

        val output =
            ByteArrayOutputStream()

        val success =
            yuvImage.compressToJpeg(
                Rect(
                    0,
                    0,
                    width,
                    height
                ),
                100,
                output
            )

        check(success) {
            "Failed to convert YUV to JPEG"
        }

        return BitmapFactory.decodeByteArray(
            output.toByteArray(),
            0,
            output.size()
        )
            ?: throw IllegalStateException(
                "Failed to decode camera JPEG"
            )
    }

    private fun copyPlane(
        source: ByteArray,
        destination: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        offset: Int
    ) {

        var destinationIndex =
            offset

        for (row in 0 until height) {

            val rowStart =
                row * rowStride

            for (col in 0 until width) {

                val sourceIndex =
                    rowStart +
                            col *
                            pixelStride

                destination[
                    destinationIndex++
                ] = source[sourceIndex]
            }
        }
    }

    /*
     * ==========================================
     * Rotation
     * ==========================================
     */
    private fun rotateBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int
    ): Bitmap {

        if (rotationDegrees == 0) {
            return bitmap
        }

        val matrix =
            Matrix()

        matrix.postRotate(
            rotationDegrees.toFloat()
        )

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /*
     * ==========================================
     * Bitmap -> RGB UINT8
     * ==========================================
     */
    private fun bitmapToRgbBuffer(
        bitmap: Bitmap
    ): ByteBuffer {

        val bufferSize =
            inputWidth *
                    inputHeight *
                    3

        val buffer =
            ByteBuffer.allocateDirect(
                bufferSize
            )

        buffer.order(
            ByteOrder.nativeOrder()
        )

        val pixels =
            IntArray(
                inputWidth *
                        inputHeight
            )

        bitmap.getPixels(
            pixels,
            0,
            inputWidth,
            0,
            0,
            inputWidth,
            inputHeight
        )

        for (pixel in pixels) {

            val red =
                (pixel shr 16) and 0xFF

            val green =
                (pixel shr 8) and 0xFF

            val blue =
                pixel and 0xFF

            buffer.put(
                red.toByte()
            )

            buffer.put(
                green.toByte()
            )

            buffer.put(
                blue.toByte()
            )
        }

        buffer.rewind()

        return buffer
    }
}