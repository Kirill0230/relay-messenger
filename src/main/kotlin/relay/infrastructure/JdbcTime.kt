package ru.relay.infrastructure

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal fun Instant.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)
