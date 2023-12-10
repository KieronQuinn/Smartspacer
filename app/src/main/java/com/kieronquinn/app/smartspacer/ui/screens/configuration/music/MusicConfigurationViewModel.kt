package com.kieronquinn.app.smartspacer.ui.screens.configuration.music

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.smartspace.targets.MusicTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.MusicTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class MusicConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(id: String)
    abstract fun onShowAlbumArtChanged(enabled: Boolean)
    abstract fun onUseDoorbellChanged(enabled: Boolean)
    abstract fun onClearPackagesClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val showAlbumArt: Boolean,
            val useDoorbell: Boolean
        ): State()
    }

}

class MusicConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    scope: CoroutineScope? = null
): MusicConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)

    private val data = id.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java).map { data ->
            data ?: TargetData()
        }
    }

    override val state = data.mapLatest {
        State.Loaded(it.showAlbumArt, it.useDoorbell)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithId(id: String) {
        viewModelScope.launch {
            this@MusicConfigurationViewModelImpl.id.emit(id)
        }
    }

    override fun onShowAlbumArtChanged(enabled: Boolean) {
        val id = id.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.MUSIC,
            ::notifyChange
        ){
            val data = it ?: TargetData()
            data.copy(showAlbumArt = enabled)
        }
    }

    override fun onUseDoorbellChanged(enabled: Boolean) {
        val id = id.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.MUSIC,
            ::notifyChange
        ){
            val data = it ?: TargetData()
            data.copy(useDoorbell = enabled)
        }
    }

    override fun onClearPackagesClicked() {
        val id = id.value ?: return
        dataRepository.updateTargetData(
            id,
            TargetData::class.java,
            TargetDataType.MUSIC,
            ::notifyChange
        ){
            val data = it ?: TargetData()
            data.copy(hiddenPackages = emptySet())
        }
    }

    private fun notifyChange(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, MusicTarget::class.java, smartspacerId)
    }

}