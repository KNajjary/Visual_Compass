package com.example.myapplication3.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication3.ml.BoundingBoxMapper
import com.example.myapplication3.ml.Detection

@Composable
fun CameraOverlay(
    debugInfo: DebugInfo,
    detections: List<Detection> = emptyList(),
    imageWidth: Int = 1,
    imageHeight: Int = 1,
    modifier: Modifier = Modifier
) {

    val pose =
        debugInfo.pose

    var debugPanelExpanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            modifier.fillMaxSize()
    ) {

        /*
         * ==========================================
         * Processing Area
         * ==========================================
         *
         * ImagePreprocessor currently does:
         *
         * rotated image
         *       ↓
         * center square crop
         *       ↓
         * 320x320
         *
         * Everything outside this square is NOT
         * sent to the model.
         */

        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {

            if (
                imageWidth > 1 &&
                imageHeight > 1
            ) {

                val processingArea =
                    BoundingBoxMapper.mapProcessingArea(
                        imageWidth =
                            imageWidth,

                        imageHeight =
                            imageHeight,

                        previewWidth =
                            size.width,

                        previewHeight =
                            size.height
                    )

                val left =
                    processingArea.left

                val top =
                    processingArea.top

                val right =
                    processingArea.right

                val bottom =
                    processingArea.bottom

                /*
                 * --------------------------------------
                 * Darken area NOT processed by model
                 * --------------------------------------
                 */

                /*
                 * Top
                 */
                if (top > 0f) {

                    drawRect(
                        color =
                            Color.Black.copy(
                                alpha = 0.35f
                            ),

                        topLeft =
                            Offset(
                                0f,
                                0f
                            ),

                        size =
                            Size(
                                size.width,
                                top
                            )
                    )
                }

                /*
                 * Bottom
                 */
                if (bottom < size.height) {

                    drawRect(
                        color =
                            Color.Black.copy(
                                alpha = 0.35f
                            ),

                        topLeft =
                            Offset(
                                0f,
                                bottom
                            ),

                        size =
                            Size(
                                size.width,
                                size.height - bottom
                            )
                    )
                }

                /*
                 * Left
                 */
                if (left > 0f) {

                    drawRect(
                        color =
                            Color.Black.copy(
                                alpha = 0.35f
                            ),

                        topLeft =
                            Offset(
                                0f,
                                top
                            ),

                        size =
                            Size(
                                left,
                                bottom - top
                            )
                    )
                }

                /*
                 * Right
                 */
                if (right < size.width) {

                    drawRect(
                        color =
                            Color.Black.copy(
                                alpha = 0.35f
                            ),

                        topLeft =
                            Offset(
                                right,
                                top
                            ),

                        size =
                            Size(
                                size.width - right,
                                bottom - top
                            )
                    )
                }

                /*
                 * --------------------------------------
                 * Processing area border
                 * --------------------------------------
                 */

                drawRect(
                    color =
                        Color.Yellow,

                    topLeft =
                        Offset(
                            left,
                            top
                        ),

                    size =
                        Size(
                            right - left,
                            bottom - top
                        ),

                    style =
                        Stroke(
                            width = 6f
                        )
                )

                /*
                 * --------------------------------------
                 * Processing area label
                 * --------------------------------------
                 */

                val paint =
                    android.graphics.Paint().apply {

                        color =
                            android.graphics.Color.YELLOW

                        textSize =
                            36f

                        textAlign =
                            android.graphics.Paint.Align.CENTER

                        isAntiAlias =
                            true

                        setShadowLayer(
                            6f,
                            0f,
                            0f,
                            android.graphics.Color.BLACK
                        )
                    }

                drawContext
                    .canvas
                    .nativeCanvas
                    .drawText(
                        "PROCESSING AREA",
                        (left + right) / 2f,
                        top + 45f,
                        paint
                    )
            }
        }

        /*
         * ==========================================
         * Detection boxes
         * ==========================================
         */

        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {

            detections.forEach { detection ->

                val box =
                    BoundingBoxMapper.map(
                        detection =
                            detection,

                        imageWidth =
                            imageWidth,

                        imageHeight =
                            imageHeight,

                        previewWidth =
                            size.width,

                        previewHeight =
                            size.height
                    )

                val left =
                    box.left

                val top =
                    box.top

                val right =
                    box.right

                val bottom =
                    box.bottom

                drawRect(
                    color =
                        Color.Red,

                    topLeft =
                        Offset(
                            left,
                            top
                        ),

                    size =
                        Size(
                            right - left,
                            bottom - top
                        ),

                    style =
                        Stroke(
                            width = 5f
                        )
                )
            }
        }

        /*
         * ==========================================
         * Debug panel
         * ==========================================
         */

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .background(
                        Color.Black.copy(
                            alpha = 0.5f
                        )
                    )
                    .padding(12.dp)
        ) {

            Text(
                text =
                    if (debugPanelExpanded) {
                        "▲ Info Panel"
                    } else {
                        "▼ Info Panel"
                    },

                color =
                    Color.White,

                fontSize =
                    16.sp,

                modifier =
                    Modifier.clickable {

                        debugPanelExpanded =
                            !debugPanelExpanded
                    }
            )

            if (debugPanelExpanded) {

                Text(
                    text =
                        "Frame     : ${debugInfo.frameNumber}",

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Text(
                    text =
                        "FPS       : ${
                            debugInfo.fps
                                ?.let {
                                    "%.1f".format(it)
                                }
                                ?: "---"
                        }",

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        "========== Camera ==========",

                    color =
                        Color.Yellow,

                    fontSize =
                        18.sp
                )

                Text(
                    text =
                        "Yaw       : %.1f°"
                            .format(
                                pose.yaw
                            ),

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Text(
                    text =
                        "Pitch     : %.1f°"
                            .format(
                                pose.pitch
                            ),

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Text(
                    text =
                        "Roll      : %.1f°"
                            .format(
                                pose.roll
                            ),

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        "========== Position ==========",

                    color =
                        Color.Yellow,

                    fontSize =
                        18.sp
                )

                Text(
                    text =
                        "Latitude  : ${
                            pose.latitude
                                ?.let {
                                    "%.6f".format(it)
                                }
                                ?: "---"
                        }",

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Text(
                    text =
                        "Longitude : ${
                            pose.longitude
                                ?.let {
                                    "%.6f".format(it)
                                }
                                ?: "---"
                        }",

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )

                Text(
                    text =
                        "Altitude  : ${
                            pose.altitude
                                ?.let {
                                    "%.1f m".format(it)
                                }
                                ?: "---"
                        }",

                    color =
                        Color.White,

                    fontSize =
                        16.sp
                )
            }
        }

        /*
         * ==========================================
         * Detection labels
         * ==========================================
         */

        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {

            detections.forEach { detection ->

                val box =
                    BoundingBoxMapper.map(
                        detection =
                            detection,

                        imageWidth =
                            imageWidth,

                        imageHeight =
                            imageHeight,

                        previewWidth =
                            size.width,

                        previewHeight =
                            size.height
                    )

                val left =
                    box.left

                val top =
                    box.top

                val right =
                    box.right

                val bottom =
                    box.bottom

                val centerX =
                    (left + right) / 2f

                val centerY =
                    (top + bottom) / 2f

                val paint =
                    android.graphics.Paint().apply {

                        color =
                            android.graphics.Color.WHITE

                        textSize =
                            42f

                        textAlign =
                            android.graphics.Paint.Align.CENTER

                        isAntiAlias =
                            true

                        setShadowLayer(
                            6f,
                            0f,
                            0f,
                            android.graphics.Color.BLACK
                        )
                    }

                /*
                 * Object label
                 */
                drawContext
                    .canvas
                    .nativeCanvas
                    .drawText(
                        detection.label,
                        centerX,
                        centerY,
                        paint
                    )

                /*
                 * Confidence
                 */
                drawContext
                    .canvas
                    .nativeCanvas
                    .drawText(
                        "${"%.1f".format(
                            detection.confidence * 100
                        )}%",
                        centerX,
                        centerY + 50f,
                        paint
                    )
            }
        }
    }
}