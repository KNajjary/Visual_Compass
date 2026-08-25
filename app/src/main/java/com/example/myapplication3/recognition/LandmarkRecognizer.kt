package com.example.myapplication3.recognition

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

class LandmarkRecognizer(
    private val context: Context
) {

    private var interpreter: Interpreter? = null


    init {
        loadModel()
    }


    private fun loadModel() {

        try {

            val model = context.assets
                .open("landmarks.tflite")
                .readBytes()

            val buffer = ByteBuffer.allocateDirect(
                model.size
            )

            buffer.put(model)

            interpreter = Interpreter(buffer)


            Log.d(
                "LandmarkRecognizer",
                "Model loaded"
            )


        } catch (e: Exception) {

            Log.e(
                "LandmarkRecognizer",
                "Model loading failed",
                e
            )

        }

    }


    fun recognize(image: ImageProxy) {

        Log.d(
            "LandmarkRecognizer",
            "Frame received"
        )

    }

}