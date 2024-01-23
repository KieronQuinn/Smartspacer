package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.kieronquinn.app.smartspacer.components.smartspace.targets.FlashlightTarget
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.FlashlightRepository.TargetState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.utils.extensions.flashlightOn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.Executors

interface FlashlightRepository {

    val targetState: StateFlow<TargetState>

    enum class TargetState {
        HIDDEN, ON, OFF
    }

}

class FlashlightRepositoryImpl(
    context: Context,
    dataRepository: DataRepository
): FlashlightRepository {

    companion object {
        private const val LIGHT_LEVEL_DEFAULT = 5f
        private const val LIGHT_LEVEL_ADJUSTMENT = 0.2f
        private const val LIGHT_LEVEL_MAX = 4f

        private fun DataRepository.requiresRecommendation(): Flow<Boolean> {
            return getTargetData(TargetDataType.FLASHLIGHT, FlashlightTarget.TargetData::class.java)
                .map { it.any { data -> data.recommend } }
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = MainScope()

    private val flashlightOn = context.flashlightOn(executor).stateIn(scope, SharingStarted.Eagerly, false)

    private val recommend = dataRepository.requiresRecommendation().flatMapLatest {
        if(it) {
            context.recommendFlashlight()
                .stateIn(scope, SharingStarted.Eagerly, false)
        }else flowOf(false)
    }

    override val targetState = combine(
        flashlightOn,
        recommend
    ) { flashlight, recommend ->
        when {
            flashlight -> TargetState.ON
            recommend -> TargetState.OFF
            else -> TargetState.HIDDEN
        }
    }.onEach {
        SmartspacerTargetProvider.notifyChange(context, FlashlightTarget::class.java)
    }.stateIn(scope, SharingStarted.Eagerly, TargetState.HIDDEN)

    /**
     *  Logic reverse engineered from ASI
     */
    private fun Context.recommendFlashlight() = callbackFlow {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        var lightLevel = LIGHT_LEVEL_DEFAULT
        val listener = object: SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.getOrNull(0) ?: return
                val previous = lightLevel
                lightLevel = value + LIGHT_LEVEL_ADJUSTMENT * (previous - value)
                if((previous > LIGHT_LEVEL_MAX && lightLevel < LIGHT_LEVEL_MAX) ||
                    (previous <= LIGHT_LEVEL_MAX && lightLevel > LIGHT_LEVEL_MAX)){
                    trySend(lightLevel < LIGHT_LEVEL_MAX)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                //No-op
            }
        }
        if(lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, 3)
        }
        awaitClose {
            if(lightSensor != null) {
                sensorManager.unregisterListener(listener)
            }
        }
    }

}