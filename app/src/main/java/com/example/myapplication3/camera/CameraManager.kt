package com.example.myapplication3.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication3.ml.ModelOutput
import com.example.myapplication3.ml.ModelRunner

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val modelRunner: ModelRunner,
    private val statistics: FrameStatistics,
    private val onModelResult: (ModelOutput) -> Unit
) {

    fun startCamera(previewView: PreviewView) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            // Preview
            val preview =
                Preview.Builder().build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            // Image Analysis
            val analysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .setOutputImageFormat(
                        ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
                    )
                    .build()

            analysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                FrameAnalyzer(
                    modelRunner = modelRunner,
                    statistics = statistics,
                    onResult = onModelResult
                )
            )

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

        }, ContextCompat.getMainExecutor(context))
    }
}