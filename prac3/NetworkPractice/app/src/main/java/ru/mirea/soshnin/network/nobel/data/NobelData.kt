package ru.mirea.soshnin.network.nobel.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirea.soshnin.network.BuildConfig
import ru.mirea.soshnin.network.nobel.domain.*

fun nobelHttpClient(base: String) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Logging) {
        logger = ru.mirea.soshnin.network.ui.AndroidHttpLogger
        level = LogLevel.INFO
    }
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    defaultRequest { url(base) }
}

private val codes = mapOf(
    "Physics" to "phy", "Chemistry" to "che", "Physiology or Medicine" to "med",
    "Literature" to "lit", "Peace" to "pea", "Economic Sciences" to "eco"
)

// ---------- Задание 2: публичный API ----------

@Serializable
data class PrizesResponse(val nobelPrizes: List<PrizeDto>)

@Serializable
data class PrizeDto(
    val awardYear: String,
    val category: Map<String, String>,
    val laureates: List<LaureateDto> = emptyList()
)

@Serializable
data class LaureateDto(
    val id: String,
    val fullName: Map<String, String>? = null,
    val orgName: Map<String, String>? = null,
    val portion: String = "",
    val motivation: Map<String, String> = emptyMap()
)

@Serializable
data class LaureateInfoDto(
    val birth: BirthDto? = null,
    val founded: BirthDto? = null
)

@Serializable
data class BirthDto(val date: String? = null, val place: PlaceDto? = null)

// в местах есть поля sameAs со списками, поэтому берем только en
@Serializable
data class Localized(val en: String? = null)

@Serializable
data class PlaceDto(
    val city: Localized? = null,
    val countryNow: Localized? = null,
    val country: Localized? = null
)

class PublicNobelRepository(
    private val client: HttpClient = nobelHttpClient("https://api.nobelprize.org/2.1/")
) : NobelRepository {

    override suspend fun getLaureates(year: Int?, category: String?): List<LaureateItem> {
        val response: PrizesResponse = client.get("nobelPrizes") {
            parameter("limit", 25)
            parameter("offset", 0)
            parameter("sort", "desc")
            if (year != null) parameter("nobelPrizeYear", year)
            if (category != null) parameter("nobelPrizeCategory", category)
        }.body()
        return response.nobelPrizes.flatMap { prize ->
            val name = prize.category["en"].orEmpty()
            prize.laureates.map { l ->
                LaureateItem(
                    laureateId = l.id.toInt(),
                    prizeId = 0,
                    year = prize.awardYear.toInt(),
                    category = codes[name] ?: name,
                    categoryName = name,
                    fullName = (l.fullName ?: l.orgName)?.get("en").orEmpty(),
                    motivation = l.motivation["en"].orEmpty(),
                    portion = l.portion
                )
            }
        }
    }

    // страна и место рождения - из отдельного запроса laureate/{id}
    override suspend fun getDetail(item: LaureateItem): LaureateDetail {
        val info: List<LaureateInfoDto> = client.get("laureate/${item.laureateId}").body()
        val birth = info.firstOrNull()?.let { it.birth ?: it.founded }
        val place = birth?.place
        return LaureateDetail(
            item = item,
            birthCountry = (place?.countryNow ?: place?.country)?.en,
            birthPlace = place?.city?.en,
            birthDate = birth?.date,
            portraitUrl = null // в ответе API портретов нет
        )
    }
}

// ---------- Задание 6: свой сервер ----------

@Serializable
data class ServerLaureate(val id: Int, val fullName: String, val portion: String, val motivation: String)

@Serializable
data class ServerPrize(
    val id: Int,
    val awardYear: Int,
    val category: String,
    val categoryName: String,
    val laureates: List<ServerLaureate> = emptyList()
)

@Serializable
data class LoginBody(val username: String, val password: String)

@Serializable
data class TokenDto(val token: String, val username: String)

private val Context.nobelStore by preferencesDataStore("nobel_server")
private val TOKEN = stringPreferencesKey("token")

class ServerNobelRepository(
    private val context: Context,
    private val client: HttpClient = nobelHttpClient(BuildConfig.NOBEL_SERVER)
) : NobelRepository, AccountRepository {

    private suspend fun token() = context.nobelStore.data.map { it[TOKEN] }.first()

    private suspend fun HttpRequestBuilder.auth() {
        token()?.let { bearerAuth(it) }
    }

    override suspend fun getLaureates(year: Int?, category: String?): List<LaureateItem> {
        val prizes: List<ServerPrize> = client.get("prizes") {
            auth()
            if (year != null) parameter("year", year)
            if (category != null) parameter("category", category)
        }.body()
        return prizes.flatMap { it.toItems() }
    }

    override suspend fun getDetail(item: LaureateItem): LaureateDetail {
        val prize: ServerPrize = client.get("prizes/${item.year}/${item.category}") { auth() }.body()
        val laureate = prize.toItems().first { it.laureateId == item.laureateId }
        return LaureateDetail(
            item = laureate,
            birthCountry = null, birthPlace = null, birthDate = null, portraitUrl = null,
            isFavorite = prize.id in favorites()
        )
    }

    override suspend fun isLoggedIn() = token() != null

    override suspend fun login(username: String, password: String) {
        val result: TokenDto = client.post("login") {
            contentType(ContentType.Application.Json)
            setBody(LoginBody(username, password))
        }.body()
        context.nobelStore.edit { it[TOKEN] = result.token }
    }

    override suspend fun logout() {
        context.nobelStore.edit { it.remove(TOKEN) }
    }

    override suspend fun favorites(): Set<Int> =
        client.get("users/me/prizes") { auth() }.body<List<ServerPrize>>().map { it.id }.toSet()

    override suspend fun setFavorite(prizeId: Int, favorite: Boolean) {
        if (favorite) client.post("users/me/prizes/$prizeId") { auth() }
        else client.delete("users/me/prizes/$prizeId") { auth() }
    }

    private fun ServerPrize.toItems() = laureates.map {
        LaureateItem(it.id, id, awardYear, category, categoryName, it.fullName, it.motivation, it.portion)
    }
}
