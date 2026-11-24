package ru.mirea.soshnin.nobel.domain.repository

import ru.mirea.soshnin.nobel.domain.model.NobelPrize
import ru.mirea.soshnin.nobel.domain.model.User

interface PrizeRepository {
    suspend fun getAll(year: Int? = null, category: String? = null): List<NobelPrize>
    suspend fun get(year: Int, category: String): NobelPrize?
    suspend fun getById(id: Int): NobelPrize?
}

interface UserRepository {
    suspend fun findByUsername(username: String): User?
    suspend fun findById(id: Int): User?
}

interface FavoriteRepository {
    suspend fun list(userId: Int): List<NobelPrize>
    suspend fun add(userId: Int, prizeId: Int): Boolean
    suspend fun remove(userId: Int, prizeId: Int): Boolean
}
