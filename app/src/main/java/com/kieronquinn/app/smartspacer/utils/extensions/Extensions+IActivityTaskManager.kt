package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.ActivityManager
import android.app.IActivityTaskManager
import android.content.pm.ParceledListSlice
import com.kieronquinn.app.smartspacer.utils.task.TaskListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun IActivityTaskManager.getTaskPackages(userId: Int) = onTasksChanged().mapLatest {
    val tasks = getRecentTasks(
        Int.MAX_VALUE, 0, userId
    ) as ParceledListSlice<ActivityManager.RecentTaskInfo>
    tasks.list.mapNotNull {
        it.topActivity?.packageName ?: it.baseActivity?.packageName ?: it.origActivity?.packageName
    }.distinct()
}.flowOn(Dispatchers.IO)

private fun IActivityTaskManager.onTasksChanged(): Flow<Unit> = callbackFlow {
    val listener = TaskListener {
        trySend(Unit)
    }
    trySend(Unit)
    registerTaskStackListener(listener)
    awaitClose {
        unregisterTaskStackListener(listener)
    }
}.debounce(250L)