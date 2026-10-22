package ru.relay.application.abstractions

import ru.relay.domain.User
import java.util.*

interface UserRepository {
    fun findById(id: UUID): User?
    fun findByUsername(username: String): User?
    fun save(user: User)
}