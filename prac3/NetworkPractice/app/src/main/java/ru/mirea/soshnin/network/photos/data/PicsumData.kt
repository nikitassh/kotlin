package ru.mirea.soshnin.network.photos.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.mirea.soshnin.network.photos.domain.Photo
import ru.mirea.soshnin.network.photos.domain.PhotoRepository

@Serializable
data class PhotoDto(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    @SerialName("download_url") val downloadUrl: String
)

interface PicsumApi {
    @GET("v2/list")
    suspend fun list(@Query("page") page: Int, @Query("limit") limit: Int = 30): List<PhotoDto>

    @GET("id/{id}/info")
    suspend fun info(@Path("id") id: String): PhotoDto
}

object PicsumClient {
    val http = OkHttpClient()
    val api: PicsumApi = Retrofit.Builder()
        .baseUrl("https://picsum.photos/")
        .client(http)
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(PicsumApi::class.java)
}

private fun PhotoDto.toDomain() = Photo(id, author, width, height, url, downloadUrl)

class PhotoRepositoryImpl(
    private val api: PicsumApi = PicsumClient.api,
    private val http: OkHttpClient = PicsumClient.http
) : PhotoRepository {
    override suspend fun getPhotos(page: Int) = api.list(page).map { it.toDomain() }

    override suspend fun getPhoto(id: String) = api.info(id).toDomain()

    override suspend fun download(photo: Photo): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(photo.downloadUrl).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Ошибка загрузки: ${response.code}")
            response.body!!.bytes()
        }
    }
}
