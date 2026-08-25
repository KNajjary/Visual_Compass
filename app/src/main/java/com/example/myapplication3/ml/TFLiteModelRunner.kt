package com.example.myapplication3.ml

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteModelRunner(
    context: Context
) : ModelRunner {

    companion object {

        private const val MODEL_FILE =
            "ssd_mobilenet_v1.tflite"

        private const val LABEL_FILE =
            "coco_labels.txt"

        private const val MAX_DETECTIONS = 10

        private const val SCORE_THRESHOLD = 0.50f
    }

    private val interpreter: Interpreter

    private val preprocessor =
        ImagePreprocessor()

    private val labels: List<String>

    init {

        val modelBuffer: ByteBuffer =
            context.assets
                .open(MODEL_FILE)
                .use { inputStream ->

                    val bytes =
                        inputStream.readBytes()

                    ByteBuffer
                        .allocateDirect(bytes.size)
                        .order(ByteOrder.nativeOrder())
                        .apply {
                            put(bytes)
                            rewind()
                        }
                }

        interpreter =
            Interpreter(modelBuffer)

        interpreter.allocateTensors()

        labels =
            context.assets
                .open(LABEL_FILE)
                .bufferedReader()
                .use { reader ->
                    reader.readLines()
                }
        Log.d(
            "TFLiteModel",
            "Labels count = ${labels.size}"
        )

        labels.forEachIndexed { index, label ->
            Log.d(
                "TFLiteModel",
                "Label[$index] = $label"
            )
        }

        Log.d(
            "TFLiteModel",
            "===== Object Detection Model ====="
        )

        Log.d(
            "TFLiteModel",
            "Input shape: ${
                interpreter
                    .getInputTensor(0)
                    .shape()
                    .contentToString()
            }"
        )

        Log.d(
            "TFLiteModel",
            "Input type: ${
                interpreter
                    .getInputTensor(0)
                    .dataType()
            }"
        )

        for (i in 0 until interpreter.outputTensorCount) {

            Log.d(
                "TFLiteModel",
                "Output[$i] shape: ${
                    interpreter
                        .getOutputTensor(i)
                        .shape()
                        .contentToString()
                }"
            )

            Log.d(
                "TFLiteModel",
                "Output[$i] type: ${
                    interpreter
                        .getOutputTensor(i)
                        .dataType()
                }"
            )
        }
    }

    override fun run(
        image: ImageProxy
    ): ModelOutput {

        val inputBuffer =
            preprocessor.preprocess(image)

        /*
         * SSD MobileNet V1 standard outputs:
         *
         * boxes
         * classes
         * scores
         * num detections
         */

        val boxes =
            Array(1) {
                Array(MAX_DETECTIONS) {
                    FloatArray(4)
                }
            }

        val classes =
            Array(1) {
                FloatArray(MAX_DETECTIONS)
            }

        val scores =
            Array(1) {
                FloatArray(MAX_DETECTIONS)
            }

        val numDetections =
            FloatArray(1)

        val outputMap =
            HashMap<Int, Any>()

        outputMap[0] = boxes
        outputMap[1] = classes
        outputMap[2] = scores
        outputMap[3] = numDetections

        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            outputMap
        )

        val detectionCount =
            minOf(
                numDetections[0].toInt(),
                MAX_DETECTIONS
            )

        val detections =
            mutableListOf<Detection>()

        for (i in 0 until detectionCount) {

            val confidence =
                scores[0][i]

            if (confidence < SCORE_THRESHOLD) {
                continue
            }

            val classIndex =
                classes[0][i].toInt()

            val label =
                if (
                    classIndex >= 0 &&
                    classIndex < labels.size
                ) {
                    labels[classIndex]
                } else {
                    "Unknown"
                }

            /*
             * SSD boxes:
             *
             * [top, left, bottom, right]
             *
             * Coordinates are normalized 0..1.
             */

            val top =
                boxes[0][i][0]

            val left =
                boxes[0][i][1]

            val bottom =
                boxes[0][i][2]

            val right =
                boxes[0][i][3]

            Log.d(
                "TFLiteModel",
                "classIndex=$classIndex " +
                        "label=$label " +
                        "score=$confidence " +
                        "box=${boxes[0][i].contentToString()}"
            )

            detections.add(
                Detection(
                    label = label,
                    confidence = confidence,
                    left = left.coerceIn(0f, 1f),
                    top = top.coerceIn(0f, 1f),
                    right = right.coerceIn(0f, 1f),
                    bottom = bottom.coerceIn(0f, 1f)
                )
            )
        }

        Log.d(
            "TFLiteModel",
            "Detections: ${detections.size}"
        )

        detections.forEach {

            Log.d(
                "TFLiteModel",
                "${it.label} ${
                    "%.2f".format(it.confidence)
                }"
            )
        }

        return ModelOutput(
            detections = detections,
            imageWidth = image.width,
            imageHeight = image.height
        )
    }

    fun close() {
        interpreter.close()
    }
}