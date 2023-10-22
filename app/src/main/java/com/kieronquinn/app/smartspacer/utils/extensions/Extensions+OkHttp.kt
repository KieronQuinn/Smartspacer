package com.kieronquinn.app.smartspacer.utils.extensions

import okhttp3.Response

fun Response.toBuilder(): Response.Builder {
    val originalResponse = this
    return Response.Builder().apply {
        request(originalResponse.request())
        protocol(originalResponse.protocol())
        code(originalResponse.code())
        message(originalResponse.message())
        handshake(originalResponse.handshake())
        headers(originalResponse.headers())
        body(originalResponse.body())
        networkResponse(originalResponse.networkResponse())
        cacheResponse(originalResponse.cacheResponse())
        priorResponse(originalResponse.priorResponse())
        sentRequestAtMillis(originalResponse.sentRequestAtMillis())
        receivedResponseAtMillis(originalResponse.receivedResponseAtMillis())
    }
}