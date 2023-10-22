package com.kieronquinn.app.smartspacer.ui.screens.configuration.geofence

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.GeofenceRequirement.GeofenceRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GeofenceRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

abstract class GeofenceRequirementConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val dismissBus: Flow<Unit>
    abstract val settingsTopPadding: StateFlow<Float>
    abstract val bottomSheetOffset: StateFlow<Float>
    abstract val radius: Flow<Float>

    abstract fun onResumed()
    abstract fun onSaveClicked()

    abstract fun setupWithId(id: String)

    abstract fun setBottomSheetInset(inset: Float)
    abstract fun setBottomSheetOffset(offset: Float)

    abstract fun onLatLngChanged(latLng: LatLng)
    abstract fun onNameChanged(name: String)
    abstract fun onRadiusChanged(radius: Float)
    abstract fun onNotificationResponsivenessChanged(notificationResponsiveness: Float)
    abstract fun onLoiteringDelayChanged(loiteringDelay: Float)

    sealed class State {
        object Loading: State()
        object RequestPermission: State()
        object RequestBackgroundPermission: State()
        object LimitReached: State()
        data class Loaded(val data: GeofenceRequirementData): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        object Saving: State()
    }

}

class GeofenceRequirementConfigurationViewModelImpl(
    context: Context,
    private val geofenceRepository: GeofenceRepository,
    private val databaseRepository: DatabaseRepository,
    private val gson: Gson,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): GeofenceRequirementConfigurationViewModel(scope) {

    companion object {
        private const val RADIUS_DEFAULT = 50f
        private const val LOITERING_DELAY_DEFAULT = 0
        private const val NOTIFICATION_RESPONSIVENESS_DEFAULT = 0
    }

    override val bottomSheetOffset = MutableStateFlow(0f)
    override val dismissBus = MutableSharedFlow<Unit>()

    private val _radius = MutableStateFlow<Float?>(null)
    override val radius = _radius.filterNotNull()

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val bottomSheetInset = MutableStateFlow(0f)
    private val requirementId = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)

    private val locationPermissionGranted = resumeBus.mapLatest {
        geofenceRepository.hasLocationPermission()
    }

    private val backgroundLocationPermissionGranted = resumeBus.mapLatest {
        geofenceRepository.hasBackgroundLocationPermission()
    }

    private val requirement = combine(
        locationPermissionGranted.filter { it }.take(1),
        backgroundLocationPermissionGranted.filter { it }.take(1),
        requirementId.filterNotNull()
    ) { _, _, id ->
        val requirements = geofenceRepository.geofenceRequirements.filterNotNull().first()
        requirements.firstOrNull { it.id == id } ?: getDefaultRequirement(context, id)
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private suspend fun getDefaultRequirement(
        context: Context,
        id: String
    ): GeofenceRequirementData {
        val location = context.getLocation().first()
        return GeofenceRequirementData(
            id,
            location?.latitude ?: 0.0,
            location?.longitude ?: 0.0,
            RADIUS_DEFAULT,
            "",
            NOTIFICATION_RESPONSIVENESS_DEFAULT,
            LOITERING_DELAY_DEFAULT
        )
    }

    override val state = combine(
        resumeBus,
        requirement,
        isSaving
    ) { _, requirement, saving ->
        if(!geofenceRepository.hasLocationPermission()){
            return@combine State.RequestPermission
        }
        if(!geofenceRepository.hasBackgroundLocationPermission()) {
            return@combine State.RequestBackgroundPermission
        }
        if(geofenceRepository.isGeofenceLimitReached()) {
            return@combine State.LimitReached
        }
        if(saving) {
            return@combine State.Saving
        }
        if(requirement != null){
            _radius.emit(requirement.radius)
            State.Loaded(requirement)
        }else{
            State.Loading
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val settingsTopPadding = combine(bottomSheetInset, bottomSheetOffset) { inset, offset ->
        val progress = 1f - offset
        progress * inset
    }.stateIn(vmScope, SharingStarted.Eagerly, 0f)

    override fun onResumed() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onSaveClicked() {
        vmScope.launch(dispatcher) {
            if(isSaving.value) return@launch
            val data = (state.value as? State.Loaded)?.data ?: return@launch
            isSaving.emit(true)
            if(data.name.isEmpty()){
                val geocodedAddress = geofenceRepository.geocodeLocation(data.getLatLng())
                data.name = geocodedAddress?.getAddressLine(0) ?: ""
            }
            if(data.name.isEmpty()){
                data.name = "${data.latitude}, ${data.longitude}"
            }
            databaseRepository.addRequirementData(
                RequirementData(data.id, RequirementDataType.GEOFENCE.name, gson.toJson(data))
            )
            isSaving.emit(false)
            dismissBus.emit(Unit)
        }
    }

    override fun setupWithId(id: String) {
        vmScope.launch {
            requirementId.emit(id)
        }
    }

    override fun onLatLngChanged(latLng: LatLng) {
        (state.value as? State.Loaded)?.data?.run {
            this.latitude = latLng.latitude
            this.longitude = latLng.longitude
        }
    }

    override fun onNameChanged(name: String) {
        (state.value as? State.Loaded)?.data?.run {
            this.name = name
        }
        onResumed()
    }

    override fun onRadiusChanged(radius: Float) {
        (state.value as? State.Loaded)?.data?.run {
            this.radius = radius
        }
        vmScope.launch {
            _radius.emit(radius)
        }
    }

    override fun onLoiteringDelayChanged(loiteringDelay: Float) {
        (state.value as? State.Loaded)?.data?.run {
            this.loiteringDelay = loiteringDelay.roundToInt()
        }
    }

    override fun onNotificationResponsivenessChanged(notificationResponsiveness: Float) {
        (state.value as? State.Loaded)?.data?.run {
            this.notificationResponsiveness = notificationResponsiveness.roundToInt()
        }
    }

    override fun setBottomSheetInset(inset: Float) {
        vmScope.launch {
            bottomSheetInset.emit(inset)
        }
    }

    override fun setBottomSheetOffset(offset: Float) {
        vmScope.launch {
            bottomSheetOffset.emit(offset)
        }
    }

}