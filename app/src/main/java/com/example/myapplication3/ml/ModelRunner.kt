package com.example.myapplication3.ml

import androidx.camera.core.ImageProxy

interface ModelRunner {

    fun run(image: ImageProxy): ModelOutput
}