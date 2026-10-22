package ru.relay.application

import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.relay.application.abstractions.UserRepository
import ru.relay.application.contracts.UserNotFoundException
import ru.relay.application.contracts.UserService
import ru.relay.application.contracts.UsernameTakenException
import ru.relay.domain.User
import java.time.Clock
import java.time.Instant
import java.util.*

@Service
class UserServiceImpl(
    private val users: UserRepository,
    private val clock: Clock,
) : UserService {

    @Transactional
    override fun register(username: String): User {
        if (users.findByUsername(username) != null) throw UsernameTakenException(username)
        val user = User(UUID.randomUUID(), username, Instant.now(clock))
        saveUnique(user)
        log.info("User {} registered as {}", user.id, user.username)
        return user
    }

    @Transactional(readOnly = true)
    override fun getById(id: UUID): User = users.findById(id) ?: throw UserNotFoundException(id)

    @Transactional(readOnly = true)
    override fun findByUsername(username: String): User? = users.findByUsername(username)

    @Transactional
    override fun changeUsername(userId: UUID, newUsername: String): User {
        val user = getById(userId)
        if (user.username == newUsername) return user
        if (users.findByUsername(newUsername) != null) throw UsernameTakenException(newUsername)
        val oldUsername = user.username
        user.changeUsername(newUsername)
        saveUnique(user)
        log.info("User {} renamed from {} to {}", user.id, oldUsername, newUsername)
        return user
    }

    private fun saveUnique(user: User) {
        try {
            users.save(user)
        } catch (e: DuplicateKeyException) {
            log.warn("Concurrent registration of username {} rejected by unique constraint", user.username)
            throw UsernameTakenException(user.username)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(UserServiceImpl::class.java)
    }
}
