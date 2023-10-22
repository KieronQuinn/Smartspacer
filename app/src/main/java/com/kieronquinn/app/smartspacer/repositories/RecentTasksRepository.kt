package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import com.kieronquinn.app.smartspacer.ITaskObserver
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface RecentTasksRepository {

    val recentTaskPackages: StateFlow<List<String>>

}

class RecentTasksRepositoryImpl(
    private val context: Context,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val scope: CoroutineScope = MainScope()
): RecentTasksRepository {

    override val recentTaskPackages = shizukuServiceRepository.isReady.flatMapLatest {
        if(!it) return@flatMapLatest flowOf(emptyList())
        getRecentTasks()
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun getRecentTasks(): Flow<List<String>> = callbackFlow {
        val listener = object: ITaskObserver.Stub() {
            override fun onTasksChanged(packages: MutableList<String>) {
                trySend(packages)
            }
        }
        shizukuServiceRepository.runWithService {
            it.setTaskObserver(listener)
        }
        awaitClose {
            shizukuServiceRepository.runWithServiceIfAvailable {
                it.setTaskObserver(null)
            }
        }
    }

    private fun setupListener() = scope.launch {
        recentTaskPackages.collect {
            SmartspacerRequirementProvider.notifyChange(context, RecentTaskRequirement::class.java)
        }
    }

    init {
        setupListener()
    }

}