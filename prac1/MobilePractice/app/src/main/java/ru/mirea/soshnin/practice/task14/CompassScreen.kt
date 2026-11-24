package ru.mirea.soshnin.practice.task14

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mirea.soshnin.practice.ui.AppTheme
import ru.mirea.soshnin.practice.ui.TaskScaffold

private val Night = Color(0xFF101418)
private val Red = Color(0xFFE53935)

@Composable
fun CompassScreen(
    onBack: () -> Unit,
    vm: CompassViewModel = viewModel()
) {
    val context = LocalContext.current
    val manager = remember {
        context.getSystemService(SensorManager::class.java)
    }
    // сначала датчик вращения, если нет - акселерометр + магнитометр
    val sensors = remember {
        val rotation = manager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR
        )
        val accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = manager.getDefaultSensor(
            Sensor.TYPE_MAGNETIC_FIELD
        )
        when {
            rotation != null -> listOf(rotation)
            accel != null && magnet != null -> listOf(accel, magnet)
            else -> emptyList()
        }
    }
    vm.displayRotation = LocalView.current.display?.rotation ?: 0

    // слушатель работает только когда экран виден
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sensors.forEach {
                    manager.registerListener(
                        vm, it, SensorManager.SENSOR_DELAY_UI
                    )
                }
                Lifecycle.Event.ON_PAUSE ->
                    manager.unregisterListener(vm)
                else -> {}
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            manager.unregisterListener(vm)
        }
    }

    AppTheme(dark = true) {
        TaskScaffold("Компас", onBack) {
            Box(
                Modifier.fillMaxSize().background(Night),
                contentAlignment = Alignment.Center
            ) {
                if (sensors.isEmpty()) {
                    Text(
                        "Устройство не поддерживает датчик ориентации",
                        color = Red, fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Compass(vm)
                }
            }
        }
    }
}

@Composable
fun Compass(vm: CompassViewModel) {
    val azimuth by vm.azimuth.collectAsState()
    val rotation by vm.rotation.collectAsState()
    // плавный поворот диска
    val angle by animateFloatAsState(
        targetValue = -rotation,
        animationSpec = tween(200),
        label = "compass"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth(0.78f).aspectRatio(1f).rotate(angle),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(Modifier.fillMaxSize()) { drawDial() }
            Text(
                "N", color = Red, fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Азимут: ${azimuth.toInt()}°",
            color = Color.White, fontSize = 36.sp
        )
    }
}

// Диск с делениями и двухцветная стрелка
fun DrawScope.drawDial() {
    val r = size.minDimension / 2
    val c = center
    drawCircle(Color(0xFF1E252C), r)
    drawCircle(Color(0xFF455A64), r, style = Stroke(4f))
    for (i in 0 until 72) {
        val len = if (i % 18 == 0) 0.12f else 0.05f
        val a = Math.toRadians(i * 5.0)
        val sin = kotlin.math.sin(a).toFloat()
        val cos = kotlin.math.cos(a).toFloat()
        drawLine(
            Color(0xFF90A4AE),
            Offset(c.x + sin * r * (1 - len), c.y - cos * r * (1 - len)),
            Offset(c.x + sin * r * 0.97f, c.y - cos * r * 0.97f),
            strokeWidth = 3f
        )
    }
    val half = r * 0.8f // стрелка = 80% диаметра
    val w = r * 0.1f
    val north = Path().apply {
        moveTo(c.x, c.y - half)
        lineTo(c.x - w, c.y)
        lineTo(c.x + w, c.y)
        close()
    }
    val south = Path().apply {
        moveTo(c.x, c.y + half)
        lineTo(c.x - w, c.y)
        lineTo(c.x + w, c.y)
        close()
    }
    drawPath(north, Red)
    drawPath(south, Color(0xFF78909C))
    drawCircle(Color.White, w * 0.4f)
}
