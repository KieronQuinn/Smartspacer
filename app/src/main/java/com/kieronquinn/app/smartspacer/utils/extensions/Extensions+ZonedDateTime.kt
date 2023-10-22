package com.kieronquinn.app.smartspacer.utils.extensions

import java.time.ZonedDateTime

fun ZonedDateTime.atStartOfMinute(): ZonedDateTime {
    return withSecond(0).withNano(0)
}