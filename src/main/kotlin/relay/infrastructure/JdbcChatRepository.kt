package ru.relay.infrastructure

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.relay.application.abstractions.ChatRepository
import ru.relay.domain.Chat
import ru.relay.domain.ChatMember
import ru.relay.domain.ChatRole
import ru.relay.domain.DirectChat
import ru.relay.domain.GroupChat
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*

@Repository
class JdbcChatRepository(
    private val jdbc: JdbcClient,
    private val namedJdbc: NamedParameterJdbcTemplate,
) : ChatRepository {

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Chat? = loadChat(id, forUpdate = false)

    @Transactional
    override fun findByIdForUpdate(id: UUID): Chat? = loadChat(id, forUpdate = true)

    @Transactional(readOnly = true)
    override fun findDirect(userA: UUID, userB: UUID): DirectChat? {
        val row = jdbc.sql(
            """
            SELECT c.id, c.type, c.title, c.created_at
            FROM chats c
            WHERE c.type = 'DIRECT'
              AND EXISTS (SELECT 1 FROM chat_members m WHERE m.chat_id = c.id AND m.user_id = :userA)
              AND EXISTS (SELECT 1 FROM chat_members m WHERE m.chat_id = c.id AND m.user_id = :userB)
            LIMIT 1
            """,
        )
            .param("userA", userA)
            .param("userB", userB)
            .query { rs, _ -> ChatRow.from(rs) }
            .optional()
            .orElse(null)
            ?: return null
        return row.toChat(loadMembers(listOf(row.id))[row.id].orEmpty()) as DirectChat
    }

    @Transactional(readOnly = true)
    override fun findByMember(userId: UUID): List<Chat> {
        val rows = jdbc.sql(
            """
            SELECT c.id, c.type, c.title, c.created_at
            FROM chats c
            JOIN chat_members m ON m.chat_id = c.id
            WHERE m.user_id = :userId
            ORDER BY c.created_at
            """,
        )
            .param("userId", userId)
            .query { rs, _ -> ChatRow.from(rs) }
            .list()
        if (rows.isEmpty()) return emptyList()

        val membersByChat = loadMembers(rows.map { it.id })
        return rows.map { it.toChat(membersByChat[it.id].orEmpty()) }
    }

    @Transactional
    override fun save(chat: Chat) {
        jdbc.sql(
            """
            INSERT INTO chats (id, type, title, created_at)
            VALUES (:id, :type, :title, :createdAt)
            ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title
            """,
        )
            .param("id", chat.id)
            .param("type", chat.type())
            .param("title", (chat as? GroupChat)?.title)
            .param("createdAt", chat.createdAt.toOffsetDateTime())
            .update()

        jdbc.sql("DELETE FROM chat_members WHERE chat_id = :chatId")
            .param("chatId", chat.id)
            .update()

        val batch = chat.members.map { member ->
            MapSqlParameterSource()
                .addValue("chatId", chat.id)
                .addValue("userId", member.userId)
                .addValue("role", member.role.name)
                .addValue("joinedAt", member.joinedAt.toOffsetDateTime())
        }
        namedJdbc.batchUpdate(
            """
            INSERT INTO chat_members (chat_id, user_id, role, joined_at)
            VALUES (:chatId, :userId, :role, :joinedAt)
            """,
            batch.toTypedArray(),
        )
    }

    private fun loadChat(id: UUID, forUpdate: Boolean): Chat? {
        val lock = if (forUpdate) " FOR UPDATE" else ""
        val row = jdbc.sql("SELECT id, type, title, created_at FROM chats WHERE id = :id$lock")
            .param("id", id)
            .query { rs, _ -> ChatRow.from(rs) }
            .optional()
            .orElse(null)
            ?: return null
        return row.toChat(loadMembers(listOf(id))[id].orEmpty())
    }

    private fun loadMembers(chatIds: List<UUID>): Map<UUID, List<ChatMember>> =
        jdbc.sql(
            """
            SELECT chat_id, user_id, role, joined_at
            FROM chat_members
            WHERE chat_id IN (:chatIds)
            ORDER BY joined_at, user_id
            """,
        )
            .param("chatIds", chatIds)
            .query { rs, _ ->
                rs.getObject("chat_id", UUID::class.java) to ChatMember(
                    userId = rs.getObject("user_id", UUID::class.java),
                    role = ChatRole.valueOf(rs.getString("role")),
                    joinedAt = rs.getObject("joined_at", OffsetDateTime::class.java).toInstant(),
                )
            }
            .list()
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    private fun Chat.type(): String = when (this) {
        is DirectChat -> DIRECT
        is GroupChat -> GROUP
    }

    private data class ChatRow(
        val id: UUID,
        val type: String,
        val title: String?,
        val createdAt: OffsetDateTime,
    ) {
        fun toChat(members: List<ChatMember>): Chat = when (type) {
            DIRECT -> {
                check(members.size == 2) { "Личный чат $id: ожидалось 2 участника, найдено ${members.size}" }
                DirectChat(id, createdAt.toInstant(), members[0].userId, members[1].userId)
            }
            GROUP -> GroupChat(id, createdAt.toInstant(), checkNotNull(title) { "У группы $id нет названия" }, members)
            else -> error("Неизвестный тип чата: $type")
        }

        companion object {
            fun from(rs: ResultSet) = ChatRow(
                id = rs.getObject("id", UUID::class.java),
                type = rs.getString("type"),
                title = rs.getString("title"),
                createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
            )
        }
    }

    private companion object {
        const val DIRECT = "DIRECT"
        const val GROUP = "GROUP"
    }
}
