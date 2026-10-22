package ru.relay.infrastructure

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.relay.application.abstractions.UserRepository
import ru.relay.domain.User
import java.time.OffsetDateTime
import java.util.*

@Repository
class JdbcUserRepository(
    private val jdbc: JdbcClient,
) : UserRepository {

    override fun findById(id: UUID): User? =
        jdbc.sql("SELECT id, username, created_at FROM users WHERE id = :id")
            .param("id", id)
            .query(userMapper)
            .optional()
            .orElse(null)

    override fun findByUsername(username: String): User? =
        jdbc.sql("SELECT id, username, created_at FROM users WHERE username = :username")
            .param("username", username)
            .query(userMapper)
            .optional()
            .orElse(null)

    override fun save(user: User) {
        jdbc.sql(
            """
            INSERT INTO users (id, username, created_at)
            VALUES (:id, :username, :createdAt)
            ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username
            """,
        )
            .param("id", user.id)
            .param("username", user.username)
            .param("createdAt", user.createdAt.toOffsetDateTime())
            .update()
    }

    private val userMapper = RowMapper { rs, _ ->
        User(
            id = rs.getObject("id", UUID::class.java),
            username = rs.getString("username"),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
        )
    }
}
