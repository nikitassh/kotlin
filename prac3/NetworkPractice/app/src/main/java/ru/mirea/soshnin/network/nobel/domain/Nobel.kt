package ru.mirea.soshnin.network.nobel.domain

// Один лауреат одной премии - элемент списка
data class LaureateItem(
    val laureateId: Int,
    val prizeId: Int,          // id премии на своем сервере (0 для публичного API)
    val year: Int,
    val category: String,      // phy, che, med, lit, pea, eco
    val categoryName: String,  // Physics, Chemistry ...
    val fullName: String,
    val motivation: String,
    val portion: String
) {
    val key get() = "$year-$category-$laureateId"
}

data class LaureateDetail(
    val item: LaureateItem,
    val birthCountry: String?,
    val birthPlace: String?,
    val birthDate: String?,
    val portraitUrl: String?,
    val isFavorite: Boolean = false
)

val categories = linkedMapOf(
    "" to "Все категории",
    "phy" to "Physics",
    "che" to "Chemistry",
    "med" to "Medicine",
    "lit" to "Literature",
    "pea" to "Peace",
    "eco" to "Economics"
)

interface NobelRepository {
    suspend fun getLaureates(year: Int?, category: String?): List<LaureateItem>
    suspend fun getDetail(item: LaureateItem): LaureateDetail
}

// Только для своего сервера: вход и избранное
interface AccountRepository {
    suspend fun isLoggedIn(): Boolean
    suspend fun login(username: String, password: String)
    suspend fun logout()
    suspend fun favorites(): Set<Int>
    suspend fun setFavorite(prizeId: Int, favorite: Boolean)
}

class GetLaureatesUseCase(private val repo: NobelRepository) {
    suspend operator fun invoke(year: Int?, category: String?) =
        repo.getLaureates(year, category?.ifBlank { null })
}

class GetLaureateDetailUseCase(private val repo: NobelRepository) {
    suspend operator fun invoke(item: LaureateItem) = repo.getDetail(item)
}
