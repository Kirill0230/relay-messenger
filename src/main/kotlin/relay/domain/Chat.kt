package ru.relay.domain

import java.time.Instant
import java.util.*

sealed class Chat(
    val id: UUID,
    val createdAt: Instant,
) {
    abstract val members: List<ChatMember>

    fun isMember(userId: UUID) = members.any { it.userId == userId }

    fun memberOf(userId: UUID): ChatMember =
        members.firstOrNull { it.userId == userId } ?: throw NotAMemberException(userId)
}

class DirectChat(
    id: UUID,
    createdAt: Instant,
    val userA: UUID,
    val userB: UUID,
) : Chat(id, createdAt) {
    init {
        require(userA != userB) { "Нельзя создать чат с самим собой" }
    }

    override val members: List<ChatMember> = listOf(
        ChatMember(userA, ChatRole.MEMBER, createdAt),
        ChatMember(userB, ChatRole.MEMBER, createdAt),
    )

    fun companionOf(userId: UUID): UUID = when (userId) {
        userA -> userB
        userB -> userA
        else -> throw NotAMemberException(userId)
    }
}

class GroupChat(
    id: UUID,
    createdAt: Instant,
    title: String,
    members: List<ChatMember>,
) : Chat(id, createdAt) {

    constructor(
        id: UUID,
        createdAt: Instant,
        title: String,
        ownerId: UUID,
        memberIds: List<UUID> = emptyList(),
    ) : this(
        id,
        createdAt,
        title,
        listOf(ChatMember(ownerId, ChatRole.OWNER, createdAt)) +
            memberIds.map { ChatMember(it, ChatRole.MEMBER, createdAt) },
    )

    var title: String = validateTitle(title)
        private set

    private val membersById: MutableMap<UUID, ChatMember> =
        members.associateByTo(LinkedHashMap()) { it.userId }

    override val members: List<ChatMember>
        get() = membersById.values.toList()

    val owner: ChatMember
        get() = membersById.values.single { it.role == ChatRole.OWNER }

    init {
        require(membersById.size == members.size) { "Участники не должны повторяться" }
        require(members.count { it.role == ChatRole.OWNER } == 1) { "В группе должен быть ровно один владелец" }
    }

    fun rename(actorId: UUID, newTitle: String) {
        requirePermission(actorId, ChatRole::canRename, "переименовывать чат")
        title = validateTitle(newTitle)
    }

    fun addMember(actorId: UUID, userId: UUID, at: Instant) {
        requirePermission(actorId, ChatRole::canManageMembers, "добавлять участников")
        if (userId in membersById) throw AlreadyMemberException(userId)
        membersById[userId] = ChatMember(userId, ChatRole.MEMBER, at)
    }

    fun removeMember(actorId: UUID, userId: UUID) {
        val actor = requirePermission(actorId, ChatRole::canManageMembers, "удалять участников")
        val target = memberOf(userId)
        if (actorId == userId) {
            throw InvalidChatOperationException("Для выхода из чата используйте leave")
        }
        if (target.role == ChatRole.OWNER) {
            throw AccessDeniedException("Владельца удалить нельзя")
        }
        if (target.role == ChatRole.ADMIN && actor.role != ChatRole.OWNER) {
            throw AccessDeniedException("Удалять администраторов может только владелец")
        }
        membersById.remove(userId)
    }

    fun leave(userId: UUID) {
        val member = memberOf(userId)
        if (member.role == ChatRole.OWNER) {
            throw InvalidChatOperationException("Владелец должен передать владение перед выходом")
        }
        membersById.remove(userId)
    }

    fun changeRole(actorId: UUID, userId: UUID, newRole: ChatRole) {
        requirePermission(actorId, ChatRole::canManageRoles, "менять роли")
        if (newRole == ChatRole.OWNER) {
            throw InvalidChatOperationException("Для смены владельца используйте transferOwnership")
        }
        val target = memberOf(userId)
        if (target.role == ChatRole.OWNER) {
            throw InvalidChatOperationException("Роль владельца меняется только через transferOwnership")
        }
        membersById[userId] = target.copy(role = newRole)
    }

    fun transferOwnership(actorId: UUID, newOwnerId: UUID) {
        val actor = memberOf(actorId)
        if (actor.role != ChatRole.OWNER) {
            throw AccessDeniedException("Передать владение может только владелец")
        }
        if (actorId == newOwnerId) return
        val newOwner = memberOf(newOwnerId)
        membersById[actorId] = actor.copy(role = ChatRole.ADMIN)
        membersById[newOwnerId] = newOwner.copy(role = ChatRole.OWNER)
    }

    private fun requirePermission(
        actorId: UUID,
        permission: (ChatRole) -> Boolean,
        action: String,
    ): ChatMember {
        val actor = memberOf(actorId)
        if (!permission(actor.role)) {
            throw AccessDeniedException("Роль ${actor.role} не позволяет $action")
        }
        return actor
    }

    private fun validateTitle(value: String): String {
        val trimmed = value.trim()
        require(trimmed.length in 1..100) { "Название чата: от 1 до 100 символов" }
        return trimmed
    }
}
