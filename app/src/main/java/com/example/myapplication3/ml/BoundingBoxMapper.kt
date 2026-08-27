package com.example.myapplication3.ml

import kotlin.math.max

data class ScreenBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object BoundingBoxMapper {

    fun map(
        detection: Detection,
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Float,
        previewHeight: Float
    ): ScreenBox {

        /*
         * IMPORTANT:
         *
         * The TFLite model uses a 320x320 input.
         *
         * ImagePreprocessor does:
         *
         * rotated image
         *      ↓
         * center square crop
         *      ↓
         * resize to 320x320
         *
         * Therefore the bounding box must be mapped
         * back from the 320x320 model space.
         */

        val modelSize =
            320f

        /*
         * ------------------------------------------
         * 1. Calculate square crop
         * ------------------------------------------
         *
         * ImagePreprocessor uses:
         *
         * cropSize = min(imageWidth, imageHeight)
         *
         * cropLeft = (imageWidth - cropSize) / 2
         * cropTop  = (imageHeight - cropSize) / 2
         */

        val cropSize =
            minOf(
                imageWidth,
                imageHeight
            ).toFloat()

        val cropLeft =
            (imageWidth - cropSize) / 2f

        val cropTop =
            (imageHeight - cropSize) / 2f

        /*
         * ------------------------------------------
         * 2. Detection coordinates
         * ------------------------------------------
         *
         * Detection coordinates are normalized:
         *
         * 0.0 ... 1.0
         *
         * Convert them into 320x320 model space.
         */

        val modelLeft =
            detection.left * modelSize

        val modelTop =
            detection.top * modelSize

        val modelRight =
            detection.right * modelSize

        val modelBottom =
            detection.bottom * modelSize

        /*
         * ------------------------------------------
         * 3. Model space -> cropped image space
         * ------------------------------------------
         *
         * The 320x320 image was created by resizing
         * the square crop.
         *
         * Therefore:
         *
         * crop coordinate =
         * model coordinate / 320 * cropSize
         */

        val cropX1 =
            modelLeft / modelSize * cropSize

        val cropY1 =
            modelTop / modelSize * cropSize

        val cropX2 =
            modelRight / modelSize * cropSize

        val cropY2 =
            modelBottom / modelSize * cropSize

        /*
         * ------------------------------------------
         * 4. Cropped image -> rotated image
         * ------------------------------------------
         */

        val imageX1 =
            cropLeft + cropX1

        val imageY1 =
            cropTop + cropY1

        val imageX2 =
            cropLeft + cropX2

        val imageY2 =
            cropTop + cropY2

        /*
         * ------------------------------------------
         * 5. Rotated image -> PreviewView
         * ------------------------------------------
         */

        val xScale =
            previewWidth /
                    imageWidth.toFloat()

        val yScale =
            previewHeight /
                    imageHeight.toFloat()

        /*
         * ------------------------------------------
         * 6. Final screen coordinates
         * ------------------------------------------
         */

        return ScreenBox(

            left =
                (imageX1 * xScale)
                    .coerceIn(
                        0f,
                        previewWidth
                    ),

            top =
                (imageY1 * yScale)
                    .coerceIn(
                        0f,
                        previewHeight
                    ),

            right =
                (imageX2 * xScale)
                    .coerceIn(
                        0f,
                        previewWidth
                    ),

            bottom =
                (imageY2 * yScale)
                    .coerceIn(
                        0f,
                        previewHeight
                    )
        )
    }
}