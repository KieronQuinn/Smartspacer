package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Parcelable
import android.provider.Settings
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.MalformedJsonException
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.PluginApi.RemotePluginInfo
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Companion.ACTION_COMPLICATION
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Companion.ACTION_REQUIREMENT
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Companion.ACTION_TARGET
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageInfoCompat
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import com.kieronquinn.app.smartspacer.utils.extensions.queryContentProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface PluginRepository {

    companion object {
        const val ACTION_TARGET = "com.kieronquinn.app.smartspacer.TARGET"
        const val ACTION_COMPLICATION = "com.kieronquinn.app.smartspacer.COMPLICATION"
        const val ACTION_REQUIREMENT = "com.kieronquinn.app.smartspacer.REQUIREMENT"
    }

    fun getPlugins(): Flow<List<Plugin>>
    fun getUpdateCount(): Flow<Int>
    fun reload(force: Boolean)

    suspend fun getPluginInfo(plugin: Plugin.Remote): RemotePluginInfo?

    sealed class Plugin(
        open val isInstalled: Boolean,
        open val packageName: String,
        open val name: CharSequence
    ): Parcelable {
        @Parcelize
        data class Local(
            override val packageName: String,
            override val name: CharSequence,
            val launchIntent: Intent
        ): Plugin(true, packageName, name)

        @Parcelize
        data class Remote(
            override val isInstalled: Boolean,
            override val packageName: String,
            override val name: CharSequence,
            val description: String,
            val url: String,
            val author: String,
            val supportedPackages: List<String>,
            val updateAvailable: Boolean,
            val recommendedApps: List<CharSequence>
        ): Plugin(isInstalled, packageName, name)
    }

}

