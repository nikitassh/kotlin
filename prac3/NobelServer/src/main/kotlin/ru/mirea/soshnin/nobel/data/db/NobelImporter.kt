package ru.mirea.soshnin.nobel.data.db

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.mirea.soshnin.nobel.domain.model.Laureate
import ru.mirea.soshnin.nobel.domain.model.NobelPrize

@Serializable
private data class ApiResponse(val nobelPrizes: List<ApiPrize>)

@Serializable
private data class ApiPrize(
    val awardYear: String,
    val category: Map<String, String>,
    val categoryFullName: Map<String, String>,
    val dateAwarded: String = "",
    val prizeAmount: Int = 0,
    val laureates: List<ApiLaureate> = emptyList()
)

@Serializable
private data class ApiLaureate(
    val id: String,
    val fullName: Map<String, String>? = null,
    val orgName: Map<String, String>? = null,
    val portion: String = "",
    val motivation: Map<String, String> = emptyMap()
)

private val codes = mapOf(
    "Physics" to "phy", "Chemistry" to "che", "Physiology or Medicine" to "med",
    "Literature" to "lit", "Peace" to "pea", "Economic Sciences" to "eco"
)

// Первичное заполнение БД из публичного Nobel Prize API
class NobelImporter(private val repo: PostgresPrizeRepository) {
    private val log = LoggerFactory.getLogger("NobelImporter")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importIfEmpty() {
        if (repo.count() > 0) return
        val prizes = try {
            fromApi()
        } catch (e: Exception) {
            log.warn("API недоступен ({}), берем данные из prizes.json", e.message)
            fromResources()
        }
        repo.saveAll(prizes)
        log.info("В БД загружено премий: {}", prizes.size)
    }

    private suspend fun fromApi(): List<NobelPrize> {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
        }
        val response: ApiResponse = client.use {
            it.get("https://api.nobelprize.org/2.1/nobelPrizes") {
                parameter("limit", 200)
                parameter("nobelPrizeYear", 2010)
                parameter("yearTo", 2024)
                parameter("sort", "desc")
            }.body()
        }
        return response.nobelPrizes.map { p ->
            val name = p.category["en"].orEmpty()
            NobelPrize(
                awardYear = p.awardYear.toInt(),
                category = codes[name] ?: name.lowercase(),
                categoryName = name,
                fullName = p.categoryFullName["en"].orEmpty(),
                dateAwarded = p.dateAwarded,
                prizeAmount = p.prizeAmount,
                laureates = p.laureates.map { l ->
                    Laureate(
                        id = l.id.toInt(),
                        fullName = (l.fullName ?: l.orgName)?.get("en").orEmpty(),
                        portion = l.portion,
                        motivation = l.motivation["en"].orEmpty()
                    )
                }
            )
        }
    }

    private fun fromResources(): List<NobelPrize> =
        json.decodeFromString(javaClass.getResource("/prizes.json")!!.readText())
}
