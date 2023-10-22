package com.kieronquinn.app.smartspacer.sdk.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants

@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class BaseProvider: ContentProvider() {

    protected fun verifySecurity() {
        val calling = callingPackage
        if(calling != SmartspacerConstants.SMARTSPACER_PACKAGE_NAME){
            throw SecurityException("Calling package $calling does not match Smartspacer")
        }
    }

    final override fun call(authority: String, method: String, arg: String?, extras: Bundle?): Bundle? {
        //No-op to prevent modification
        return super.call(authority, method, arg, extras)
    }

    final override fun onCreate(): Boolean {
        return true
    }

    fun provideContext(): Context {
        return context ?: throw RuntimeException("Could not provide context")
    }

    val resources: Resources
        get() = provideContext().resources

    //Default ContentProvider methods are all no-op

    override fun getType(uri: Uri): String? = null

    final override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    final override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    final override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    final override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

}