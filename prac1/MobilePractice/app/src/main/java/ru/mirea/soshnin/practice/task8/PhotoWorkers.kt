package ru.mirea.soshnin.practice.task8

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val KEY_URI = "uri"
const val KEY_PATH = "path"
const val KEY_FAIL = "fail"
const val KEY_ERROR = "error"
const val KEY_PROGRESS = "progress"

// Общая часть трех шагов: имитация прогресса 0-100%
abstract class StepWorker(ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    suspend fun progress(stepMs: Long = 150) {
        for (p in 0..100 step 5) {
            setProgress(workDataOf(KEY_PROGRESS to p))
            delay(stepMs)
        }
    }

    fun fail(message: String) =
        Result.failure(workDataOf(KEY_ERROR to message))

    fun saveJpeg(bitmap: Bitmap, name: String, quality: Int): File {
        val file = File(applicationContext.cacheDir, name)
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
        }
        return file
    }
}

// Шаг 1. Сжатие фото
class CompressWorker(ctx: Context, params: WorkerParameters) :
    StepWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val uri = Uri.parse(inputData.getString(KEY_URI))
        val source = try {
            applicationContext.contentResolver.openInputStream(uri)
                .use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        } ?: return fail("Не удалось открыть фото")
        progress()
        // уменьшаем до 1280 px по большей стороне
        val scale = 1280f / maxOf(source.width, source.height)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source, (source.width * scale).toInt(),
                (source.height * scale).toInt(), true
            )
        } else source
        val file = saveJpeg(small, "compressed.jpg", 70)
        return Result.success(workDataOf(KEY_PATH to file.path))
    }
}

// Шаг 2. Водяной знак
class WatermarkWorker(ctx: Context, params: WorkerParameters) :
    StepWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PATH)
            ?: return fail("Нет файла после сжатия")
        progress()
        if (inputData.getBoolean(KEY_FAIL, false)) {
            return fail("Ошибка при наложении водяного знака")
        }
        val bitmap = BitmapFactory.decodeFile(path)
            .copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = bitmap.width / 18f
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        canvas.drawText(
            "MIREA | Soshnin N.A.", 24f,
            bitmap.height - 32f, paint
        )
        val file = saveJpeg(bitmap, "watermark.jpg", 90)
        return Result.success(workDataOf(KEY_PATH to file.path))
    }
}

// Шаг 3. "Загрузка в облако" - копируем в папку cloud
class UploadWorker(ctx: Context, params: WorkerParameters) :
    StepWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PATH)
            ?: return fail("Нет файла для загрузки")
        progress(250)
        val cloud = File(applicationContext.filesDir, "cloud")
        cloud.mkdirs()
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date())
        val target = File(cloud, "photo_$time.jpg")
        File(path).copyTo(target, overwrite = true)
        return Result.success(workDataOf(KEY_PATH to target.path))
    }
}
