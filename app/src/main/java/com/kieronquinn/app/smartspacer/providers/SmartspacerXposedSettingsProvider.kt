package com.kieronquinn.app.smartspacer.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.utils.extensions.registerContentObserverSafely
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess

class SmartspacerXposedSettingsProvider: ContentProvider() {

    companion object {
        private const val EXTRA_ENABLED = "enabled"
        private const val METHOD_EXPANDED = "expanded"

        private val URI_SMARTSPACER_XPOSED =
            Uri.parse("content://${BuildConfig.APPLICATION_ID}.xposed")

        private val URI_SMARTSPACER_XPOSED_EXPANDED =
            Uri.parse("content://${BuildConfig.APPLICATION_ID}.xposed/expanded")

        private var hasRegisteredCallback = false

        @Synchronized
        fun getExpandedEnabledAndRegisterCallback(context: Context): Boolean {
            val isEnabled = try {
                context.contentResolver.call(
                    URI_SMARTSPACER_XPOSED,
                    METHOD_EXPANDED,
                    null,
                    null
                )?.getBoolean(EXTRA_ENABLED, false)
            }catch (e: Exception) {
                null
            } ?: return false //If we cannot connect to Smartspacer, assume disabled
            if(!hasRegisteredCallback) {
                hasRegisteredCallback = true
                context.contentResolver.registerContentObserverSafely(
                    URI_SMARTSPACER_XPOSED_EXPANDED,
                    false,
                    object: ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            super.onChange(selfChange)
                            exitProcess(0)
                        }
                    }
                )
            }
            return isEnabled
        }

        fun notifyChange(context: Context) {
            context.contentResolver.notifyChange(URI_SMARTSPACER_XPOSED_EXPANDED, null)
        }
    }

    private val settings by inject<SmartspacerSettingsRepository>()

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        when(method) {
            METHOD_EXPANDED -> {
                return bundleOf(EXTRA_ENABLED to settings.expandedXposedEnabled.getSync())
            }
        }
        return null
    }

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