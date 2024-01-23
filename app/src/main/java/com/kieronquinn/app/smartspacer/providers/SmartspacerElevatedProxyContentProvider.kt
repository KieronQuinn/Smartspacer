package com.kieronquinn.app.smartspacer.providers

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.sdk.utils.createSmartspacerProxyUri
import com.kieronquinn.app.smartspacer.sdk.utils.getProxyUri
import org.koin.android.ext.android.inject

class SmartspacerElevatedProxyContentProvider: ContentProvider() {

    companion object {
        private val SMARTSPACER_ELEVATED_PROXY_URI = Uri.Builder()
            .scheme("content")
            .authority("${BuildConfig.APPLICATION_ID}.elevatedproxyprovider")
            .build()

        fun createSmartspacerElevatedProxyUri(originalUri: Uri): Uri {
            return createSmartspacerProxyUri(
                originalUri,
                SMARTSPACER_ELEVATED_PROXY_URI
            )
        }
    }

    private val contentResolver by lazy {
        context!!.contentResolver
    }

    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun getType(uri: Uri): String? {
        return uri.runProxied {
            shizukuServiceRepository.runWithServiceIfAvailable { shizuku ->
                shizuku.proxyContentProviderGetType(it)
            }.unwrap() ?: contentResolver.getType(it)
        }
    }

    override fun getStreamTypes(uri: Uri, mimeTypeFilter: String): Array<String>? {
        return uri.runProxied {
            shizukuServiceRepository.runWithServiceIfAvailable { shizuku ->
                shizuku.proxyContentProviderGetStreamTypes(it, mimeTypeFilter)
            }.unwrap() ?: contentResolver.getStreamTypes(it, mimeTypeFilter)
        }
    }

    @SuppressLint("Recycle")
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return uri.runProxied {
            shizukuServiceRepository.runWithServiceIfAvailable { shizuku ->
                shizuku.proxyContentProviderOpenFile(it, mode)
            }.unwrap() ?: contentResolver.openFile(it, mode, null)
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

    private fun verifySecurity() {
        if(callingPackage != null && callingPackage != BuildConfig.APPLICATION_ID) {
            throw SecurityException("Access denied")
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

}