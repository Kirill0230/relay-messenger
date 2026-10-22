package ru.relay.application

import org.junit.jupiter.api.assertThrows
import ru.relay.application.contracts.ChatNotFoundException
import ru.relay.application.contracts.UserNotFoundException
import ru.relay.domain.AccessDeniedException
import ru.relay.domain.AlreadyMemberException
import ru.relay.domain.ChatRole
import ru.relay.domain.GroupChat
import ru.relay.domain.InvalidChatOperationException
import ru.relay.domain.NotAMemberException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChatServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC)
    private val userRepository = InMemoryUserRepository()
    private val userService = UserServiceImpl(userRepository, clock)
    private val service = ChatServiceImpl(InMemoryChatRepository(), userRepository, clock)

    private val alice = userService.register("alice").id
    private val bob = userService.register("bob").id
    private val carol = userService.register("carol").id

    @Test
    fun `group is created with owner and members`() {
        val group = service.createGroup(alice, "Команда", listOf(bob))

        val loaded = assertIs<GroupChat>(service.getChat(alice, group.id))
        assertEquals(alice, loaded.owner.userId)
        assertEquals(listOf(alice, bob), loaded.members.map { it.userId })
        assertEquals(ChatRole.MEMBER, loaded.memberOf(bob).role)
    }

    @Test
    fun `group with unknown user is rejected`() {
        assertThrows<UserNotFoundException> { service.createGroup(alice, "Команда", listOf(UUID.randomUUID())) }
    }

    @Test
    fun `direct chat is reused instead of duplicated`() {
        val first = service.openDirectChat(alice, bob)
        val second = service.openDirectChat(bob, alice)

        assertEquals(first.id, second.id)
        assertEquals(1, service.listChats(alice).size)
    }

    @Test
    fun `direct chat with yourself is rejected`() {
        assertThrows<IllegalArgumentException> { service.openDirectChat(alice, alice) }
    }

    @Test
    fun `admin can add members and rename`() {
        val group = service.createGroup(alice, "Команда", listOf(bob))

        service.changeRole(alice, group.id, bob, ChatRole.ADMIN)
        service.addMember(bob, group.id, carol)
        service.renameGroup(bob, group.id, "Новое имя")

        val loaded = assertIs<GroupChat>(service.getChat(alice, group.id))
        assertEquals("Новое имя", loaded.title)
        assertEquals(ChatRole.ADMIN, loaded.memberOf(bob).role)
        assertEquals(ChatRole.MEMBER, loaded.memberOf(carol).role)
    }

    @Test
    fun `member cannot add members and nothing is saved`() {
        val group = service.createGroup(alice, "Команда", listOf(bob))

        assertThrows<AccessDeniedException> { service.addMember(bob, group.id, carol) }
        assertEquals(listOf(alice, bob), service.getChat(alice, group.id).members.map { it.userId })
    }

    @Test
    fun `adding existing member fails`() {
        val group = service.createGroup(alice, "Команда", listOf(bob))

        assertThrows<AlreadyMemberException> { service.addMember(alice, group.id, bob) }
    }

    @Test
    fun `admin cannot remove another admin`() {
        val group = service.createGroup(alice, "Команда", listOf(bob, carol))
        service.changeRole(alice, group.id, bob, ChatRole.ADMIN)
        service.changeRole(alice, group.id, carol, ChatRole.ADMIN)

        assertThrows<AccessDeniedException> { service.removeMember(bob, group.id, carol) }
    }

    @Test
    fun `owner cannot leave without transferring ownership`() {
        val group = service.createGroup(alice, "Команда", listOf(bob))

        assertThrows<InvalidChatOperationException> { service.leaveGroup(alice, group.id) }
    }

    @Test
    fun `transfer ownership then leave`() {
        val group = service.createGroup(alice, "Команда", listOf(bob))

        service.transferOwnership(alice, group.id, bob)
        service.leaveGroup(alice, group.id)

        val loaded = assertIs<GroupChat>(service.getChat(bob, group.id))
        assertEquals(bob, loaded.owner.userId)
        assertEquals(listOf(bob), loaded.members.map { it.userId })
    }

    @Test
    fun `group operation on direct chat fails`() {
        val direct = service.openDirectChat(alice, bob)

        assertThrows<InvalidChatOperationException> { service.renameGroup(alice, direct.id, "Имя") }
    }

    @Test
    fun `outsider cannot read chat`() {
        val group = service.createGroup(alice, "Команда")

        assertThrows<NotAMemberException> { service.getChat(carol, group.id) }
    }

    @Test
    fun `unknown chat is not found`() {
        assertThrows<ChatNotFoundException> { service.getChat(alice, UUID.randomUUID()) }
    }
}
