package ru.relay.domain

import java.time.Instant
import java.util.*

class User(
    val id: UUID,
    username: String,
    val createdAt: Instant,
) {
    var username: String = validateUsername(username)
        private set

    fun changeUsername(newUsername: String) {
        username = validateUsername(newUsername)
    }

    private companion object {
        val USERNAME_REGEX = Regex("^[a-z0-9_]{3,32}$")

        fun validateUsername(value: String): String {
            require(USERNAME_REGEX.matches(value)) { "username: 3–32 символа, латиница в нижнем регистре, цифры и _" }
            return value
        }
    }
}