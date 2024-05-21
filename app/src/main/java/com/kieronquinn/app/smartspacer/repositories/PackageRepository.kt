package com.kieronquinn.app.smartspacer.repositories

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.utils.extensions.getInstalledApplications
import com.kieronquinn.app.smartspacer.utils.extensions.queryIntentActivitiesCompat
import com.kieronquinn.app.smartspacer.utils.extensions.registerReceiverCompat
import com.kieronquinn.app.smartspacer.utils.extensions.unregisterReceiverCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

interface PackageRepository {

    val onPackageChanged: Flow<String>

    fun onPackageChanged(vararg packageName: String): Flow<Long>
    fun onPackageChanged(scope: CoroutineScope, vararg packageName: String): StateFlow<Long>

    suspend fun getInstalledApps(includeNotLaunchable: Boolean = false): List<ListAppsApp>

    data class ListAppsApp(
        val packageName: String,
        val label: CharSequence,
        val showPackageName: Boolean = false
    )

}

class PackageRepositoryImpl(private val context: Context): PackageRepository {

    private val scope = MainScope()

    override val onPackageChanged = callbackFlow {
        val receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val packageName = when(intent.action){
                    Intent.ACTION_PACKAGE_ADDED -> intent.dataString
                    Intent.ACTION_PACKAGE_CHANGED -> intent.dataString
                    Intent.ACTION_PACKAGE_REPLACED -> intent.dataString
                    Intent.ACTION_PACKAGE_REMOVED -> intent.dataString
                    else -> null
                } ?: return
                trySend(packageName)
            }
        }
        context.registerReceiverCompat(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
        awaitClose {
            context.unregisterReceiverCompat(receiver)
        }
    }.map {
        it.removePrefix("package:")
    }.shareIn(scope, SharingStarted.Eagerly)

    override fun onPackageChanged(vararg packageName: String): Flow<Long> {
        return onPackageChanged.filter { packageName.contains(it) }
            .map { System.currentTimeMillis() }
            .debounce(1000L)
    }

    override fun onPackageChanged(
        scope: CoroutineScope,
        vararg packageName: String
    ): StateFlow<Long> {
        return onPackageChanged(*packageName)
            .stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())
    }

    override suspend fun getInstalledApps(includeNotLaunchable: Boolean): List<ListAppsApp> {
        return withContext(Dispatchers.IO) {
            if(includeNotLaunchable){
                getAllApps()
            }else{
                getLaunchableApps()
            }
        }
    }

    private fun getLaunchableApps(): List<ListAppsApp> {
        val packageManager = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivitiesCompat(launchIntent, 0).mapNotNull {
            val label = it.loadLabel(packageManager)?.trim() ?: return@mapNotNull null
            Pair(it.activityInfo.packageName, label)
        }.sortApps()
    }

    private fun getAllApps(): List<ListAppsApp> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications().map {
            val label = it.loadLabel(packageManager).trim()
            Pair(it.packageName, label)
        }.sortApps()
    }

    private fun List<Pair<String, CharSequence>>.sortApps(): List<ListAppsApp> {
        return map {
            ListAppsApp(it.first, it.second)
        }.sortedBy {
            it.label.toString().lowercase()
        }.let { apps ->
            //Show the package name if there's multiple
            apps.map { app ->
                if(apps.containsIdenticalLabel(app.label)) {
                    app.copy(showPackageName = true)
                }else app
            }
        }
    }

    /**
     *  If there's multiple apps with the same label (ignoring whitespace and case), we want to
     *  show the package name to differentiate between them, so count labels which match this
     *  similarity and return if there's more than one (ie. there are multiple similar apps)
     */
    private fun List<ListAppsApp>.containsIdenticalLabel(label: CharSequence): Boolean {
        val formattedLabel = label.toString().lowercase().trim()
        val count = count {
            it.label.toString().lowercase().trim() == formattedLabel
        }
        return count > 1
    }

}