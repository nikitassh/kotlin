package ru.mirea.soshnin.nobel.data.memory

import kotlinx.serialization.json.Json
import ru.mirea.soshnin.nobel.domain.model.NobelPrize
import ru.mirea.soshnin.nobel.domain.model.User
import ru.mirea.soshnin.nobel.domain.repository.FavoriteRepository
import ru.mirea.soshnin.nobel.domain.repository.PrizeRepository
import ru.mirea.soshnin.nobel.domain.repository.UserRepository
import ru.mirea.soshnin.nobel.domain.usecase.hashPassword
import java.util.concurrent.ConcurrentHashMap

// Задание 4: данные лежат в памяти, загружаются из resources/prizes.json
class InMemoryPrizeRepository : PrizeRepository {
    private val prizes: List<NobelPrize> = run {
        val text = javaClass.getResource("/prizes.json")!!.readText()
        Json { ignoreUnknownKeys = true }
            .decodeFromString<List<NobelPrize>>(text)
            .mapIndexed { i, p -> p.copy(id = i + 1) }
    }

    override suspend fun getAll(year: Int?, category: String?) =
        prizes.filter {
            (year == null || it.awardYear == year) &&
                (category == null || it.category == category)
        }

    override suspend fun get(year: Int, category: String) =
        prizes.find { it.awardYear == year && it.category == category }

    override suspend fun getById(id: Int) = prizes.find { it.id == id }
}

class InMemoryUserRepository : UserRepository {
    private val users = listOf(
        User(1, "student", hashPassword("student123"), "user"),
        User(2, "admin", hashPassword("admin123"), "admin")
    )

    override suspend fun findByUsername(username: String) =
        users.find { it.username == username }

    override suspend fun findById(id: Int) = users.find { it.id == id }
}

class InMemoryFavoriteRepository(
    private val prizes: PrizeRepository
) : FavoriteRepository {
    private val data = ConcurrentHashMap<Int, MutableSet<Int>>()

    override suspend fun list(userId: Int) =
        data[userId].orEmpty().mapNotNull { prizes.getById(it) }

    override suspend fun add(userId: Int, prizeId: Int) =
        data.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(prizeId)

    override suspend fun remove(userId: Int, prizeId: Int) =
        data[userId]?.remove(prizeId) ?: false
}
