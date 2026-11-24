package ru.mirea.soshnin.nobel.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(val token: String, val username: String, val expiresInSec: Int)

@Serializable
data class ProfileResponse(val id: Int, val username: String, val role: String)

@Serializable
data class MessageResponse(val message: String)
