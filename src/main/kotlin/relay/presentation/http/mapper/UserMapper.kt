package ru.relay.presentation.http.mapper

import ru.relay.presentation.http.dto.UserResponse
import ru.relay.domain.User

fun User.toResponse() = UserResponse(
    id = id,
    username = username,
    createdAt = createdAt,
)
