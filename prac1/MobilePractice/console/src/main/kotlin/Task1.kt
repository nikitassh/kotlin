import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

// Задание 1. Параллельная загрузка трех источников данных
val json = Json { ignoreUnknownKeys = true }
var failChance = 0.2 // вероятность случайного сбоя

fun maybeFail(source: String) {
    if (Random.nextDouble() < failChance) {
        throw IllegalStateException("сервер $source не ответил")
    }
}

suspend fun loadUsers(): List<String> {
    delay(1800)
    maybeFail("пользователей")
    val text = File("data/users.json").readText()
    return json.decodeFromString<List<User>>(text).map { it.name }
}

suspend fun loadSales(): Map<String, Int> {
    delay(1200)
    maybeFail("продаж")
    val text = File("data/sales.json").readText()
    val sales = json.decodeFromString<Sales>(text)
    return sales.items.associate { it.product to it.revenue }
}

suspend fun loadWeather(): List<String> {
    delay(2500)
    maybeFail("погоды")
    val text = File("data/weather.json").readText()
    return json.decodeFromString<List<Weather>>(text)
        .map { "${it.city}: ${it.temp}°C" }
}

// Оборачиваем загрузку, чтобы ошибка не роняла программу
suspend fun <T> safe(name: String, block: suspend () -> T) =
    try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(Exception("$name: ${e.message}"))
    }

fun main(args: Array<String>) = runBlocking {
    if ("fail" in args) failChance = 1.0 / 3
    if ("ok" in args) failChance = 0.0

    val time = measureTimeMillis {
        val users = async(Dispatchers.IO) {
            safe("Пользователи") { loadUsers() }
        }
        val sales = async(Dispatchers.IO) {
            safe("Продажи") { loadSales() }
        }
        val weather = async(Dispatchers.IO) {
            safe("Погода") { loadWeather() }
        }
        val results = awaitAll(users, sales, weather)

        println("=== Результаты загрузки ===")
        users.await().onSuccess { println("Пользователи: $it") }
        sales.await().onSuccess { println("Продажи: $it") }
        weather.await().onSuccess { println("Погода: $it") }

        val errors = results.mapNotNull { it.exceptionOrNull() }
        if (errors.isNotEmpty()) {
            println("Часть данных не загрузилась:")
            errors.forEach { println("  - ${it.message}") }
        }
    }
    println("Общее время: $time мс")
}
