package com.example.myapplication3.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication3.ml.ModelOutput
import com.example.myapplication3.ml.ModelRunner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val modelRunner: ModelRunner,
    private val statistics: FrameStatistics,
    private val onModelResult: (ModelOutput) -> Unit
) {

    /*
     * =========================================================
     * Background executor
     * =========================================================
     *
     * CameraX analysis + preprocessing + TFLite inference
     * همه روی این thread انجام می‌شوند.
     *
     * Single thread مهم است چون یک Interpreter مشترک داریم.
     */
    private val analysisExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null

    /*
     * =========================================================
     * Start Camera
     * =========================================================
     */

    fun startCamera(previewView: PreviewView) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            try {

                val provider =
                    cameraProviderFuture.get()

                cameraProvider = provider

                /*
                 * =================================================
                 * Preview
                 * =================================================
                 */

                val preview =
                    Preview.Builder()
                        .build()

                preview.surfaceProvider =
                    previewView.surfaceProvider

                /*
                 * =================================================
                 * Image Analysis
                 * =================================================
                 */

                val analysis =
                    ImageAnalysis.Builder()

                        /*
                         * اگر inference کند باشد،
                         * فریم‌های قدیمی را نگه نمی‌داریم.
                         *
                         * فقط آخرین فریم مهم است.
                         */
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )

                        /*
                         * YUV برای پردازش دوربین مناسب است.
                         */
                        .setOutputImageFormat(
                            ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
                        )

                        .build()

                /*
                 * =================================================
                 * Analyzer
                 * =================================================
                 *
                 * بسیار مهم:
                 *
                 * اینجا نباید Main Executor استفاده شود.
                 *
                 * قبلاً:
                 *
                 * ContextCompat.getMainExecutor(context)
                 *
                 * استفاده می‌کردیم.
                 *
                 * حالا:
                 *
                 * analysisExecutor
                 *
                 * استفاده می‌شود.
                 */
                analysis.setAnalyzer(
                    analysisExecutor,
                    FrameAnalyzer(
                        modelRunner = modelRunner,
                        statistics = statistics,
                        onResult = onModelResult
                    )
                )

                /*
                 * =================================================
                 * Bind Camera
                 * =================================================
                 */

                provider.unbindAll()

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                Log.d(
                    "CameraManager",
                    "Camera started successfully"
                )

            } catch (e: Exception) {

                Log.e(
                    "CameraManager",
                    "Failed to start camera",
                    e
                )
            }

        }, ContextCompat.getMainExecutor(context))
    }

    /*
     * =========================================================
     * Stop Camera
     * =========================================================
     */

    fun stopCamera() {

        cameraProvider?.unbindAll()

        cameraProvider = null

        Log.d(
            "CameraManager",
            "Camera stopped"
        )
    }

    /*
     * =========================================================
     * Shutdown
     * =========================================================
     *
     * وقتی CameraScreen از Composition خارج شد،
     * باید executor را هم متوقف کنیم.
     */
    fun shutdown() {

        stopCamera()

        analysisExecutor.shutdown()

        Log.d(
            "CameraManager",
            "Analysis executor shutdown"
        )
    }
}