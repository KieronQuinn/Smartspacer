package com.kieronquinn.app.smartspacer.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow

/**
 *  For use on StateFlows where the value may not be set immediately. Loops incoming items, checking
 *  if they match [O]. Standard Turbine [test] rules apply for timeouts on awaitItem, so if type [O]
 *  is not emitted, it will throw an error.
 */
suspend inline fun <T, reified O> Flow<T>.assertOutputs() = test {
    while(true){
        if(awaitItem() is O) break
    }
}

/**
 *  Slightly hacky - for use when a flow may emit two updates, with the latter being the one we
 *  want, but does not always. This tries to wait for two, but if the second one never comes
 *  returns the first.
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemOrTwo(): T {
    val item = awaitItem()
    val possibleSecondItem = try {
        awaitItem()
    }catch (e: Error){
        null
    }
    return possibleSecondItem ?: item
}