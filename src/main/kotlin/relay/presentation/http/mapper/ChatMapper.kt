package ru.relay.presentation.http.mapper

import ru.relay.presentation.http.dto.ChatMemberResponse
import ru.relay.presentation.http.dto.ChatResponse
import ru.relay.presentation.http.dto.ChatType
import ru.relay.domain.Chat
import ru.relay.domain.ChatMember
import ru.relay.domain.DirectChat
import ru.relay.domain.GroupChat

fun Chat.toResponse() = ChatResponse(
    id = id,
    type = when (this) {
        is DirectChat -> ChatType.DIRECT
        is GroupChat -> ChatType.GROUP
    },
    title = (this as? GroupChat)?.title,
    createdAt = createdAt,
    members = members.map { it.toResponse() },
)

fun ChatMember.toResponse() = ChatMemberResponse(
    userId = userId,
    role = role,
    joinedAt = joinedAt,
)
