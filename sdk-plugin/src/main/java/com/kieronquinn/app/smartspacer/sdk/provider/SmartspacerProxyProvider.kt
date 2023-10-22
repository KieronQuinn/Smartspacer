package com.kieronquinn.app.smartspacer.sdk.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerProxyProvider.Companion.proxy
import com.kieronquinn.app.smartspacer.sdk.utils.createSmartspacerProxyUri
import com.kieronquinn.app.smartspacer.sdk.utils.getProxyUri

/**
 *  [ContentProvider] that proxies calls to other providers, via your app. This allows access to
 *  files you have gained access to with SAF to be accessed by Smartspacer.
 *
 *  Add this to your manifest, exported, and call [proxy] with your required URI and authority of
 *  this provider.
 */
abstract class SmartspacerProxyProvider: ContentProvider() {

    companion object {
        fun proxy(uri: Uri, proxyAuthority: String): Uri {
            val proxyUri = Uri.Builder()
                .scheme("content")
                .authority(proxyAuthority)
                .build()
            return createSmartspacerProxyUri(uri, proxyUri)
        }
    }

    private val contentResolver by lazy {
        context!!.contentResolver
    }

    private fun verifySecurity() {
        if(callingPackage != SmartspacerConstants.SMARTSPACER_PACKAGE_NAME) {
            throw SecurityException("Accessing Proxy Content Provider denied from $callingPackage")
        }
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

    private fun <T> Uri.runProxied(block: (Uri) -> T): T? {
        verifySecurity()
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

}