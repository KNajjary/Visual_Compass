package com.example.myapplication3

import android.Manifest
import android.content.pm.PackageManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.compose.runtime.collectAsState

import com.example.myapplication3.camera.CameraManager
import com.example.myapplication3.camera.CameraPreview
import com.example.myapplication3.camera.FrameStatistics

import com.example.myapplication3.ml.TFLiteModelRunner

import com.example.myapplication3.sensor.CameraPose
import com.example.myapplication3.sensor.CameraPoseSensor

import com.example.myapplication3.overlay.CameraOverlay
import com.example.myapplication3.overlay.DebugInfo

import com.example.myapplication3.ml.ModelOutput

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier

) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --------------------------------------------------
    // TFLite model
    // --------------------------------------------------

    val tfliteRunner = remember {
        TFLiteModelRunner(context)
    }

    // --------------------------------------------------
    // Camera permission
    // --------------------------------------------------

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // --------------------------------------------------
    // Frame statistics
    // --------------------------------------------------
    var modelOutput by remember {
        mutableStateOf(
            ModelOutput(
                detections = emptyList(),
                imageWidth = 1,
                imageHeight = 1
            )
        )
    }

    val statistics = remember {
        FrameStatistics()
    }

    val frameNumber by statistics.frameNumber.collectAsState()
    val fps by statistics.fps.collectAsState()

    // --------------------------------------------------
    // Camera pose
    // --------------------------------------------------

    var cameraPose by remember {
        mutableStateOf(
            CameraPose(
                yaw = 0f,
                pitch = 0f,
                roll = 0f
            )
        )
    }

    // --------------------------------------------------
    // Camera pose sensor
    // --------------------------------------------------

    val cameraPoseSensor = remember {

        CameraPoseSensor(context) {
            cameraPose = it
        }
    }

    DisposableEffect(Unit) {

        cameraPoseSensor.start()

        onDispose {
            cameraPoseSensor.stop()
        }
    }

    // --------------------------------------------------
    // Camera
    // --------------------------------------------------

    if (hasPermission) {

        val cameraManager = remember {

            CameraManager(
                context = context,
                lifecycleOwner = lifecycleOwner,
                modelRunner = tfliteRunner,
                statistics = statistics,
                onModelResult = { result ->
                    modelOutput = result
                }
            )
        }

        // --------------------------------------------------
        // Debug information
        // --------------------------------------------------

        val debugInfo = DebugInfo(
            frameNumber = frameNumber,
            fps = fps,
            pose = cameraPose
        )

        // --------------------------------------------------
        // Camera UI
        // --------------------------------------------------

        Box(
            modifier = modifier.fillMaxSize()
        ) {

            // Camera preview
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewReady = { previewView ->

                    cameraManager.startCamera(
                        previewView
                    )
                }
            )

            // Debug panel
            CameraOverlay(
                debugInfo = debugInfo,
                detections = modelOutput.detections,
                imageWidth = modelOutput.imageWidth,
                imageHeight = modelOutput.imageHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

    } else {

        Text("در انتظار مجوز دوربین...")

    }
}