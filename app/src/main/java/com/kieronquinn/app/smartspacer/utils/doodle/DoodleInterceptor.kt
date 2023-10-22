package com.kieronquinn.app.smartspacer.utils.doodle

import com.kieronquinn.app.smartspacer.utils.extensions.toBuilder
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

/**
 *  Interceptor that fixes the body to remove the broken JSON prefix
 */
class DoodleInterceptor: Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .build()
        val response = chain.proceed(newRequest)
        return response.toBuilder().apply {
            val originalBody = response.peekBody(Long.MAX_VALUE)
            val originalContent = originalBody.string()
            val newContent = if(originalContent.contains("{")){
                originalContent.substring(originalContent.indexOf("{"))
            }else ""
            body(ResponseBody.create(originalBody.contentType(), newContent))
            if(newContent.isEmpty()){
                code(404)
            }}.build()
    }

}