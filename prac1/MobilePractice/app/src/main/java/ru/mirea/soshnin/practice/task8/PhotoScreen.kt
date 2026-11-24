package ru.mirea.soshnin.practice.task8

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import ru.mirea.soshnin.practice.ui.TaskScaffold

const val PHOTO_WORK = "photo_chain"

private val steps = mapOf(
    "compress" to "Сжимаем фото…",
    "watermark" to "Добавляем водяной знак…",
    "upload" to "Загружаем…"
)

// Последовательная цепочка: сжатие -> знак -> загрузка
fun startPhotoChain(context: Context, uri: Uri, fail: Boolean) {
    val compress = OneTimeWorkRequestBuilder<CompressWorker>()
        .setInputData(workDataOf(KEY_URI to uri.toString()))
        .addTag("compress").build()
    val watermark = OneTimeWorkRequestBuilder<WatermarkWorker>()
        .setInputData(workDataOf(KEY_FAIL to fail))
        .addTag("watermark").build()
    val upload = OneTimeWorkRequestBuilder<UploadWorker>()
        .addTag("upload").build()
    WorkManager.getInstance(context)
        .beginUniqueWork(
            PHOTO_WORK, ExistingWorkPolicy.REPLACE, compress
        )
        .then(watermark)
        .then(upload)
        .enqueue()
}

data class ChainState(
    val status: String = "Фото не выбрано",
    val progress: Float? = null,
    val result: String? = null,
    val error: String? = null
) {
    val working get() = progress != null
}

// Разбираем состояние всех трех работ цепочки
fun chainState(infos: List<WorkInfo>): ChainState {
    if (infos.isEmpty()) return ChainState()
    // если шаг упал, следующие шаги тоже становятся FAILED
    val failed = infos.filter { it.state == WorkInfo.State.FAILED }
    if (failed.isNotEmpty()) {
        val message = failed.firstNotNullOfOrNull {
            it.outputData.getString(KEY_ERROR)
        }
        return ChainState(
            status = "Ошибка",
            error = message ?: "Шаг завершился с ошибкой"
        )
    }
    infos.firstOrNull { it.state == WorkInfo.State.RUNNING }?.let {
        val step = it.tags.firstNotNullOfOrNull { t -> steps[t] }
        val p = it.progress.getInt(KEY_PROGRESS, 0)
        return ChainState(step ?: "Работаем…", p / 100f)
    }
    if (infos.all { it.state == WorkInfo.State.SUCCEEDED }) {
        val last = infos.first { "upload" in it.tags }
        return ChainState(
            status = "Готово! Фото загружено",
            result = last.outputData.getString(KEY_PATH)
        )
    }
    if (infos.any { it.state == WorkInfo.State.CANCELLED }) {
        return ChainState("Цепочка отменена")
    }
    return ChainState("Ожидание…", 0f)
}

@Composable
fun PhotoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var fail by remember { mutableStateOf(false) }
    val infos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(PHOTO_WORK)
        .collectAsState(initial = emptyList())
    val state = chainState(infos)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) startPhotoChain(context, uri, fail) }

    TaskScaffold("Задание 8. Обработка фото", onBack) {
        Spacer(Modifier.height(32.dp))
        Text(state.status, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        state.progress?.let {
            LinearProgressIndicator(
                progress = { it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            enabled = !state.working
        ) { Text("Начать обработку и загрузку") }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.toggleable(fail) { fail = it }
        ) {
            Checkbox(checked = fail, onCheckedChange = null)
            Text("Имитировать сбой на 2 шаге")
        }
        Spacer(Modifier.height(16.dp))
        state.result?.let { path ->
            Text("Файл: $path")
            val bitmap = remember(path) {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            }
            bitmap?.let {
                Image(
                    it, "Результат", Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
        state.error?.let { Text(it, color = Color(0xFFC62828)) }
    }
}
