package ru.mirea.soshnin.storage.diary

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DiaryEntry(
    val fileName: String,
    val title: String,
    val text: String,
    val time: Long
)

// Каждая запись - отдельный файл в приватной папке filesDir/diary
class DiaryRepository(context: Context) {
    private val dir = File(context.filesDir, "diary").apply { mkdirs() }

    // Полное сканирование папки - только при запуске
    suspend fun loadAll(): List<DiaryEntry> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "txt" }
            .orEmpty()
            .map { read(it) }
            .sortedByDescending { it.time }
    }

    suspend fun save(
        title: String, text: String, old: DiaryEntry? = null
    ): DiaryEntry = withContext(Dispatchers.IO) {
        // при редактировании время создания сохраняем
        val time = old?.time ?: System.currentTimeMillis()
        val file = File(dir, fileName(time, title))
        file.writeText(text)
        if (old != null && old.fileName != file.name) {
            File(dir, old.fileName).delete()
        }
        DiaryEntry(file.name, title.trim(), text, time)
    }

    suspend fun delete(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        File(dir, entry.fileName).delete()
    }

    // имя файла: timestamp_заголовок.txt
    private fun fileName(time: Long, title: String): String {
        val safe = title.trim()
            .replace(Regex("[^\\p{L}\\p{N} _-]"), "")
            .replace(' ', '_')
            .take(40)
        return if (safe.isEmpty()) "$time.txt" else "${time}_$safe.txt"
    }

    private fun read(file: File): DiaryEntry {
        val name = file.nameWithoutExtension
        val time = name.substringBefore('_').toLongOrNull()
            ?: file.lastModified()
        val title = if ('_' in name) {
            name.substringAfter('_').replace('_', ' ')
        } else ""
        return DiaryEntry(file.name, title, file.readText(), time)
    }
}
