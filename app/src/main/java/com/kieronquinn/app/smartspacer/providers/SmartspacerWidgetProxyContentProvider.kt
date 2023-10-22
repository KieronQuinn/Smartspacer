package com.kieronquinn.app.smartspacer.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.kieronquinn.app.smartspacer.BuildConfig
import java.net.URLDecoder
import java.net.URLEncoder

class SmartspacerWidgetProxyContentProvider: ContentProvider() {

    companion object {
        private const val ARG_PROXY = "proxy"
        private val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widgetproxyprovider"

        private val SMARTSPACER_WIDGET_PROXY_URI = Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .build()

        /**
         *  Creates a "proxy" Uri for a given [originalUri]. This routes content calls via
         *  Smartspacer's widget proxy ContentProvider, since the Widget's host app won't have
         *  granted Uri access to the plugin, but to Smartspacer.
         */
        fun createSmartspacerWidgetProxyUri(originalUri: Uri): Uri {
            if(originalUri.authority == AUTHORITY) return originalUri
            val encodedUri = URLEncoder.encode(originalUri.toString(), Charsets.UTF_8.name())
            return SMARTSPACER_WIDGET_PROXY_URI.buildUpon()
                .appendQueryParameter(ARG_PROXY, encodedUri).build()
        }
    }

    private val contentResolver by lazy {
        context!!.contentResolver
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return uri.runProxied {
            contentResolver.query(it, projection, selection, selectionArgs, sortOrder)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        return uri.runProxied {
            contentResolver.query(it, projection, queryArgs, cancellationSignal)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        return uri.runProxied {
            contentResolver.query(
                it, projection, selection, selectionArgs, sortOrder, cancellationSignal
            )
        }
    }

    override fun getType(uri: Uri): String? {
        return uri.runProxied {
            contentResolver.getType(it)
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return uri.runProxied {
            contentResolver.openFile(it, mode, null)
        }
    }

    override fun openFile(
        uri: Uri,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        return uri.runProxied {
            contentResolver.openFile(it, mode, signal)
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        return uri.runProxied {
            contentResolver.openAssetFile(it, mode, null)
        }
    }

    override fun openAssetFile(
        uri: Uri,
        mode: String,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        return uri.runProxied {
            contentResolver.openAssetFile(it, mode, null)
        }
    }

    override fun openTypedAssetFile(
        uri: Uri,
        mimeTypeFilter: String,
        opts: Bundle?
    ): AssetFileDescriptor? {
        return uri.runProxied {
            contentResolver.openTypedAssetFile(it, mimeTypeFilter, opts, null)
        }
    }

    override fun openTypedAssetFile(
        uri: Uri,
        mimeTypeFilter: String,
        opts: Bundle?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        return uri.runProxied {
            contentResolver.openTypedAssetFile(it, mimeTypeFilter, opts, signal)
        }
    }

    private fun <T> Uri.runProxied(block: (Uri) -> T): T? {
        val proxyUri = getProxyUri() ?: return null
        val callingIdentity = clearCallingIdentity()
        return block(proxyUri).also {
            restoreCallingIdentity(callingIdentity)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    private fun Uri.getProxyUri(): Uri? {
        val encoded = getQueryParameter(ARG_PROXY) ?: return null
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        return Uri.parse(decoded)
    }

}