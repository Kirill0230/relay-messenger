package ru.relay.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.relay.application.abstractions.ChatRepository
import ru.relay.application.abstractions.UserRepository
import ru.relay.application.contracts.ChatNotFoundException
import ru.relay.application.contracts.ChatService
import ru.relay.application.contracts.UserNotFoundException
import ru.relay.domain.*
import java.time.Clock
import java.time.Instant
import java.util.*

@Service
class ChatServiceImpl(
    private val chats: ChatRepository,
    private val users: UserRepository,
    private val clock: Clock,
) : ChatService {

    @Transactional
    override fun createGroup(ownerId: UUID, title: String, memberIds: List<UUID>): GroupChat {
        requireUsersExist(listOf(ownerId) + memberIds)
        val group = GroupChat(UUID.randomUUID(), now(), title, ownerId, memberIds.distinct())
        chats.save(group)
        log.info("Group {} created by {} with {} members", group.id, ownerId, group.members.size)
        return group
    }

    @Transactional
    override fun openDirectChat(userId: UUID, otherUserId: UUID): DirectChat {
        requireUsersExist(listOf(userId, otherUserId))
        chats.findDirect(userId, otherUserId)?.let { return it }
        val direct = DirectChat(UUID.randomUUID(), now(), userId, otherUserId)
        chats.save(direct)
        log.info("Direct chat {} created between {} and {}", direct.id, userId, otherUserId)
        return direct
    }

    @Transactional(readOnly = true)
    override fun getChat(userId: UUID, chatId: UUID): Chat {
        val chat = chats.findById(chatId) ?: throw ChatNotFoundException(chatId)
        if (!chat.isMember(userId)) throw NotAMemberException(userId)
        return chat
    }

    @Transactional(readOnly = true)
    override fun listChats(userId: UUID): List<Chat> = chats.findByMember(userId)

    @Transactional
    override fun renameGroup(actorId: UUID, chatId: UUID, newTitle: String): GroupChat =
        updateGroup(chatId, "renamed by $actorId") { it.rename(actorId, newTitle) }

    @Transactional
    override fun addMember(actorId: UUID, chatId: UUID, userId: UUID): GroupChat {
        requireUsersExist(listOf(userId))
        return updateGroup(chatId, "member $userId added by $actorId") { it.addMember(actorId, userId, now()) }
    }

    @Transactional
    override fun removeMember(actorId: UUID, chatId: UUID, userId: UUID): GroupChat =
        updateGroup(chatId, "member $userId removed by $actorId") { it.removeMember(actorId, userId) }

    @Transactional
    override fun leaveGroup(userId: UUID, chatId: UUID): GroupChat =
        updateGroup(chatId, "member $userId left") { it.leave(userId) }

    @Transactional
    override fun changeRole(actorId: UUID, chatId: UUID, userId: UUID, newRole: ChatRole): GroupChat =
        updateGroup(chatId, "role of $userId changed to $newRole by $actorId") { it.changeRole(actorId, userId, newRole) }

    @Transactional
    override fun transferOwnership(actorId: UUID, chatId: UUID, newOwnerId: UUID): GroupChat =
        updateGroup(chatId, "ownership transferred from $actorId to $newOwnerId") { it.transferOwnership(actorId, newOwnerId) }

    private fun updateGroup(chatId: UUID, action: String, change: (GroupChat) -> Unit): GroupChat {
        val chat = chats.findByIdForUpdate(chatId) ?: throw ChatNotFoundException(chatId)
        val group = chat as? GroupChat
            ?: throw InvalidChatOperationException("Операция доступна только в групповом чате")
        change(group)
        chats.save(group)
        log.info("Group {}: {}", chatId, action)
        return group
    }

    private fun requireUsersExist(ids: List<UUID>) {
        ids.distinct().forEach { id -> users.findById(id) ?: throw UserNotFoundException(id) }
    }

    private fun now(): Instant = Instant.now(clock)

    private companion object {
        val log = LoggerFactory.getLogger(ChatServiceImpl::class.java)
    }
}
