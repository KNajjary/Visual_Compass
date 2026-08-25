package com.example.myapplication3.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewReady: (PreviewView) -> Unit
) {

    AndroidView(
        modifier = modifier,
        factory = { context: Context ->

            PreviewView(context).also {

                it.scaleType =
                    PreviewView.ScaleType.FILL_CENTER

                onPreviewReady(it)
            }
        }
    )
}