package com.kieronquinn.app.smartspacer.ui.screens.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.update.Release
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class UpdateViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val showFab: StateFlow<Boolean>

    abstract fun setupWithRelease(release: Release)
    abstract fun onDownloadBrowserClicked()
    abstract fun startDownload()
    abstract fun startInstall()

    sealed class State {
        object Loading: State()
        data class Info(val release: Release): State()
        data class Downloading(val downloadState: DownloadState.Progress): State()
        data class StartInstall(val uri: Uri): State()
        object Failed: State()
    }

}

class UpdateViewModelImpl(
    private val navigation: ContainerNavigation,
    private val downloadRepository: DownloadRepository,
    context: Context,
    scope: CoroutineScope? = null
) : UpdateViewModel(scope) {

    private val updateRelease = MutableStateFlow<Release?>(null)
    private val downloadRequest = MutableSharedFlow<DownloadRequest?>()
    private val downloadDescription = context.getString(R.string.download_manager_description)

    private val downloadTitle = { version: String ->
        context.getString(R.string.update_heading, version)
    }

    private val updateDownload = downloadRequest.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        downloadRepository.download(it)
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override val state = combine(
        updateRelease.filterNotNull(),
        updateDownload
    ) { release, download ->
        if(download == null) return@combine State.Info(release)
        when(download) {
            is DownloadState.Progress -> {
                State.Downloading(download)
            }
            is DownloadState.DownloadComplete -> {
                State.StartInstall(download.file)
            }
            is DownloadState.Failed -> {
                State.Failed
            }
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val showFab = state.mapLatest {
        it is State.Info
    }.stateIn(vmScope, SharingStarted.Eagerly, state.value is State.Info)

    override fun setupWithRelease(release: Release) {
        vmScope.launch {
            updateRelease.emit(release)
        }
    }

    override fun startDownload() {
        vmScope.launch {
            val currentRelease = updateRelease.value ?: return@launch
            val downloadRequest = DownloadRequest(
                currentRelease.downloadUrl,
                downloadTitle(currentRelease.versionName),
                downloadDescription,
                currentRelease.fileName
            )
            this@UpdateViewModelImpl.downloadRequest.emit(downloadRequest)
        }
    }

    override fun startInstall() {
        vmScope.launch {
            val state = state.value as? State.StartInstall ?: return@launch
            val intent = Intent(Intent.ACTION_VIEW).apply {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                setDataAndType(state.uri, CONTENT_TYPE_APK)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            navigation.navigate(intent)
        }
    }

    override fun onDownloadBrowserClicked() {
        vmScope.launch {
            val currentRelease = updateRelease.value ?: return@launch
            navigation.navigate(Uri.parse(currentRelease.gitHubUrl))
        }
    }

    override fun onCleared() {
        super.onCleared()
        val currentId = (state.value as? State.Downloading)?.downloadState?.id ?: return
        downloadRepository.cancelDownload(currentId)
    }

}