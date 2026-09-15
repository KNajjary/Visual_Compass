
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
     * Camera Image -> Preview FIT_CENTER
     * ==========================================
     *
     * The camera image is fitted inside the PreviewView
     * while preserving its aspect ratio.
     *
     * Therefore:
     *
     * scale = min(
     *     previewWidth / imageWidth,
     *     previewHeight / imageHeight
     * )
     *
     * The remaining space becomes letterbox/pillarbox.
     */

    private fun calculateImageRect(
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Float,
        previewHeight: Float
    ): ScreenBox {

        val imageW =
            imageWidth.toFloat()

        val imageH =
            imageHeight.toFloat()

        val scale =
            minOf(
                previewWidth / imageW,
                previewHeight / imageH
            )

        val displayedWidth =
            imageW * scale

        val displayedHeight =
            imageH * scale

        val offsetX =
            (previewWidth - displayedWidth) / 2f

        val offsetY =
            (previewHeight - displayedHeight) / 2f

        return ScreenBox(
            left = offsetX,
            top = offsetY,
            right = offsetX + displayedWidth,
            bottom = offsetY + displayedHeight
        )
    }

    /*
     * ==========================================
     * Detection -> Screen
     * ==========================================
     *
     * Pipeline:
     *
     * rotated image
     *       ↓
     * center square crop
     *       ↓
     * 320x320
     *       ↓
     * detection normalized coordinates
     *       ↓
     * rotated image coordinates
     *       ↓
     * FIT_CENTER Preview coordinates
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
         * 3. Crop space -> image space
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
         * 4. Calculate actual displayed image rect
         * ------------------------------------------
         */

        val imageRect =
            calculateImageRect(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                previewWidth = previewWidth,
                previewHeight = previewHeight
            )

        val scale =
            minOf(
                previewWidth / imageWidth.toFloat(),
                previewHeight / imageHeight.toFloat()
            )

        /*
         * ------------------------------------------
         * 5. Image -> Preview
         * ------------------------------------------
         */

        val screenX1 =
            imageRect.left +
                    imageX1 * scale

        val screenY1 =
            imageRect.top +
                    imageY1 * scale

        val screenX2 =
            imageRect.left +
                    imageX2 * scale

        val screenY2 =
            imageRect.top +
                    imageY2 * scale

        /*
         * ------------------------------------------
         * 6. Clamp to actual displayed image
         * ------------------------------------------
         */

        return ScreenBox(

            left =
                screenX1.coerceIn(
                    imageRect.left,
                    imageRect.right
                ),

            top =
                screenY1.coerceIn(
                    imageRect.top,
                    imageRect.bottom
                ),

            right =
                screenX2.coerceIn(
                    imageRect.left,
                    imageRect.right
                ),

            bottom =
                screenY2.coerceIn(
                    imageRect.top,
                    imageRect.bottom
                )
        )
    }

    /*
     * ==========================================
     * Processing Area -> Screen
     * ==========================================
     *
     * This represents the exact square region
     * that ImagePreprocessor sends to EfficientDet.
     *
     * Pipeline:
     *
     * rotated image
     *       ↓
     * center square crop
     *       ↓
     * 320x320
     */

    fun mapProcessingArea(
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Float,
        previewHeight: Float
    ): ScreenBox {

        /*
         * ------------------------------------------
         * 1. Calculate square crop
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
         * 2. Calculate actual FIT_CENTER image rect
         * ------------------------------------------
         */

        val imageRect =
            calculateImageRect(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                previewWidth = previewWidth,
                previewHeight = previewHeight
            )

        val scale =
            minOf(
                previewWidth / imageWidth.toFloat(),
                previewHeight / imageHeight.toFloat()
            )

        /*
         * ------------------------------------------
         * 3. Crop -> Preview
         * ------------------------------------------
         */

        val screenLeft =
            imageRect.left +
                    cropLeft * scale

        val screenTop =
            imageRect.top +
                    cropTop * scale

        val screenRight =
            imageRect.left +
                    (cropLeft + cropSize) * scale

        val screenBottom =
            imageRect.top +
                    (cropTop + cropSize) * scale

        /*
         * ------------------------------------------
         * 4. Return processing area
         * ------------------------------------------
         */

        return ScreenBox(
            left =
                screenLeft.coerceIn(
                    imageRect.left,
                    imageRect.right
                ),

            top =
                screenTop.coerceIn(
                    imageRect.top,
                    imageRect.bottom
                ),

            right =
                screenRight.coerceIn(
                    imageRect.left,
                    imageRect.right
                ),

            bottom =
                screenBottom.coerceIn(
                    imageRect.top,
                    imageRect.bottom
                )
        )
    }
}
