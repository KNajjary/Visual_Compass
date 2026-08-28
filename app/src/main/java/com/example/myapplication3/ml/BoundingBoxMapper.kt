package com.example.myapplication3.ml

data class ScreenBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object BoundingBoxMapper {

    /*
     * ==========================================
     * Detection -> Screen
     * ==========================================
     */
    fun map(
        detection: Detection,
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Float,
        previewHeight: Float
    ): ScreenBox {

        /*
         * ------------------------------------------
         * 1. Calculate square crop
         * ------------------------------------------
         *
         * ImagePreprocessor does:
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
         * 2. Detection normalized -> crop space
         * ------------------------------------------
         */

        val cropX1 =
            detection.left * cropSize

        val cropY1 =
            detection.top * cropSize

        val cropX2 =
            detection.right * cropSize

        val cropY2 =
            detection.bottom * cropSize

        /*
         * ------------------------------------------
         * 3. Crop space -> rotated image space
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
         * 4. Rotated image -> Preview
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
         * 5. Final screen coordinates
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

    /*
     * ==========================================
     * Processing Area -> Screen
     * ==========================================
     *
     * Returns the exact square area that
     * ImagePreprocessor sends to the model.
     *
     * Pipeline:
     *
     * rotated image
     *       ↓
     * center square crop
     *       ↓
     * resize 320x320
     *
     * The returned ScreenBox represents the
     * square crop on the Preview.
     */
    fun mapProcessingArea(
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Float,
        previewHeight: Float
    ): ScreenBox {

        /*
         * ------------------------------------------
         * Same crop calculation as ImagePreprocessor
         * ------------------------------------------
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
         * Rotated image -> Preview
         * ------------------------------------------
         */

        val xScale =
            previewWidth /
                    imageWidth.toFloat()

        val yScale =
            previewHeight /
                    imageHeight.toFloat()

        val screenLeft =
            cropLeft * xScale

        val screenTop =
            cropTop * yScale

        val screenRight =
            (cropLeft + cropSize) * xScale

        val screenBottom =
            (cropTop + cropSize) * yScale

        return ScreenBox(

            left =
                screenLeft.coerceIn(
                    0f,
                    previewWidth
                ),

            top =
                screenTop.coerceIn(
                    0f,
                    previewHeight
                ),

            right =
                screenRight.coerceIn(
                    0f,
                    previewWidth
                ),

            bottom =
                screenBottom.coerceIn(
                    0f,
                    previewHeight
                )
        )
    }
}