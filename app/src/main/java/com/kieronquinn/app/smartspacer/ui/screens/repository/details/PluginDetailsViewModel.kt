package com.kieronquinn.app.smartspacer.ui.screens.repository.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.kieronquinn.app.smartspacer.BuildConfig.VERSION_CODE
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PluginApi.RemotePluginInfo
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel.PluginViewState.IncompatibleReason
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageInfoCompat
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class PluginDetailsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithPlugin(plugin: Plugin.Remote)
    abstract fun onInstallClicked()
    abstract fun onStartInstallClicked()
    abstract fun onUninstallClicked()
    abstract fun onOpenClicked()
    abstract fun onInstallCompanionClicked()
    abstract fun onScreenshotClicked(url: String)

    sealed class State {
        object Loading: State()
        object Error: State()
        data class Loaded(
            val plugin: Plugin.Remote,
            val remote: RemotePluginInfo,
            val viewState: PluginViewState
        ): State()
    }

    sealed class PluginViewState {
        object Install: PluginViewState()
        data class Incompatible(val reason: IncompatibleReason): PluginViewState()
        data class Downloading(val downloadState: DownloadState.Progress): PluginViewState()
        data class StartInstall(val uri: Uri): PluginViewState()
        data class Installed(val launchIntent: Intent?, val upToDate: Boolean): PluginViewState()

        sealed class IncompatibleReason {
            data class IncompatibleSDK(val required: Int) : IncompatibleReason()
            data object OutdatedSmartspacer: IncompatibleReason()
            data class MissingFeature(val feature: String): IncompatibleReason()
            data class RequiresApp(val packageName: String, val url: String): IncompatibleReason()
        }
    }

}

