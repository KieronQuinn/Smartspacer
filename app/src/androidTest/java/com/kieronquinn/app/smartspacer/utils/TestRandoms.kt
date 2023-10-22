package com.kieronquinn.app.smartspacer.utils

import java.util.UUID
import kotlin.random.Random

fun randomBoolean(): Boolean {
    return Random.nextInt(0, 2) == 1
}

fun randomString(): String {
    return UUID.randomUUID().toString()
}

fun randomInt(from: Int = 0, to: Int = 10): Int {
    return Random.nextInt(from, to)
}

fun randomLong(from: Long = 0L, to: Long = 100): Long {
    return Random.nextLong(from, to)
}

fun randomDouble(from: Double = 0.0, to: Double = 100.0): Double {
    return Random.nextDouble(from, to)
}

fun randomFloat(): Float {
    return Random.nextFloat()
}

inline fun <reified E: Enum<E>> randomEnum(): E {
    return E::class.java.enumConstants!!.random()
}