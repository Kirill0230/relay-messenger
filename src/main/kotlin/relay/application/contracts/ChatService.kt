package ru.relay.application.contracts

import ru.relay.domain.Chat
import ru.relay.domain.ChatRole
import ru.relay.domain.DirectChat
import ru.relay.domain.GroupChat
import java.util.*

interface ChatService {
    fun createGroup(ownerId: UUID, title: String, memberIds: List<UUID> = emptyList()): GroupChat

    fun openDirectChat(userId: UUID, otherUserId: UUID): DirectChat

    fun getChat(userId: UUID, chatId: UUID): Chat

    fun listChats(userId: UUID): List<Chat>

    fun renameGroup(actorId: UUID, chatId: UUID, newTitle: String): GroupChat
    fun addMember(actorId: UUID, chatId: UUID, userId: UUID): GroupChat
    fun removeMember(actorId: UUID, chatId: UUID, userId: UUID): GroupChat
    fun leaveGroup(userId: UUID, chatId: UUID): GroupChat
    fun changeRole(actorId: UUID, chatId: UUID, userId: UUID, newRole: ChatRole): GroupChat
    fun transferOwnership(actorId: UUID, chatId: UUID, newOwnerId: UUID): GroupChat
}
