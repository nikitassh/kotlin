package ru.mirea.soshnin.practice.task14

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Азимут хранится во ViewModel и не сбрасывается при повороте
class CompassViewModel : ViewModel(), SensorEventListener {

    private val _azimuth = MutableStateFlow(0f)
    val azimuth = _azimuth.asStateFlow() // 0..360

    // угол стрелки без скачка 359 -> 0
    private val _rotation = MutableStateFlow(0f)
    val rotation = _rotation.asStateFlow()

    var displayRotation = Surface.ROTATION_0

    private val matrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR ->
                SensorManager.getRotationMatrixFromVector(
                    matrix, event.values
                )
            Sensor.TYPE_ACCELEROMETER -> {
                gravity = event.values.clone()
                if (!fromAccelMagnet()) return
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic = event.values.clone()
                if (!fromAccelMagnet()) return
            }
            else -> return
        }
        SensorManager.getOrientation(matrix, orientation)
        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        update(deg + 90f * displayRotation)
    }

    private fun fromAccelMagnet(): Boolean {
        val g = gravity ?: return false
        val m = geomagnetic ?: return false
        return SensorManager.getRotationMatrix(matrix, null, g, m)
    }

    private fun update(raw: Float) {
        val az = (raw % 360 + 360) % 360
        // кратчайший поворот от текущего угла
        var delta = az - _azimuth.value
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        // сглаживание от дрожания
        val smooth = delta * 0.3f
        _azimuth.value = ((_azimuth.value + smooth) % 360 + 360) % 360
        _rotation.value += smooth
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
