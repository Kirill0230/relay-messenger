package ru.relay.application.contracts

import java.util.*

sealed class NotFoundException(message: String) : RuntimeException(message)

class UserNotFoundException(message: String) : NotFoundException(message) {
    constructor(id: UUID) : this("Пользователь $id не найден")
}

class ChatNotFoundException(id: UUID) : NotFoundException("Чат $id не найден")

class UsernameTakenException(username: String) : RuntimeException("Имя $username уже занято")
