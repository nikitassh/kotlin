package ru.mirea.soshnin.practice.task9

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.delay
import ru.mirea.soshnin.practice.Notifications
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

const val WEATHER_WORK = "weather"
const val NOTIFICATION_ID = 9
const val FINAL_NOTIFICATION_ID = 10
const val KEY_CITY = "city"
const val KEY_TEMP = "temp"
const val KEY_REPORT = "report"

val cities = listOf("Москва", "Лондон", "Нью-Йорк", "Токио")

// Какие города уже готовы (для текста уведомления)
object WeatherProgress {
    val done: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun text(): String {
        val ready = cities.filter { it in done }
        val left = cities - ready.toSet()
        return when {
            ready.isEmpty() ->
                "Загружаем погоду для ${cities.size} городов…"
            left.isEmpty() ->
                "Все данные получены, формируем отчет…"
            else -> "Готово: ${ready.joinToString(" и ")}, " +
                "${left.joinToString(", ")} в процессе…"
        }
    }
}

fun weatherNotification(context: Context, text: String, ongoing: Boolean) =
    Notifications.builder(context, Notifications.CHANNEL_WORK)
        .setContentTitle("Прогноз погоды")
        .setContentText(text)
        .setOngoing(ongoing)
        .setOnlyAlertOnce(true)

// Один город - один Worker, все города идут параллельно
class CityWorker(ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val city = inputData.getString(KEY_CITY)
            ?: return Result.failure()
        delay(Random.nextLong(3000, 10000)) // "сеть"
        val temp = Random.nextInt(5, 20)
        WeatherProgress.done += city
        return Result.success(
            workDataOf(KEY_CITY to city, KEY_TEMP to temp)
        )
    }
}

fun foregroundInfo(context: Context, text: String): ForegroundInfo {
    val n = weatherNotification(context, text, true).build()
    return if (Build.VERSION.SDK_INT >= 29) {
        ForegroundInfo(
            NOTIFICATION_ID, n,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    } else {
        ForegroundInfo(NOTIFICATION_ID, n)
    }
}

// Работает параллельно с городами и держит
// постоянное уведомление через setForeground
class ProgressWorker(ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        var last = ""
        repeat(120) { // не дольше минуты
            val text = WeatherProgress.text()
            if (text != last) {
                setForeground(foregroundInfo(applicationContext, text))
                last = text
            }
            if (WeatherProgress.done.size == cities.size) {
                return Result.success()
            }
            delay(500)
        }
        return Result.success()
    }
}

// Финальный Worker: собирает результаты всех городов
class ReportWorker(ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        setForeground(foregroundInfo(ctx, WeatherProgress.text()))
        delay(3000)
        // ArrayCreatingInputMerger собрал значения в массивы
        val names = inputData.getStringArray(KEY_CITY).orEmpty()
        val temps = inputData.getIntArray(KEY_TEMP) ?: IntArray(0)
        val avg = temps.average().toInt()
        val lines = names.indices.joinToString("\n") {
            "${names[it]}: +${temps[it]}°C"
        }
        // итоговое уведомление остается после завершения работы
        Notifications.show(
            ctx, FINAL_NOTIFICATION_ID,
            weatherNotification(
                ctx, "Отчет готов! Средняя температура +$avg°C", false
            )
        )
        return Result.success(
            workDataOf(KEY_REPORT to "$lines\nСредняя: +$avg°C")
        )
    }
}

fun startWeather(context: Context) {
    WeatherProgress.done.clear()
    NotificationManagerCompat.from(context)
        .cancel(FINAL_NOTIFICATION_ID)
    // уведомление показываем сразу после нажатия
    Notifications.show(
        context, NOTIFICATION_ID,
        weatherNotification(context, WeatherProgress.text(), true)
    )
    val progress = OneTimeWorkRequestBuilder<ProgressWorker>()
        .addTag("progress")
        .build()
    val cityWork = cities.map {
        OneTimeWorkRequestBuilder<CityWorker>()
            .setInputData(workDataOf(KEY_CITY to it))
            .addTag("city")
            .addTag("city:$it")
            .build()
    }
    val report = OneTimeWorkRequestBuilder<ReportWorker>()
        .setInputMerger(ArrayCreatingInputMerger::class)
        .addTag("report")
        .build()
    WorkManager.getInstance(context)
        .beginUniqueWork(
            WEATHER_WORK, ExistingWorkPolicy.REPLACE,
            cityWork + progress
        )
        .then(report)
        .enqueue()
}
