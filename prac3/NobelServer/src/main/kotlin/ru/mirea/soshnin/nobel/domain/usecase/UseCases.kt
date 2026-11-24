package ru.mirea.soshnin.nobel.domain.usecase

import at.favre.lib.crypto.bcrypt.BCrypt
import ru.mirea.soshnin.nobel.domain.model.User
import ru.mirea.soshnin.nobel.domain.repository.FavoriteRepository
import ru.mirea.soshnin.nobel.domain.repository.PrizeRepository
import ru.mirea.soshnin.nobel.domain.repository.UserRepository

class LoginUseCase(private val users: UserRepository) {
    // null - неверный логин или пароль
    suspend operator fun invoke(username: String, password: String): User? {
        val user = users.findByUsername(username) ?: return null
        val ok = BCrypt.verifyer()
            .verify(password.toCharArray(), user.passwordHash).verified
        return if (ok) user else null
    }
}

class GetPrizesUseCase(private val prizes: PrizeRepository) {
    suspend operator fun invoke(year: Int?, category: String?) =
        prizes.getAll(year, category?.lowercase())
}

class GetPrizeUseCase(private val prizes: PrizeRepository) {
    suspend operator fun invoke(year: Int, category: String) =
        prizes.get(year, category.lowercase())
}

class GetLaureatesUseCase(private val prizes: PrizeRepository) {
    suspend operator fun invoke(year: Int, category: String) =
        prizes.get(year, category.lowercase())?.laureates
}

class GetProfileUseCase(private val users: UserRepository) {
    suspend operator fun invoke(id: Int) = users.findById(id)
}

class FavoritesUseCase(
    private val favorites: FavoriteRepository,
    private val prizes: PrizeRepository
) {
    suspend fun list(userId: Int) = favorites.list(userId)

    // false - такой премии нет
    suspend fun add(userId: Int, prizeId: Int): Boolean {
        if (prizes.getById(prizeId) == null) return false
        favorites.add(userId, prizeId)
        return true
    }

    suspend fun remove(userId: Int, prizeId: Int) = favorites.remove(userId, prizeId)
}

fun hashPassword(password: String): String =
    BCrypt.withDefaults().hashToString(10, password.toCharArray())
