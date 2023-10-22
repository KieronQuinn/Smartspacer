package com.kieronquinn.app.smartspacer.model.smartspace

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider.Companion.EXTRA_INTENT
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider.Companion.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider.Companion.METHOD_GET_CONFIG
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider.Companion.METHOD_ON_RECEIVE
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider.Config
import com.kieronquinn.app.smartspacer.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable

class BroadcastListener(
    val context: Context,
    val id: String,
    val packageName: String,
    val authority: String
): KoinComponent, Closeable {

    private val scope = MainScope()

    private val idBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .appendPath(id)
        .build()

    private val authorityBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .build()

    private val packageRepository by inject<PackageRepository>()
    private val contentResolver = context.contentResolver
    private val authorityBasedRemoteChange = contentResolver.observerAsFlow(authorityBasedUri)
    private val idBasedRemoteChange = contentResolver.observerAsFlow(idBasedUri)

    private val appChange = packageRepository.onPackageChanged(scope, packageName)

    private val change = combine(appChange, authorityBasedRemoteChange, idBasedRemoteChange) { _, _, _ ->
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val remoteConfig = change.map {
        getRemoteConfig()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val defaultConfig = Config(emptyList())

    private suspend fun getRemoteConfig() = withContext(Dispatchers.IO) {
        val config = callRemote(METHOD_GET_CONFIG, bundleOf(EXTRA_SMARTSPACER_ID to id))
            ?: return@withContext defaultConfig
        if(config.isEmpty) return@withContext defaultConfig
        Config(config)
    }

    private suspend fun onReceive(intent: Intent) {
        val extras = bundleOf(
            EXTRA_SMARTSPACER_ID to id,
            EXTRA_INTENT to intent
        )
        callRemote(METHOD_ON_RECEIVE, extras)
    }

    private suspend fun callRemote(
        method: String, extras: Bundle? = null
    ): Bundle? = withContext(Dispatchers.IO) {
        try {
            contentResolver?.callSafely(authority, method, null, extras)
        }catch (e: Throwable){
            //Provider has gone
            null
        }
    }

    private fun setupReceivers() = scope.launch {
        val intents = remoteConfig.filterNotNull().flatMapLatest {
            val intentFilters = it.intentFilters.map { filter ->
                context.broadcastReceiverAsFlow(filter)
            }.toTypedArray()
            merge(*intentFilters)
        }
        intents.collect {
            onReceive(it)
        }
    }

    private fun IntentFilter.toFormattedString() = StringBuilder().apply {
        append("IntentFilter { ")
        if(countActions() > 0){
            append("actions=[")
            val actions = ArrayList<String>()
            actionsIterator().forEach {
                actions.add(it)
            }
            append(actions.joinToString(", "))
            append("] ")
        }
        if(countCategories() > 0) {
            append("categories=[")
            val categories = ArrayList<String>()
            categoriesIterator().forEach {
                categories.add(it)
            }
            append(categories.joinToString(", "))
            append("] ")
        }
        append(" }")
    }

    init {
        setupReceivers()
    }

    override fun close() {
        scope.cancel()
    }

}