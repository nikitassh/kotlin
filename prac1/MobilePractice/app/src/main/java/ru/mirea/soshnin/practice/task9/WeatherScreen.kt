package ru.mirea.soshnin.practice.task9

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ru.mirea.soshnin.practice.ui.AskNotificationPermission
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Composable
fun WeatherScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    AskNotificationPermission()
    val infos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(WEATHER_WORK)
        .collectAsState(initial = emptyList())
    val working = infos.any { !it.state.isFinished }
    val report = infos.firstOrNull { "report" in it.tags }
        ?.outputData?.getString(KEY_REPORT)

    TaskScaffold("Задание 9. Прогноз погоды", onBack) {
        Button(
            onClick = { startWeather(context) },
            enabled = !working
        ) { Text("Собрать прогноз") }
        Spacer(Modifier.height(16.dp))
        infos.filter { "city" in it.tags }.forEach { info ->
            CityRow(info)
        }
        if (working) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        report?.let {
            Spacer(Modifier.height(16.dp))
            Text("Отчет", style = MaterialTheme.typography.titleLarge)
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun CityRow(info: WorkInfo) {
    val city = info.tags.first { it.startsWith("city:") }
        .removePrefix("city:")
    val temp = info.outputData.getInt(KEY_TEMP, 0)
    val text = when (info.state) {
        WorkInfo.State.SUCCEEDED -> "$city: +$temp°C"
        WorkInfo.State.FAILED -> "$city: ошибка"
        else -> "$city: загрузка…"
    }
    Text(text, style = MaterialTheme.typography.titleMedium)
}
