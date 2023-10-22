package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.database.CursorIndexOutOfBoundsException
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GmailComplication
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GmailComplication.ActionData
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.GmailRepository.Label
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.map
import com.kieronquinn.app.smartspacer.utils.extensions.querySafely
import com.kieronquinn.app.smartspacer.utils.extensions.unsafeQueryAsFlow
import com.kieronquinn.app.smartspacer.utils.gmail.GmailContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

interface GmailRepository {

    fun reload()
    fun getUnreadCount(smartspacerId: String): Int?
    suspend fun getAllLabels(accountName: String): List<Label>

    data class Label(val name: String, val canonicalName: String)

}

class GmailRepositoryImpl(
    private val context: Context,
    dataRepository: DataRepository,
    private val scope: CoroutineScope = MainScope(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): GmailRepository {

    private val contentResolver = context.contentResolver

    @VisibleForTesting
    val reloadBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val currentUnreadCounts = HashMap<String, Int?>()

    private val complicationData = dataRepository.getActionData(
        ActionDataType.GMAIL, ActionData::class.java
    )

    @VisibleForTesting
    val unreadCounts = complicationData.flatMapLatest {
        val counts = it.map { data -> data.loadUnreadCount() }
        merge(*counts.toTypedArray())
    }

    private fun ActionData.loadUnreadCount(): Flow<Pair<String, Int?>> {
        return reloadBus.flatMapLatest {
            getUnreadCount().map { count -> Pair(id, count) }
        }
    }

    private fun ActionData.getUnreadCount(): Flow<Int?> {
        if(!canRead()) return flowOf(null)
        val account = accountName ?: return flowOf(null)
        return contentResolver.unsafeQueryAsFlow(
            GmailContract.Labels.getLabelsUri(account),
            projection = arrayOf(
                GmailContract.Labels.CANONICAL_NAME,
                GmailContract.Labels.NUM_UNREAD_CONVERSATIONS
            )
        ).mapLatest {
            var unreadCount = 0
            it.moveToFirst()
            try {
                do {
                    val canonicalName = it.getString(0)
                    val unread = it.getInt(1)
                    if(enabledLabels.contains(canonicalName)){
                        unreadCount += unread
                    }
                }while (it.moveToNext())
            }catch (e: CursorIndexOutOfBoundsException){
                //Database issue
            }
            it.close()
            unreadCount
        }.flowOn(Dispatchers.IO)
    }

    override fun getUnreadCount(smartspacerId: String): Int? {
        return currentUnreadCounts[smartspacerId]
    }

    override fun reload() {
        scope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override suspend fun getAllLabels(accountName: String): List<Label> {
        if(!canRead()) return emptyList()
        return try {
            val cursor = withContext(dispatcher) {
                contentResolver.querySafely(
                    GmailContract.Labels.getLabelsUri(accountName),
                    arrayOf(
                        GmailContract.Labels.NAME,
                        GmailContract.Labels.CANONICAL_NAME
                    )
                )
            } ?: return emptyList()
            cursor.map {
                val name = it.getString(0)
                val canonicalName = it.getString(1)
                Label(name, canonicalName)
            }.also {
                cursor.close()
            }
        }catch (e: Exception) {
            emptyList()
        }
    }

    private fun setupUnreadCounts() = scope.launch {
        unreadCounts.collect {
            currentUnreadCounts[it.first] = it.second
            SmartspacerComplicationProvider.notifyChange(
                context, GmailComplication::class.java, it.first
            )
        }
    }

    private fun canRead(): Boolean {
        if(!context.hasPermission(GmailContract.PERMISSION)) return false
        return GmailContract.canReadLabels(context)
    }

    init {
        setupUnreadCounts()
    }

}