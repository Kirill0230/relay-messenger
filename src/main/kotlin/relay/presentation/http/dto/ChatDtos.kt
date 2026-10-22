package ru.relay.presentation.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.relay.domain.ChatRole
import java.time.Instant
import java.util.*

data class CreateGroupRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    val memberIds: List<UUID> = emptyList(),
)

data class OpenDirectChatRequest(
    val userId: UUID,
)

data class RenameGroupRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
)

data class AddMemberRequest(
    val userId: UUID,
)

data class ChangeRoleRequest(
    val role: ChatRole,
)

data class TransferOwnershipRequest(
    val newOwnerId: UUID,
)

enum class ChatType { DIRECT, GROUP }

data class ChatResponse(
    val id: UUID,
    val type: ChatType,
    val title: String?,
    val createdAt: Instant,
    val members: List<ChatMemberResponse>,
)

data class ChatMemberResponse(
    val userId: UUID,
    val role: ChatRole,
    val joinedAt: Instant,
)
