import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest

// Задание 2. Поиск дубликатов json-файлов с таймаутом

suspend fun sha256(file: File): String =
    withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                ensureActive() // проверка отмены
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        // имитация долгой обработки большого файла
        delay(300)
        digest.digest().joinToString("") { "%02x".format(it) }
    }

suspend fun findDuplicates(dir: File): Map<String, List<File>> =
    coroutineScope {
        val files = dir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()
        println("Найдено json-файлов: ${files.size}")

        files.map { file ->
            async { sha256(file) to file }
        }.awaitAll()
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
    }

fun main(args: Array<String>) = runBlocking {
    val dir = File(args.getOrElse(0) { "data" })
    val timeoutSec = args.getOrElse(1) { "5" }.toDouble()
    println("Папка: ${dir.path}, таймаут: $timeoutSec с")

    val result = withTimeoutOrNull((timeoutSec * 1000).toLong()) {
        findDuplicates(dir)
    }

    when {
        result == null -> println("Поиск прерван по таймауту")
        result.isEmpty() -> println("Дубликатов нет")
        else -> result.entries.forEachIndexed { i, (hash, files) ->
            println("Группа ${i + 1} (${hash.take(12)}...):")
            files.forEach { println("  ${it.path}") }
        }
    }
}
