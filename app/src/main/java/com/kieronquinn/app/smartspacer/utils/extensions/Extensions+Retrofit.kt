package com.kieronquinn.app.smartspacer.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

const val CONTENT_TYPE_PROTOBUF = "application/x-protobuf"

suspend fun <T> Call<T>.invoke(): T? = withContext(Dispatchers.IO) {
    return@withContext getSuspended()
}

private suspend fun <T> Call<T>.getSuspended() = suspendCancellableCoroutine<T?> {
    enqueue(object: Callback<T>{
        override fun onResponse(call: Call<T>, response: Response<T>) {
            it.resume(response.body())
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            it.resume(null)
        }
    })
    it.invokeOnCancellation {
        if(!this.isCanceled){
            cancel()
        }
    }
}

fun ByteArray.toRequestBody(contentType: String): RequestBody {
    return object: RequestBody() {
        override fun contentType(): MediaType? {
            return MediaType.parse(contentType)
        }

        override fun writeTo(sink: BufferedSink) {
            sink.write(this@toRequestBody)
        }
    }
}