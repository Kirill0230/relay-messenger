package ru.relay.presentation.http

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.relay.application.contracts.ChatService
import ru.relay.presentation.http.dto.AddMemberRequest
import ru.relay.presentation.http.dto.ChangeRoleRequest
import ru.relay.presentation.http.dto.ChatResponse
import ru.relay.presentation.http.dto.CreateGroupRequest
import ru.relay.presentation.http.dto.OpenDirectChatRequest
import ru.relay.presentation.http.dto.RenameGroupRequest
import ru.relay.presentation.http.dto.TransferOwnershipRequest
import ru.relay.presentation.http.mapper.toResponse
import java.util.*

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService,
) {

    @GetMapping
    fun myChats(@RequestHeader(USER_ID_HEADER) userId: UUID): List<ChatResponse> =
        chatService.listChats(userId).map { it.toResponse() }

    @GetMapping("/{chatId}")
    fun getChat(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
    ): ChatResponse =
        chatService.getChat(userId, chatId).toResponse()

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroup(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @Valid @RequestBody request: CreateGroupRequest,
    ): ChatResponse =
        chatService.createGroup(userId, request.title, request.memberIds).toResponse()

    @PostMapping("/direct")
    fun openDirectChat(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @RequestBody request: OpenDirectChatRequest,
    ): ChatResponse =
        chatService.openDirectChat(userId, request.userId).toResponse()

    @PatchMapping("/{chatId}/title")
    fun renameGroup(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
        @Valid @RequestBody request: RenameGroupRequest,
    ): ChatResponse =
        chatService.renameGroup(userId, chatId, request.title).toResponse()

    @PostMapping("/{chatId}/members")
    fun addMember(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
        @RequestBody request: AddMemberRequest,
    ): ChatResponse =
        chatService.addMember(userId, chatId, request.userId).toResponse()

    @DeleteMapping("/{chatId}/members/{memberId}")
    fun removeMember(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
        @PathVariable memberId: UUID,
    ): ChatResponse =
        chatService.removeMember(userId, chatId, memberId).toResponse()

    @PostMapping("/{chatId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
    ) {
        chatService.leaveGroup(userId, chatId)
    }

    @PutMapping("/{chatId}/members/{memberId}/role")
    fun changeRole(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
        @PathVariable memberId: UUID,
        @RequestBody request: ChangeRoleRequest,
    ): ChatResponse =
        chatService.changeRole(userId, chatId, memberId, request.role).toResponse()

    @PostMapping("/{chatId}/owner")
    fun transferOwnership(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @PathVariable chatId: UUID,
        @RequestBody request: TransferOwnershipRequest,
    ): ChatResponse =
        chatService.transferOwnership(userId, chatId, request.newOwnerId).toResponse()
}
