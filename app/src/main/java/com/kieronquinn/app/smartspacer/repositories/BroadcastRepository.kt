package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import com.kieronquinn.app.smartspacer.model.smartspace.BroadcastListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

interface BroadcastRepository

class BroadcastRepositoryImpl(
    private val context: Context,
    databaseRepository: DatabaseRepository,
    private val scope: CoroutineScope = MainScope()
): BroadcastRepository {

    @VisibleForTesting
    val broadcastListeners = databaseRepository.getBroadcastListeners()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    @VisibleForTesting
    var currentListeners = emptyList<BroadcastListener>()

    private fun setupListeners() = scope.launch {
        broadcastListeners.collect {
            currentListeners.forEach { listener -> listener.close() }
            currentListeners = it.map { listener ->
                @Suppress("CloseBroadcastListener")
                BroadcastListener(context, listener.id, listener.packageName, listener.authority)
            }
        }
    }

    init {
        setupListeners()
    }

}