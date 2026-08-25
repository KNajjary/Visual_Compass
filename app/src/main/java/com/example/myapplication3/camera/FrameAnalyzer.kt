package com.example.myapplication3.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.myapplication3.ml.ModelOutput
import com.example.myapplication3.ml.ModelRunner

class FrameAnalyzer(
    private val modelRunner: ModelRunner,
    private val statistics: FrameStatistics,
    private val onResult: (ModelOutput) -> Unit
) : ImageAnalysis.Analyzer {

    private val changeDetector = FrameChangeDetector()

    private var lastProcessTime = 0L

    override fun analyze(image: ImageProxy) {

        val currentTime = System.currentTimeMillis()

        // فقط هر یک ثانیه یک فریم را بررسی کن
        if (currentTime - lastProcessTime < 1000L) {
            image.close()
            return
        }

        lastProcessTime = currentTime

        // آیا تصویر تغییر کرده؟
        val changed = changeDetector.hasChanged(image)

        if (!changed) {
            image.close()
            return
        }

        // تصویر تغییر کرده است
        statistics.onFrame()

        try {

            // اجرای مدل
            val result = modelRunner.run(image)

            // ارسال نتیجه به بیرون
            onResult(result)

        } finally {

            image.close()
        }
    }
}