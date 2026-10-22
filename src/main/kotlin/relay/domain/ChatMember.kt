package ru.relay.domain

import java.time.Instant
import java.util.*

data class ChatMember(
    val userId: UUID,
    val role: ChatRole,
    val joinedAt: Instant,
)