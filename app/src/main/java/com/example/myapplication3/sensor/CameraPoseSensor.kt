package com.example.myapplication3.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class CameraPoseSensor(
    context: Context,
    private val onOrientationChanged: (CameraPose) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR)
            return

        val rotationMatrix = FloatArray(9)

        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            event.values
        )

        val orientation = FloatArray(3)

        SensorManager.getOrientation(
            rotationMatrix,
            orientation
        )

        onOrientationChanged(
            CameraPose(
                yaw = Math.toDegrees(orientation[0].toDouble()).toFloat(),
                pitch = Math.toDegrees(orientation[1].toDouble()).toFloat(),
                roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            )
        )
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}