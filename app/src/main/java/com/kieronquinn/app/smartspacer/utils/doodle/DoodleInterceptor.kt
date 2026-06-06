package com.kieronquinn.app.smartspacer.utils.doodle

import android.content.Context
import android.webkit.WebSettings
import com.kieronquinn.app.smartspacer.utils.extensions.toBuilder
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 *  Interceptor that fixes the body to remove the broken JSON prefix
 */
class DoodleInterceptor(context: Context): Interceptor {

    private val userAgent = WebSettings.getDefaultUserAgent(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request()
            .newBuilder()
            // Doodle endpoint now checks user agent, doesn't seem to like okhttp
            .addHeader("User-Agent", userAgent)
            .build()
        val response = chain.proceed(newRequest)
        return response.toBuilder().apply {
            val originalBody = response.peekBody(Long.MAX_VALUE)
            val originalContent = originalBody.string()
            val newContent = if(originalContent.contains("{")){
                originalContent.substring(originalContent.indexOf("{"))
            }else ""
            body(newContent.toResponseBody(originalBody.contentType()))
            if(newContent.isEmpty()){
                code(404)
            }}.build()
    }

}