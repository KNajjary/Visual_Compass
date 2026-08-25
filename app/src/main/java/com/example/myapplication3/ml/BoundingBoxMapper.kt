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
         * این همان scale ای است که در
         * ImagePreprocessor استفاده کردیم:
         *
         * scale = max(
         *     imageWidth / 300,
         *     imageHeight / 300
         * )
         */

        val scale = max(
            imageWidth.toFloat() / 300f,
            imageHeight.toFloat() / 300f
        )

        /*
         * اندازه تصویر بعد از scale
         */
        val scaledWidth =
            imageWidth / scale

        val scaledHeight =
            imageHeight / scale

        /*
         * مقدار crop در تصویر 300x300
         */
        val cropX =
            (300f - scaledWidth) / 2f

        val cropY =
            (300f - scaledHeight) / 2f

        /*
         * Detection coordinates:
         *
         * normalized 0..1
         *
         * تبدیل به مختصات 300x300
         */
        val modelLeft =
            detection.left * 300f

        val modelTop =
            detection.top * 300f

        val modelRight =
            detection.right * 300f

        val modelBottom =
            detection.bottom * 300f

        /*
         * حذف crop
         */
        val imageX1 =
            (modelLeft - cropX) * scale

        val imageY1 =
            (modelTop - cropY) * scale

        val imageX2 =
            (modelRight - cropX) * scale

        val imageY2 =
            (modelBottom - cropY) * scale

        /*
         * تبدیل مختصات تصویر اصلی
         * به PreviewView
         */
        val xScale =
            previewWidth / imageWidth.toFloat()

        val yScale =
            previewHeight / imageHeight.toFloat()

        return ScreenBox(
            left = imageX1 * xScale,
            top = imageY1 * yScale,
            right = imageX2 * xScale,
            bottom = imageY2 * yScale
        )
    }
}