class PluginRepositoryImpl(
    context: Context,
    packageRepository: PackageRepository,
    okHttpClient: OkHttpClient,
    settingsRepository: SmartspacerSettingsRepository,
    private val scope: CoroutineScope = MainScope(),
    private val packageManager: PackageManager = context.packageManager
): PluginRepository {

    companion object {
        private const val TIMEOUT = 5000L
        private const val MIN_RELOAD_TIME = 60_000L
    }

    @VisibleForTesting
    val refreshBus = MutableStateFlow(System.currentTimeMillis())

    private val cache = Cache(context.cacheDir, 100 * 1024 * 1024)
    private val client = okHttpClient.newBuilder()
        .cache(cache)
        .build()

    private val pluginApi = Retrofit.Builder()
        .client(client)
        .baseUrl("http://localhost/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PluginApi::class.java)

    private val packageChanged = packageRepository.onPackageChanged
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val installedPlugins = packageChanged.debounce(1000L).mapLatest {
        getInstalledPlugins()
    }.flowOn(Dispatchers.IO)

    private val remotePlugins = combine(
        settingsRepository.pluginRepositoryUrl.asFlow(),
        settingsRepository.pluginRepositoryEnabled.asFlow(),
        refreshBus
    ) { url, enabled, _ ->
        if(!enabled) return@combine emptyList()
        val plugins = try {
            pluginApi.withTimeout {
                it.getPlugins(url).execute().body()
            } ?: run {
                emptyList()
            }
        }catch (e: Exception){
            emptyList()
        }
        plugins.map { plugin ->
            Plugin.Remote(
                false,
                plugin.packageName,
                plugin.name,
                plugin.description,
                plugin.url,
                plugin.author,
                plugin.packages?.parseCommaSeparated() ?: emptyList(),
                updateAvailable = false,
                recommendedApps = emptyList()
            )
        }
    }.flatMapLatest { plugins ->
        if(plugins.isEmpty()) return@flatMapLatest flowOf(plugins)
        combine(*plugins.map { it.withRemoteInfo() }.toTypedArray()){
            it.toList()
        }
    }

    override fun getPlugins(): Flow<List<Plugin>> {
        return combine(
            remotePlugins,
            installedPlugins
        ) { remote, installed ->
            val combined = ArrayList<Plugin>(remote)
            installed.forEach { plugin ->
                if(combined.none { plugin.packageName == it.packageName }){
                    combined.add(plugin)
                }
            }
            combined
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getPluginInfo(plugin: Plugin.Remote): RemotePluginInfo? {
        return withContext(Dispatchers.IO){
            try {
                pluginApi.withTimeout {
                    val response = it.getRemotePluginInfo(plugin.url).execute().body()
                    response ?: RemotePluginInfo.Url(plugin.url)
                }
            }catch (e: MalformedJsonException){
                if(!plugin.url.endsWith(".json")) {
                    RemotePluginInfo.Url(plugin.url)
                }else null
            }catch (e: Exception){
                null
            }
        }
    }

    override fun getUpdateCount(): Flow<Int> {
        return getPlugins().flatMapLatest {
            val remote = it.filterIsInstance<Plugin.Remote>().map {
                flow {
                    emit(Pair(it, getPluginInfo(it)))
                }
            }.toTypedArray()
            if(remote.isEmpty()) return@flatMapLatest flowOf(0)
            combine(*remote) { plugins ->
                plugins.count { plugin ->
                    val remoteVersion = (plugin.second as? RemotePluginInfo.UpdateJson)?.versionCode
                        ?: return@count false
                    val currentVersion = getPackageVersion(plugin.first.packageName)
                        ?: return@count false
                    currentVersion < remoteVersion
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun reload(force: Boolean) {
        scope.launch {
            //Only reload if either forced or after a minimum amount of time
            val elapsedTime = System.currentTimeMillis() - refreshBus.value
            if(elapsedTime < MIN_RELOAD_TIME && !force) return@launch
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    private fun getInstalledPlugins(): List<Plugin.Local> {
        val installedTargets = packageManager
            .queryContentProviders(Intent(ACTION_TARGET))
            .map { it.providerInfo.packageName }
        val installedComplications = packageManager
            .queryContentProviders(Intent(ACTION_COMPLICATION))
            .map { it.providerInfo.packageName }
        val installedRequirements = packageManager
            .queryContentProviders(Intent(ACTION_REQUIREMENT))
            .map { it.providerInfo.packageName }
        val pluginPackages = (installedTargets + installedComplications + installedRequirements)
            .toSet()
        return pluginPackages.mapNotNull {
            if(it == BuildConfig.APPLICATION_ID) return@mapNotNull null
            val label = packageManager.getPackageLabel(it) ?: return@mapNotNull null
            val launchIntent = packageManager.getLaunchIntentForPackage(it) ?:
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$it")
                }
            Plugin.Local(it, label, launchIntent)
        }
    }

    private fun Plugin.Remote.withRemoteInfo(): Flow<Plugin.Remote> = combine(
        packageVersion(packageName),
        packagesInstalled(supportedPackages)
    ){ version, recommended ->
        val recommendedApps = recommended.mapNotNull {
            packageManager.getPackageLabel(it)
        }.sortedBy { it.toString().lowercase() }
        val remote = if(version != null){
            getPluginInfo(this@withRemoteInfo)
        }else null
        if(version != null && remote is RemotePluginInfo.UpdateJson){
            val updateAvailable = remote.versionCode != null && remote.versionCode > version
            this.copy(
                updateAvailable = updateAvailable,
                isInstalled = true,
                recommendedApps = recommendedApps
            )
        }else{
            this.copy(
                updateAvailable = false,
                isInstalled = false,
                recommendedApps = recommendedApps
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun packagesInstalled(packages: List<String>): Flow<List<String>> = flow {
        emit(packages.filter { packageManager.isPackageInstalled(it) })
        packageChanged.filter { packages.contains(it) }.collect {
            emit(packages.filter { packageManager.isPackageInstalled(it) })
        }
    }

    private fun packageVersion(packageName: String): Flow<Long?> = flow {
        emit(getPackageVersion(packageName))
        packageChanged.filter { it == packageName }.collect {
            emit(getPackageVersion(packageName))
        }
    }

    private fun getPackageVersion(packageName: String): Long? {
        return try {
            packageManager.getPackageInfoCompat(packageName).longVersionCode
        }catch (e: NameNotFoundException){
            null
        }
    }

    private fun String.parseCommaSeparated(): List<String> {
        if(isBlank()) return emptyList()
        return if(contains(",")){
            split(",")
        }else{
            listOf(this)
        }
    }

    private suspend fun <T> PluginApi.withTimeout(block: (PluginApi) -> T?): T? {
        return withTimeoutOrNull(TIMEOUT) {
            block(this@withTimeout)
        }
    }

}

interface PluginApi {

    @GET
    fun getPlugins(@Url url: String): Call<List<RemotePlugin>>

    @GET
    fun getRemotePluginInfo(@Url url: String): Call<RemotePluginInfo.UpdateJson>

    sealed class RemotePluginInfo {
        data class Url(val url: String): RemotePluginInfo()
        data class UpdateJson(
            @SerializedName("icon")
            val icon: String?,
            @SerializedName("description")
            val description: String,
            @SerializedName("changelog")
            val changelog: String? = null,
            @SerializedName("screenshots")
            val screenshots: List<String>? = null,
            @SerializedName("download_url")
            val downloadUrl: String,
            @SerializedName("required_package")
            val requiredPackage: String? = null,
            @SerializedName("required_package_url")
            val requiredPackageUrl: String? = null,
            @SerializedName("required_sdk")
            val requiredSdk: Int? = null,
            @SerializedName("required_features")
            val requiredFeatures: List<String>? = null,
            @SerializedName("minimum_smartspacer_version")
            val minimumSmartspacerVersion: Long?,
            @SerializedName("version_code")
            val versionCode: Long?
        ): RemotePluginInfo()
    }



    data class RemotePlugin(
        @SerializedName("author")
        val author: String,
        @SerializedName("package_name")
        val packageName: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("description")
        val description: String,
        @SerializedName("url")
        val url: String,
        @SerializedName("packages")
        val packages: String?
    )

}