package com.kieronquinn.app.smartspacer.sdk.utils

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.BuildConfig
import java.net.URLDecoder
import java.net.URLEncoder

private const val ARG_PROXY = "proxy"
private val SMARTSPACER_PROXY_URI = Uri.Builder()
    .scheme("content")
    .authority("${BuildConfig.SMARTSPACER_PACKAGE_NAME}.proxyprovider")
    .build()

/**
 *  Creates a "proxy" Uri for a given [originalUri]. This routes content calls via Smartspacer's
 *  proxy ContentProvider, so you don't need to give access to every launcher which may wish to
 *  access your provider.
 */
fun createSmartspacerProxyUri(originalUri: Uri): Uri {
    return createSmartspacerProxyUri(originalUri, SMARTSPACER_PROXY_URI)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun createSmartspacerProxyUri(originalUri: Uri, proxyUri: Uri): Uri {
    val encodedUri = URLEncoder.encode(originalUri.toString(), Charsets.UTF_8.name())
    return proxyUri.buildUpon().appendQueryParameter(ARG_PROXY, encodedUri).build()
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Uri.getProxyUri(): Uri? {
    val encoded = getQueryParameter(ARG_PROXY) ?: return null
    val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
    return Uri.parse(decoded)
}

private val REGEX_IDENTIFIER = "(.*):(.*)/(.*)".toRegex()

fun Context.getResourceForIdentifier(identifier: String): Int? {
    return try {
        val split = REGEX_IDENTIFIER.matchEntire(identifier) ?: return null
        val packageName = split.groups[1]?.value ?: return null
        val type = split.groups[2]?.value ?: return null
        val name = split.groups[3]?.value ?: return null
        resources.getIdentifier(name, type, packageName)
    }catch (e: Resources.NotFoundException) {
        null
    }
}