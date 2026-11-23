package com.lab3d.science

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lab3d.science.model.ExperimentId
import com.lab3d.science.model.LabProgress
import com.lab3d.science.sim.LabState
import com.lab3d.science.ui.ExperimentScreen
import com.lab3d.science.ui.HomeScreen
import com.lab3d.science.ui.LabTheme
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var activeLabState: LabState? = null
    private var lastShakeMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LabProgress.init(this)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        setContent {
            LabTheme {
                var currentExp by remember { mutableStateOf<ExperimentId?>(null) }

                BackHandler(enabled = currentExp != null) {
                    activeLabState = null
                    currentExp = null
                }

                val exp = currentExp
                if (exp == null) {
                    activeLabState = null
                    HomeScreen(onOpenExperiment = { currentExp = it })
                } else {
                    val state = remember(exp) { LabState(exp) }
                    activeLabState = state
                    ExperimentScreen(
                        state = state,
                        onBack = {
                            activeLabState = null
                            currentExp = null
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val acc = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (acc != null) {
            sensorManager?.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val v = event?.values ?: return
        val g = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (g > 2.35f && now - lastShakeMs > 900L) {
            lastShakeMs = now
            activeLabState?.onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
