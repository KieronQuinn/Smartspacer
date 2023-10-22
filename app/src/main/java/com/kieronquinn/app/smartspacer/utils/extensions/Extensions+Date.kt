package com.kieronquinn.app.smartspacer.utils.extensions

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

fun LocalDateTime.toDate(): Date {
    return Date.from(toInstant(ZonedDateTime.now().offset))
}