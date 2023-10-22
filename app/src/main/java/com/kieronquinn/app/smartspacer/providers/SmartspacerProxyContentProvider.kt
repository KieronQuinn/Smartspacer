package com.kieronquinn.app.smartspacer.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.sdk.utils.getProxyUri
import com.kieronquinn.app.smartspacer.utils.extensions.packageHasPermission
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 *  [ContentProvider] that proxies calls to other providers, via the Smartspacer app. This allows
 *  plugins to only need to grant Uri permission to Smartspacer, and not every launcher app,
 *  SystemUI, etc.
 *
 *  Only packages that are hosting a Smartspacer widget or are have been granted permission to
 *  access Smartspacer can use this ContentProvider, to prevent misuse.
 */
class SmartspacerProxyContentProvider: ContentProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.proxyprovider"

        private const val PERMISSION_SMARTSPACE = "android.permission.MANAGE_SMARTSPACE"
        private const val PERMISSION_STATUS_BAR = "android.permission.STATUS_BAR"
    }

    private val contentResolver by lazy {
        context!!.contentResolver
    }

    private val appWidgetRepository by inject<AppWidgetRepository>()
    private val grantRepository by inject<GrantRepository>()

    private fun hasGrant(): Boolean = runBlocking {
        val calling = callingPackage ?: BuildConfig.APPLICATION_ID
        when {
            hasNativeSmartspacePermission(calling) -> true
            appWidgetRepository.hasAppWidget(calling) -> true
            grantRepository.getGrantForPackage(calling)?.smartspace == true -> true
            else -> false
        }
    }

    /**
     *  Checks if the caller is Native Smartspace with either MANAGE_SMARTSPACE directly, or
     *  STATUS_BAR as a proxy
     */
    private fun hasNativeSmartspacePermission(packageName: String): Boolean {
        return context!!.packageManager.let {
            it.packageHasPermission(packageName, PERMISSION_SMARTSPACE) ||
                    it.packageHasPermission(packageName, PERMISSION_STATUS_BAR)
        }
    }

    private fun verifySecurity() {
        if(!hasGrant()) {
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