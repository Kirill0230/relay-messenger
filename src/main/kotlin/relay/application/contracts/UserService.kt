package ru.relay.application.contracts

import ru.relay.domain.User
import java.util.*

interface UserService {
    fun register(username: String): User

    fun getById(id: UUID): User

    fun findByUsername(username: String): User?

    fun changeUsername(userId: UUID, newUsername: String): User
}
