package ru.relay.application.abstractions

import ru.relay.domain.Chat
import ru.relay.domain.DirectChat
import java.util.*

interface ChatRepository {
    fun findById(id: UUID): Chat?

    fun findByIdForUpdate(id: UUID): Chat?

    fun findByMember(userId: UUID): List<Chat>
    fun findDirect(userA: UUID, userB: UUID): DirectChat?
    fun save(chat: Chat)
}
