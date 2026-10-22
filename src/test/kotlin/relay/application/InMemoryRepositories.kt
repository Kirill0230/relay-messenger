package ru.relay.application

import ru.relay.application.abstractions.ChatRepository
import ru.relay.application.abstractions.UserRepository
import ru.relay.domain.Chat
import ru.relay.domain.DirectChat
import ru.relay.domain.GroupChat
import ru.relay.domain.User
import java.util.*

class InMemoryUserRepository : UserRepository {
    private val byId = LinkedHashMap<UUID, User>()

    override fun findById(id: UUID) = byId[id]?.copy()
    override fun findByUsername(username: String) = byId.values.firstOrNull { it.username == username }?.copy()
    override fun save(user: User) {
        byId[user.id] = user.copy()
    }

    private fun User.copy() = User(id, username, createdAt)
}

class InMemoryChatRepository : ChatRepository {
    private val byId = LinkedHashMap<UUID, Chat>()

    override fun findById(id: UUID) = byId[id]?.copy()
    override fun findByIdForUpdate(id: UUID) = findById(id)
    override fun findByMember(userId: UUID) = byId.values.filter { it.isMember(userId) }.map { it.copy() }
    override fun findDirect(userA: UUID, userB: UUID) = byId.values
        .filterIsInstance<DirectChat>()
        .firstOrNull { it.isMember(userA) && it.isMember(userB) }
        ?.copy() as DirectChat?

    override fun save(chat: Chat) {
        byId[chat.id] = chat.copy()
    }

    private fun Chat.copy(): Chat = when (this) {
        is DirectChat -> DirectChat(id, createdAt, userA, userB)
        is GroupChat -> GroupChat(id, createdAt, title, members)
    }
}
