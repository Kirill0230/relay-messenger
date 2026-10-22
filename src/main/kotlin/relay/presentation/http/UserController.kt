package ru.relay.presentation.http

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.relay.application.contracts.UserNotFoundException
import ru.relay.application.contracts.UserService
import ru.relay.presentation.http.dto.ChangeUsernameRequest
import ru.relay.presentation.http.dto.RegisterUserRequest
import ru.relay.presentation.http.dto.UserResponse
import ru.relay.presentation.http.mapper.toResponse
import java.util.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterUserRequest): UserResponse =
        userService.register(request.username).toResponse()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): UserResponse =
        userService.getById(id).toResponse()

    @GetMapping
    fun findByUsername(@RequestParam username: String): UserResponse =
        userService.findByUsername(username)?.toResponse()
            ?: throw UserNotFoundException("Пользователь $username не найден")

    @GetMapping("/me")
    fun me(@RequestHeader(USER_ID_HEADER) userId: UUID): UserResponse =
        userService.getById(userId).toResponse()

    @PatchMapping("/me/username")
    fun changeUsername(
        @RequestHeader(USER_ID_HEADER) userId: UUID,
        @Valid @RequestBody request: ChangeUsernameRequest,
    ): UserResponse =
        userService.changeUsername(userId, request.username).toResponse()
}
