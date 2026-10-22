package ru.relay.presentation.http

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.relay.application.contracts.NotFoundException
import ru.relay.application.contracts.UsernameTakenException
import ru.relay.domain.AccessDeniedException
import ru.relay.domain.AlreadyMemberException
import ru.relay.domain.ChatDomainException
import ru.relay.domain.InvalidChatOperationException
import ru.relay.domain.NotAMemberException

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun notFound(e: NotFoundException) = problem(HttpStatus.NOT_FOUND, e)

    @ExceptionHandler(UsernameTakenException::class)
    fun usernameTaken(e: UsernameTakenException) = problem(HttpStatus.CONFLICT, e)

    @ExceptionHandler(ChatDomainException::class)
    fun chatDomain(e: ChatDomainException) = problem(
        status = when (e) {
            is NotAMemberException -> HttpStatus.FORBIDDEN
            is AccessDeniedException -> HttpStatus.FORBIDDEN
            is AlreadyMemberException -> HttpStatus.CONFLICT
            is InvalidChatOperationException -> HttpStatus.UNPROCESSABLE_ENTITY
        },
        e = e,
    )

    @ExceptionHandler(IllegalArgumentException::class)
    fun badArgument(e: IllegalArgumentException) = problem(HttpStatus.BAD_REQUEST, e)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidBody(e: MethodArgumentNotValidException) = problem(
        HttpStatus.BAD_REQUEST,
        e,
        e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" },
    )

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MissingRequestHeaderException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun badRequest(e: Exception) = problem(HttpStatus.BAD_REQUEST, e, "Некорректный запрос: ${e.message}")

    private fun problem(status: HttpStatus, e: Exception, detail: String? = e.message): ProblemDetail {
        log.info("Request rejected with {} ({}): {}", status.value(), e.javaClass.simpleName, detail)
        return ProblemDetail.forStatusAndDetail(status, detail ?: status.reasonPhrase)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
