package ru.relay.application

import org.junit.jupiter.api.assertThrows
import ru.relay.application.contracts.UserNotFoundException
import ru.relay.application.contracts.UsernameTakenException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserServiceTest {
    private val now = Instant.parse("2026-10-20T10:00:00Z")
    private val repository = InMemoryUserRepository()
    private val service = UserServiceImpl(repository, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `register saves user with time from clock`() {
        val user = service.register("alice")

        assertEquals(now, user.createdAt)
        assertEquals(user.id, repository.findByUsername("alice")?.id)
    }

    @Test
    fun `register rejects taken username`() {
        service.register("alice")

        assertThrows<UsernameTakenException> { service.register("alice") }
    }

    @Test
    fun `register rejects invalid username`() {
        assertThrows<IllegalArgumentException> { service.register("Alice!") }
    }

    @Test
    fun `change username persists and frees old name`() {
        val user = service.register("alice")

        service.changeUsername(user.id, "alice_new")

        assertEquals("alice_new", service.getById(user.id).username)
        assertNull(service.findByUsername("alice"))
    }

    @Test
    fun `change username to taken name fails and keeps old name`() {
        val alice = service.register("alice")
        service.register("bob")

        assertThrows<UsernameTakenException> { service.changeUsername(alice.id, "bob") }
        assertEquals("alice", service.getById(alice.id).username)
    }

    @Test
    fun `unknown user is not found`() {
        assertThrows<UserNotFoundException> { service.getById(UUID.randomUUID()) }
    }
}
