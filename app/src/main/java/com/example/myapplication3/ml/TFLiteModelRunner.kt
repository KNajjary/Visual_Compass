package com.example.myapplication3.ml

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteModelRunner(
    context: Context
) : ModelRunner {

    companion object {

        private const val TAG =
            "TFLiteModel"

        private const val MODEL_FILE =
            "efficientdet_lite0.tflite"

        private const val LABEL_FILE =
            "coco_labels.txt"

        private const val SCORE_THRESHOLD =
            0.30f
    }

    private val interpreter: Interpreter

    private val preprocessor: ImagePreprocessor

    private val labels: List<String>

    private val inputWidth: Int

    private val inputHeight: Int

    init {

        /*
         * ==========================================
         * Load model
         * ==========================================
         */
        val modelBuffer =
            context.assets
                .open(MODEL_FILE)
                .use { inputStream ->

                    val bytes =
                        inputStream.readBytes()

                    ByteBuffer
                        .allocateDirect(
                            bytes.size
                        )
                        .order(
                            ByteOrder.nativeOrder()
                        )
                        .apply {

                            put(bytes)

                            rewind()
                        }
                }

        interpreter =
            Interpreter(modelBuffer)

        interpreter.allocateTensors()

        /*
         * ==========================================
         * Read model input dynamically
         * ==========================================
         */
        val inputTensor =
            interpreter.getInputTensor(0)

        val inputShape =
            inputTensor.shape()

        val inputType =
            inputTensor.dataType()

        require(
            inputShape.size == 4
        ) {
            "Expected 4D input tensor"
        }

        inputHeight =
            inputShape[1]

        inputWidth =
            inputShape[2]

        val inputChannels =
            inputShape[3]

        require(
            inputChannels == 3
        ) {
            "Expected RGB input with 3 channels"
        }

        require(
            inputType == DataType.UINT8
        ) {
            "This preprocessor currently expects UINT8 input. " +
                    "Model input type = $inputType"
        }

        /*
         * ==========================================
         * Create preprocessing pipeline
         * ==========================================
         */
        preprocessor =
            ImagePreprocessor(
                inputWidth = inputWidth,
                inputHeight = inputHeight
            )

        /*
         * ==========================================
         * Labels
         * ==========================================
         */
        labels =
            context.assets
                .open(LABEL_FILE)
                .bufferedReader()
                .use { reader ->
                    reader.readLines()
                        .map {
                            it.trim()
                        }
                        .filter {
                            it.isNotEmpty()
                        }
                }

        Log.d(
            TAG,
            "Labels count = ${labels.size}"
        )

        labels.forEachIndexed { index, label ->

            Log.d(
                TAG,
                "Label[$index] = $label"
            )
        }

        /*
         * ==========================================
         * Model information
         * ==========================================
         */
        Log.d(
            TAG,
            "===== Object Detection Model ====="
        )

        Log.d(
            TAG,
            "Input shape = ${
                inputShape.contentToString()
            }"
        )

        Log.d(
            TAG,
            "Input type = $inputType"
        )

        Log.d(
            TAG,
            "Input size = ${inputWidth}x${inputHeight}"
        )

        for (
        i in 0 until interpreter.outputTensorCount
        ) {

            val tensor =
                interpreter.getOutputTensor(i)

            Log.d(
                TAG,
                "Output[$i] shape = ${
                    tensor.shape()
                        .contentToString()
                }"
            )

            Log.d(
                TAG,
                "Output[$i] type = ${
                    tensor.dataType()
                }"
            )
        }
    }

    override fun run(
        image: ImageProxy
    ): ModelOutput {

        /*
         * ==========================================
         * PREPROCESS
         * ==========================================
         */
        val preprocessing =
            preprocessor.preprocess(
                image
            )

        val inputBuffer =
            preprocessing.buffer

        /*
         * ==========================================
         * Allocate outputs dynamically
         * ==========================================
         *
         * This is important.
         *
         * We don't assume:
         *
         * 10 detections
         *
         * or
         *
         * 25 detections
         *
         * anymore.
         *
         * The output tensor itself tells us.
         */
        val boxesTensor =
            interpreter.getOutputTensor(0)

        val classesTensor =
            interpreter.getOutputTensor(1)

        val scoresTensor =
            interpreter.getOutputTensor(2)

        val countTensor =
            interpreter.getOutputTensor(3)

        val maxDetections =
            boxesTensor.shape()[1]

        val boxes =
            Array(1) {
                Array(maxDetections) {
                    FloatArray(4)
                }
            }

        val classes =
            Array(1) {
                FloatArray(
                    maxDetections
                )
            }

        val scores =
            Array(1) {
                FloatArray(
                    maxDetections
                )
            }

        val numDetections =
            FloatArray(1)

        val outputMap =
            HashMap<Int, Any>()

        outputMap[0] =
            boxes

        outputMap[1] =
            classes

        outputMap[2] =
            scores

        outputMap[3] =
            numDetections

        /*
         * ==========================================
         * INFERENCE
         * ==========================================
         */
        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            outputMap
        )

        /*
         * ==========================================
         * Parse detections
         * ==========================================
         */
        val detectionCount =
            numDetections[0]
                .toInt()
                .coerceIn(
                    0,
                    maxDetections
                )

        val detections =
            mutableListOf<Detection>()

        for (
        i in 0 until detectionCount
        ) {

            val confidence =
                scores[0][i]

            if (
                confidence <
                SCORE_THRESHOLD
            ) {
                continue
            }

            val classIndex =
                classes[0][i]
                    .toInt()

            val label =
                if (
                    classIndex >= 0 &&
                    classIndex < labels.size
                ) {
                    labels[classIndex]
                } else {
                    "Unknown"
                }

            val top =
                boxes[0][i][0]

            val left =
                boxes[0][i][1]

            val bottom =
                boxes[0][i][2]

            val right =
                boxes[0][i][3]

            Log.d(
                TAG,
                "classIndex=$classIndex " +
                        "label=$label " +
                        "score=$confidence " +
                        "box=${
                            boxes[0][i]
                                .contentToString()
                        }"
            )

            detections.add(
                Detection(

                    label = label,

                    confidence =
                        confidence,

                    left =
                        left.coerceIn(
                            0f,
                            1f
                        ),

                    top =
                        top.coerceIn(
                            0f,
                            1f
                        ),

                    right =
                        right.coerceIn(
                            0f,
                            1f
                        ),

                    bottom =
                        bottom.coerceIn(
                            0f,
                            1f
                        )
                )
            )
        }

        Log.d(
            TAG,
            "Detections: ${detections.size}"
        )

        /*
         * ==========================================
         * Return
         * ==========================================
         */
        return ModelOutput(

            detections =
                detections,

            /*
             * These dimensions represent the
             * ROTATED / DISPLAY-ORIENTED image.
             *
             * They must match BoundingBoxMapper.
             */
            imageWidth =
                preprocessing.rotatedWidth,

            imageHeight =
                preprocessing.rotatedHeight
        )
    }

    fun close() {

        interpreter.close()
    }
}