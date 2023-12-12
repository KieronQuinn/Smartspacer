package com.kieronquinn.app.smartspacer.ui.screens.settings.dump

import android.content.Context
import android.net.Uri
import android.os.Process
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.DumpSmartspacerSession
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

abstract class DumpSmartspacerViewModel: ViewModel() {

    abstract val content: StateFlow<String>
    abstract val successToastBus: StateFlow<Unit?>

    abstract fun onResume()
    abstract fun onPause()
    abstract fun onWriteToFileClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onWriteToFileSelected(context: Context, uri: Uri)
    abstract fun consumeToast()

}

class DumpSmartspacerViewModelImpl(
    context: Context,
    systemSmartspaceRepository: SystemSmartspaceRepository
): DumpSmartspacerViewModel() {

    companion object {
        const val DUMP_FILE_TEMPLATE = "smartspacer_target_dump_%s.txt"
    }

    private val targets = MutableStateFlow("Loading...")

    private val defaultTargets = combine(
        systemSmartspaceRepository.homeTargets,
        systemSmartspaceRepository.lockTargets,
        systemSmartspaceRepository.mediaTargets
    ) { home, lock, media ->
        StringBuilder().apply {
            if(home.isNotEmpty()) {
                appendLine()
                appendLine("=====================")
                appendLine("System Home Targets:")
                appendLine("=====================")
                appendLine(home.joinToString("\n\n"))
            }
            if(lock.isNotEmpty()) {
                appendLine()
                appendLine("=====================")
                appendLine("System Lock Targets:")
                appendLine("=====================")
                appendLine(lock.joinToString("\n\n"))
            }
            if(media.isNotEmpty()) {
                appendLine()
                appendLine("=====================")
                appendLine("System Media Targets:")
                appendLine("=====================")
                appendLine(media.joinToString("\n\n"))
            }
        }.toString()
    }

    override val content = combine(targets, defaultTargets) { t, d ->
        StringBuilder().apply {
            appendLine("=====================")
            appendLine("   Merged Targets:")
            appendLine("=====================")
            appendLine(t)
            appendLine(d)
        }.toString()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Loading...")

    override val successToastBus = MutableStateFlow<Unit?>(null)

    private val session by lazy {
        DumpSmartspacerSession(
            context,
            SmartspaceConfig(Int.MAX_VALUE, UiSurface.HOMESCREEN, BuildConfig.APPLICATION_ID),
            SmartspaceSessionId(UUID.randomUUID().toString(), Process.myUserHandle()),
            ::onTargetsChanged
        )
    }

    private suspend fun onTargetsChanged(targets: List<SmartspaceTarget>) {
        this.targets.emit(targets.joinToString("\n\n"))
    }

    override fun onResume() {
        session.onResume()
    }

    override fun onPause() {
        session.onPause()
    }

    override fun onCleared() {
        super.onCleared()
        session.onDestroy()
    }

    override fun onWriteToFileClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getFilename())
    }

    override fun onWriteToFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                val current = content.value.takeIf { c -> c.isNotEmpty() } ?: return@use
                it.write(current.toByteArray())
                it.flush()
            } ?: return@launch
            successToastBus.emit(Unit)
        }
    }

    override fun consumeToast() {
        viewModelScope.launch {
            successToastBus.emit(null)
        }
    }

    private fun getFilename(): String {
        val time = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return String.format(DUMP_FILE_TEMPLATE, dateTimeFormatter.format(time))
    }

}