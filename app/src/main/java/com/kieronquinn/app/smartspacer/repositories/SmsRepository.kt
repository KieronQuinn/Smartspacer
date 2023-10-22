package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.Context
import android.provider.Telephony
import com.kieronquinn.app.smartspacer.components.smartspace.complications.SmsComplication
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.queryAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface SmsRepository {

    val smsUnreadCount: StateFlow<Int>

    fun reload()
    fun isSmsAppGoogleMessages(): Boolean

}

class SmsRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = MainScope()
): SmsRepository {

    companion object {
        private const val PACKAGE_GOOGLE_MESSAGES = "com.google.android.apps.messaging"
    }

    private val contentResolver = context.contentResolver
    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    override val smsUnreadCount = reloadBus.flatMapLatest {
        if(!hasPermission()) return@flatMapLatest flowOf(0)
        getUnreadSmsCount()
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    override fun reload() {
        scope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun isSmsAppGoogleMessages(): Boolean {
        return Telephony.Sms.getDefaultSmsPackage(context) == PACKAGE_GOOGLE_MESSAGES
    }

    private fun hasPermission(): Boolean {
        return context.hasPermission(Manifest.permission.READ_SMS)
    }

    private fun getUnreadSmsCount(): Flow<Int> {
        return contentResolver.queryAsFlow(
            Telephony.Sms.CONTENT_URI,
            projection = arrayOf(
                Telephony.Sms.READ
            ),
            selection = "${Telephony.Sms.READ}=?",
            selectionArgs = arrayOf("0")
        ).mapLatest { cursor ->
            cursor.count.also {
                cursor.close()
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun setupUnreadCountListener() = scope.launch {
        smsUnreadCount.collect {
            SmartspacerComplicationProvider.notifyChange(context, SmsComplication::class.java)
        }
    }

    init {
        setupUnreadCountListener()
    }

}