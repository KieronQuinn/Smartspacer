package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.MusicPlayingRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.targets.MusicTarget
import com.kieronquinn.app.smartspacer.model.media.MediaContainer
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface MediaRepository {

    val mediaController: StateFlow<MediaContainer?>
    val mediaPlaying: Flow<Boolean>

    fun setMediaContainer(mediaContainer: MediaContainer?)

}

class MediaRepositoryImpl(private val context: Context): MediaRepository {

    override val mediaController = MutableStateFlow<MediaContainer?>(null)
    override val mediaPlaying = mediaController.map { it != null }

    private val scope = MainScope()

    override fun setMediaContainer(mediaContainer: MediaContainer?) {
        scope.launch {
            mediaController.emit(mediaContainer)
        }
    }

    private fun setupMusicTarget() = scope.launch {
        mediaController.debounce(500L).collect {
            SmartspacerTargetProvider.notifyChange(context, MusicTarget::class.java)
            SmartspacerRequirementProvider.notifyChange(context, MusicPlayingRequirement::class.java)
        }
    }

    init {
        setupMusicTarget()
    }

}