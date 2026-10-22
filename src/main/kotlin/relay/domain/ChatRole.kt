package ru.relay.domain

enum class ChatRole {
    OWNER,
    ADMIN,
    MEMBER;

    val canRename get() = this == OWNER || this == ADMIN
    val canManageMembers get() = this == OWNER || this == ADMIN
    val canManageRoles get() = this == OWNER
}