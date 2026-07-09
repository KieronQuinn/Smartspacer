package com.kieronquinn.app.smartspacer.utils.doodle

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Fetches the Google doodle JSON using the system WebView (Chromium), which carries the correct
 * TLS fingerprint and cookie store that Google requires to serve a non-empty doodle response.
 *
 * Strategy:
 *  1. Load https://www.google.com in a headless WebView so the origin is established and any
 *     consent/preference cookies Google sets are stored.
 *  2. Once the page finishes loading, inject a fetch() call to the doodle API endpoint.
 *  3. The raw JSON text is returned via a JavascriptInterface callback.
 */
class WebViewDoodleFetcher(private val context: Context) {

    companion object {
        private const val DOODLE_URL = "https://www.google.com/async/ddljson?async=ntp:1"
        private const val TIMEOUT_MS = 15_000L
    }

    suspend fun fetchRawJson(): String? = withTimeoutOrNull(TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                val webView = WebView(context.applicationContext)
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }

                var resumed = false

                val jsInterface = object {
                    @JavascriptInterface
                    fun onResult(json: String) {
                        if (!resumed) {
                            resumed = true
                            handler.post { webView.destroy() }
                            cont.resume(json)
                        }
                    }

                    @JavascriptInterface
                    fun onError(msg: String) {
                        if (!resumed) {
                            resumed = true
                            handler.post { webView.destroy() }
                            cont.resume(null)
                        }
                    }
                }

                webView.addJavascriptInterface(jsInterface, "SmartspacerDoodle")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        // Only trigger the fetch once we've loaded the google.com homepage.
                        if (!url.contains("/async/")) {
                            view.evaluateJavascript(
                                """
                                fetch('$DOODLE_URL', {credentials: 'include'})
                                  .then(function(r){ return r.text(); })
                                  .then(function(t){ SmartspacerDoodle.onResult(t); })
                                  .catch(function(e){ SmartspacerDoodle.onError(e.toString()); });
                                """.trimIndent(),
                                null
                            )
                        }
                    }
                }

                webView.loadUrl("https://www.google.com/")

                cont.invokeOnCancellation {
                    handler.post { webView.destroy() }
                }
            }
        }
    }
}
