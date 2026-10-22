package ru.relay.domain

import java.util.*

sealed class ChatDomainException(message: String) : RuntimeException(message)

class NotAMemberException(userId: UUID) :
    ChatDomainException("Пользователь $userId не состоит в чате")

class AlreadyMemberException(userId: UUID) :
    ChatDomainException("Пользователь $userId уже состоит в чате")

class AccessDeniedException(message: String) : ChatDomainException(message)

class InvalidChatOperationException(message: String) : ChatDomainException(message)