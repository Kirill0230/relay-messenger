package ru.relay.infrastructure

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.relay.domain.ChatRole
import ru.relay.domain.DirectChat
import ru.relay.domain.GroupChat
import ru.relay.domain.User
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcUserRepository::class, JdbcChatRepository::class)
@Testcontainers(disabledWithoutDocker = true)
class JdbcRepositoriesTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var users: JdbcUserRepository

    @Autowired
    lateinit var chats: JdbcChatRepository

    private val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

    private fun newUser(name: String) = User(UUID.randomUUID(), name, now).also(users::save)

    @Test
    fun `user is saved and found by id and username`() {
        val alice = newUser("alice")

        assertEquals("alice", users.findById(alice.id)?.username)
        assertEquals(alice.id, users.findByUsername("alice")?.id)
        assertNull(users.findByUsername("nobody"))
    }

    @Test
    fun `username change is persisted`() {
        val alice = newUser("alice")
        alice.changeUsername("alice_new")
        users.save(alice)

        assertEquals("alice_new", users.findById(alice.id)?.username)
    }

    @Test
    fun `group chat keeps members and roles`() {
        val alice = newUser("alice")
        val bob = newUser("bob")
        val carol = newUser("carol")
        val group = GroupChat(UUID.randomUUID(), now, "Команда", ownerId = alice.id, memberIds = listOf(bob.id))
        group.changeRole(alice.id, bob.id, ChatRole.ADMIN)
        group.addMember(bob.id, carol.id, now)
        chats.save(group)

        val loaded = assertIs<GroupChat>(chats.findById(group.id))
        assertEquals("Команда", loaded.title)
        assertEquals(alice.id, loaded.owner.userId)
        assertEquals(
            mapOf(alice.id to ChatRole.OWNER, bob.id to ChatRole.ADMIN, carol.id to ChatRole.MEMBER),
            loaded.members.associate { it.userId to it.role },
        )
    }

    @Test
    fun `removed member disappears after save`() {
        val alice = newUser("alice")
        val bob = newUser("bob")
        val group = GroupChat(UUID.randomUUID(), now, "Команда", ownerId = alice.id, memberIds = listOf(bob.id))
        chats.save(group)

        group.removeMember(alice.id, bob.id)
        group.rename(alice.id, "Новое имя")
        chats.save(group)

        val loaded = assertIs<GroupChat>(chats.findById(group.id))
        assertEquals("Новое имя", loaded.title)
        assertEquals(listOf(alice.id), loaded.members.map { it.userId })
    }

    @Test
    fun `direct chat round trip and find by member`() {
        val alice = newUser("alice")
        val bob = newUser("bob")
        val carol = newUser("carol")
        val direct = DirectChat(UUID.randomUUID(), now, alice.id, bob.id)
        val group = GroupChat(UUID.randomUUID(), now, "Группа", ownerId = alice.id, memberIds = listOf(carol.id))
        chats.save(direct)
        chats.save(group)

        val loaded = assertIs<DirectChat>(chats.findById(direct.id))
        assertEquals(setOf(alice.id, bob.id), setOf(loaded.userA, loaded.userB))

        assertEquals(setOf(direct.id, group.id), chats.findByMember(alice.id).map { it.id }.toSet())
        assertEquals(listOf(direct.id), chats.findByMember(bob.id).map { it.id })
        assertEquals(emptyList(), chats.findByMember(UUID.randomUUID()))
    }

    @Test
    fun `unknown chat returns null`() {
        assertNull(chats.findById(UUID.randomUUID()))
    }
}
