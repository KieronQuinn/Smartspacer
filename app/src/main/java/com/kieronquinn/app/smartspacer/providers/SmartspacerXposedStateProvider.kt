package com.kieronquinn.app.smartspacer.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import kotlin.system.exitProcess

class SmartspacerXposedStateProvider: ContentProvider() {

    companion object {
        private const val METHOD_GET = "get"
        private const val EXTRA_ENABLED = "enabled"

        private val URI_XPOSED_STATE =
            Uri.parse("content://${BuildConfig.APPLICATION_ID}.xposedstate")

        fun getXposedEnabled(context: Context): Boolean {
            return try {
                context.contentResolver
                    .callSafely(URI_XPOSED_STATE, METHOD_GET, null, null)
                    ?.getBoolean(EXTRA_ENABLED, false)
            }catch (e: Exception) {
                null
            } ?: false
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(method) {
            METHOD_GET -> bundleOf(EXTRA_ENABLED to isEnabled())
            else -> null
        }.also {
            killAfterDelay()
        }
    }

    private fun isEnabled(): Boolean {
        return false
    }

    private fun killAfterDelay() = Thread {
        Thread.sleep(500L)
        exitProcess(0)
    }.start()

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

}