class PluginDetailsViewModelImpl(
    private val navigation: ContainerNavigation,
    private val packageRepository: PackageRepository,
    private val downloadRepository: DownloadRepository,
    context: Context,
    pluginRepository: PluginRepository,
    scope: CoroutineScope? = null
): PluginDetailsViewModel(scope) {

    companion object {
        private const val URL_TEMPLATE_PLAY = "https://play.google.com/store/apps/details?id="
    }

    private val plugin = MutableStateFlow<Plugin.Remote?>(null)
    private val packageManager = context.packageManager
    private val downloadTitle = context.getString(R.string.plugin_details_downloading_title)

    @VisibleForTesting
    val pluginToDownload = MutableSharedFlow<DownloadRequest?>()

    private val pluginDownload = pluginToDownload.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        downloadRepository.download(it).map { state ->
            if(state is DownloadState.Failed){
                Toast.makeText(context, R.string.plugin_details_download_failed, Toast.LENGTH_LONG)
                    .show()
                null
            }else state
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val remotePluginInfo = plugin.filterNotNull().mapLatest {
        Pair(it, pluginRepository.getPluginInfo(it))
    }

    private val viewState = remotePluginInfo.flatMapLatest { remote ->
        val requiredApp = (remote.second as? RemotePluginInfo.UpdateJson)?.requiredPackage
        val companionAppChanged = requiredApp?.let { pkg -> isPackageInstalled(pkg) }
            ?: flowOf(true)
        val isInstalled = isPackageInstalled(remote.first.packageName)
        combine(
            companionAppChanged,
            isInstalled,
            pluginDownload
        ) { _, installed, download ->
            val compatibility = remote.second?.getIncompatibleReasonOrNull()
            val remoteVersion = (remote.second as? RemotePluginInfo.UpdateJson)?.versionCode
            val isUpToDate = if(installed && remoteVersion != null){
                packageManager.getPackageInfoCompat(remote.first.packageName).longVersionCode >=
                        remoteVersion
            }else true
            when {
                download != null -> {
                    when (download) {
                        is DownloadState.Progress -> {
                            PluginViewState.Downloading(download)
                        }
                        is DownloadState.DownloadComplete -> {
                            PluginViewState.StartInstall(download.file)
                        }
                        else -> throw RuntimeException("Invalid state")
                    }
                }
                installed -> {
                    val launchIntent = packageManager.getLaunchIntentForPackage(
                        remote.first.packageName
                    )
                    PluginViewState.Installed(launchIntent, isUpToDate)
                }
                compatibility != null -> {
                    PluginViewState.Incompatible(compatibility)
                }
                else -> {
                    PluginViewState.Install
                }
            }
        }
    }

    override val state = combine(
        remotePluginInfo,
        viewState
    ) { remote, viewState ->
        State.Loaded(remote.first, remote.second ?: return@combine State.Error, viewState)
    }.onEach {
        if(it !is State.Loaded) return@onEach
        if(it.remote is RemotePluginInfo.Url) {
            openUrlAndDismiss(it.remote)
        }
        if(it.viewState is PluginViewState.StartInstall) {
            onStartInstallClicked()
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    private suspend fun openUrlAndDismiss(remotePluginInfo: RemotePluginInfo.Url) {
        navigation.navigate(Uri.parse(remotePluginInfo.url))
        navigation.navigateBack()
    }

    override fun setupWithPlugin(plugin: Plugin.Remote) {
        vmScope.launch {
            this@PluginDetailsViewModelImpl.plugin.emit(plugin)
        }
    }

    private fun isPackageInstalled(packageName: String): StateFlow<Boolean> {
        return packageRepository.onPackageChanged(packageName).map {
            packageManager.isPackageInstalled(packageName)
        }.stateIn(
            vmScope,
            SharingStarted.Eagerly,
            packageManager.isPackageInstalled(packageName)
        )
    }

    private fun RemotePluginInfo.getIncompatibleReasonOrNull(): IncompatibleReason? {
        if(this !is RemotePluginInfo.UpdateJson) return null
        val missingFeature = requiredFeatures?.firstOrNull { !packageManager.hasSystemFeature(it) }
        return when {
            requiredSdk != null && Build.VERSION.SDK_INT < requiredSdk -> {
                IncompatibleReason.IncompatibleSDK(requiredSdk)
            }
            minimumSmartspacerVersion != null && VERSION_CODE.toLong() < minimumSmartspacerVersion -> {
                IncompatibleReason.OutdatedSmartspacer
            }
            missingFeature != null -> {
                IncompatibleReason.MissingFeature(missingFeature)
            }
            requiredPackage != null && !packageManager.isPackageInstalled(requiredPackage) -> {
                val requiredUrl = requiredPackageUrl ?: "$URL_TEMPLATE_PLAY$requiredPackage"
                IncompatibleReason.RequiresApp(requiredPackage, requiredUrl)
            }
            else -> null
        }
    }

    override fun onOpenClicked() {
        val viewState = getViewState<PluginViewState.Installed>() ?: return
        vmScope.launch {
            navigation.navigate(viewState.launchIntent ?: return@launch)
        }
    }

    override fun onInstallClicked() {
        vmScope.launch {
            val plugin = (state.value as? State.Loaded)?.plugin ?: return@launch
            val remote = (state.value as? State.Loaded)?.remote as? RemotePluginInfo.UpdateJson
                ?: return@launch
            val request = DownloadRequest(
                remote.downloadUrl,
                downloadTitle,
                plugin.name,
                "${plugin.name}.apk"
            )
            pluginToDownload.emit(request)
        }
    }

    override fun onStartInstallClicked() {
        vmScope.launch {
            val startInstall = getViewState<PluginViewState.StartInstall>() ?: return@launch
            pluginToDownload.emit(null)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                setDataAndType(startInstall.uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            navigation.navigate(intent)
        }
    }

    override fun onUninstallClicked() {
        val packageName = (state.value as? State.Loaded)?.plugin?.packageName ?: return
        vmScope.launch {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
            }
            navigation.navigate(intent)
        }
    }

    override fun onInstallCompanionClicked() {
        val reason = getViewState<PluginViewState.Incompatible>()?.reason
        val url = (reason as? IncompatibleReason.RequiresApp)?.url ?: return
        vmScope.launch {
            navigation.navigate(Uri.parse(url))
        }
    }

    override fun onScreenshotClicked(url: String) {
        vmScope.launch {
            navigation.navigate(PluginDetailsFragmentDirections.actionPluginDetailsFragmentToPluginDetailsScreenshotFragment(url))
        }
    }

    override fun onCleared() {
        super.onCleared()
        val state = (state.value as? State.Loaded) ?: return
        val id = (state.viewState as? PluginViewState.Downloading)?.downloadState?.id ?: return
        downloadRepository.cancelDownload(id)
    }

    private fun <T: PluginViewState> getViewState(): T? {
        val state = (state.value as? State.Loaded) ?: return null
        return state.viewState as? T ?: return null
    }

}