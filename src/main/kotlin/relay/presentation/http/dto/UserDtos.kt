package ru.relay.presentation.http.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.*

data class RegisterUserRequest(
    @field:NotBlank
    val username: String,
)

data class ChangeUsernameRequest(
    @field:NotBlank
    val username: String,
)

data class UserResponse(
    val id: UUID,
    val username: String,
    val createdAt: Instant,
)
