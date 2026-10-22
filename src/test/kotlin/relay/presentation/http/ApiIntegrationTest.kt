package ru.relay.presentation.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var json: ObjectMapper

    private fun uniqueName(prefix: String) = prefix + "_" + UUID.randomUUID().toString().take(8)

    private fun ResultActionsDsl.body(): JsonNode = json.readTree(andReturn().response.contentAsString)

    private fun register(username: String): String =
        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username": "$username"}"""
        }.andExpect { status { isCreated() } }.body()["id"].asText()

    private fun createGroup(ownerId: String, title: String, memberIds: List<String>): String =
        mvc.post("/api/chats/groups") {
            header(USER_ID_HEADER, ownerId)
            contentType = MediaType.APPLICATION_JSON
            content = json.writeValueAsString(mapOf("title" to title, "memberIds" to memberIds))
        }.andExpect { status { isCreated() } }.body()["id"].asText()

    @Test
    fun `register and fetch user`() {
        val name = uniqueName("alice")
        val id = register(name)

        mvc.get("/api/users/$id").andExpect {
            status { isOk() }
            jsonPath("$.username") { value(name) }
        }
        mvc.get("/api/users") { param("username", name) }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
        }
    }

    @Test
    fun `duplicate username returns 409`() {
        val name = uniqueName("bob")
        register(name)

        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username": "$name"}"""
        }.andExpect { status { isConflict() } }
    }

    @Test
    fun `invalid username returns 400`() {
        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username": "Bad Name!"}"""
        }.andExpect { status { isBadRequest() } }

        mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username": ""}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `group lifecycle with roles and permissions`() {
        val owner = register(uniqueName("owner"))
        val member = register(uniqueName("member"))
        val other = register(uniqueName("other"))
        val chatId = createGroup(owner, "Команда", listOf(member))

        mvc.post("/api/chats/$chatId/members") {
            header(USER_ID_HEADER, member)
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId": "$other"}"""
        }.andExpect { status { isForbidden() } }

        mvc.put("/api/chats/$chatId/members/$member/role") {
            header(USER_ID_HEADER, owner)
            contentType = MediaType.APPLICATION_JSON
            content = """{"role": "ADMIN"}"""
        }.andExpect { status { isOk() } }

        mvc.post("/api/chats/$chatId/members") {
            header(USER_ID_HEADER, member)
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId": "$other"}"""
        }.andExpect { status { isOk() } }

        mvc.patch("/api/chats/$chatId/title") {
            header(USER_ID_HEADER, member)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "Новое имя"}"""
        }.andExpect { status { isOk() } }

        val chat = mvc.get("/api/chats/$chatId") { header(USER_ID_HEADER, other) }
            .andExpect { status { isOk() } }
            .body()
        assertEquals("Новое имя", chat["title"].asText())
        assertEquals(
            mapOf(owner to "OWNER", member to "ADMIN", other to "MEMBER"),
            chat["members"].associate { it["userId"].asText() to it["role"].asText() },
        )

        mvc.post("/api/chats/$chatId/leave") { header(USER_ID_HEADER, owner) }
            .andExpect { status { isUnprocessableEntity() } }
    }

    @Test
    fun `outsider gets 403 and unknown chat gets 404`() {
        val owner = register(uniqueName("owner"))
        val outsider = register(uniqueName("outsider"))
        val chatId = createGroup(owner, "Закрытая", emptyList())

        mvc.get("/api/chats/$chatId") { header(USER_ID_HEADER, outsider) }
            .andExpect { status { isForbidden() } }
        mvc.get("/api/chats/${UUID.randomUUID()}") { header(USER_ID_HEADER, owner) }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `direct chat is reused`() {
        val a = register(uniqueName("a"))
        val b = register(uniqueName("b"))

        val first = mvc.post("/api/chats/direct") {
            header(USER_ID_HEADER, a)
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId": "$b"}"""
        }.andExpect { status { isOk() } }.body()["id"].asText()
        val second = mvc.post("/api/chats/direct") {
            header(USER_ID_HEADER, b)
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId": "$a"}"""
        }.andExpect { status { isOk() } }.body()["id"].asText()

        assertEquals(first, second)
        mvc.get("/api/chats") { header(USER_ID_HEADER, a) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }
    }

    @Test
    fun `missing user header returns 400`() {
        mvc.get("/api/chats").andExpect { status { isBadRequest() } }
    }
}
