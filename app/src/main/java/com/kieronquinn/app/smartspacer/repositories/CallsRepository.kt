package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.Context
import android.provider.CallLog.Calls
import com.kieronquinn.app.smartspacer.components.smartspace.complications.MissedCallsComplication
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.map
import com.kieronquinn.app.smartspacer.utils.extensions.queryAsFlow
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

interface CallsRepository {

    val missedCallsCount: StateFlow<Int>

    fun reload()

}

class CallsRepositoryImpl(
    private val context: Context
): CallsRepository {

    private val contentResolver = context.contentResolver
    private val reloadBus = MutableStateFlow(System.currentTimeMillis())
    private val scope = MainScope()

    override val missedCallsCount = reloadBus.flatMapLatest {
        if(!hasPermission()) return@flatMapLatest flowOf(0)
        getMissedCallsCount()
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    private fun getMissedCallsCount(): Flow<Int> {
        return contentResolver.queryAsFlow(
            Calls.CONTENT_URI,
            projection = arrayOf(
                Calls.IS_READ
            ),
            selection = "${Calls.TYPE}= ? AND ${Calls.IS_READ}=?",
            selectionArgs = arrayOf(
                Calls.MISSED_TYPE.toString(),
                "0"
            )
        ).mapLatest {
            it.map { row ->
                row.getInt(0)
            }.count { read ->
                read == 0
            }.also { _ ->
                it.close()
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun reload() {
        scope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    private fun hasPermission(): Boolean {
        return context.hasPermission(Manifest.permission.READ_CALL_LOG)
    }

    private fun setupMissedCallsListener() = scope.launch {
        missedCallsCount.collect {
            SmartspacerComplicationProvider.notifyChange(
                context, MissedCallsComplication::class.java
            )
        }
    }

    init {
        setupMissedCallsListener()
    }

}