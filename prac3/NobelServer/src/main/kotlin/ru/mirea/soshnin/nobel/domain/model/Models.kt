package ru.mirea.soshnin.nobel.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Laureate(
    val id: Int,
    val fullName: String,
    val portion: String,
    val motivation: String,
    val portraitUrl: String? = null
)

@Serializable
data class NobelPrize(
    val id: Int = 0,
    val awardYear: Int,
    val category: String,      // phy, che, med, lit, pea, eco
    val categoryName: String,  // Physics, Chemistry ...
    val fullName: String,
    val dateAwarded: String = "",
    val prizeAmount: Int = 0,
    val laureates: List<Laureate> = emptyList()
)

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val role: String
)
