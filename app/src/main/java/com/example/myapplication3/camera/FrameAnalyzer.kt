package com.example.myapplication3.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.myapplication3.ml.ModelOutput
import com.example.myapplication3.ml.ModelRunner

class FrameAnalyzer(
    private val modelRunner: ModelRunner,
    private val statistics: FrameStatistics,
    private val onResult: (ModelOutput) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {

        private const val TAG =
            "FrameAnalyzer"

        /*
         * Run inference approximately every 300 ms.
         *
         * 1000 ms = 1 inference / second
         * 300 ms  = ~3.3 inferences / second
         */
        private const val INFERENCE_INTERVAL_MS =
            300L
    }

    private var lastProcessTime =
        0L

    override fun analyze(image: ImageProxy) {

        /*
         * This method is already running on the
         * dedicated background executor created
         * in CameraManager.
         */

        val currentTime =
            System.currentTimeMillis()

        /*
         * Throttle inference.
         *
         * Camera may produce 30+ frames/sec,
         * but we don't need to run TFLite on every frame.
         */
        if (
            currentTime - lastProcessTime <
            INFERENCE_INTERVAL_MS
        ) {

            image.close()

            return
        }

        lastProcessTime =
            currentTime

        statistics.onFrame()

        try {

            /*
             * --------------------------------------
             * PREPROCESSING + TFLITE INFERENCE
             * --------------------------------------
             *
             * Runs on background thread.
             */
            val result =
                modelRunner.run(image)

            /*
             * --------------------------------------
             * RESULT
             * --------------------------------------
             *
             * Important:
             *
             * onResult may update UI state.
             *
             * If the UI callback must explicitly run
             * on Main, we can dispatch it there later.
             */
            onResult(result)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Model inference failed",
                e
            )

        } finally {

            /*
             * ImageProxy MUST always be closed.
             */
            image.close()
        }
    }
}