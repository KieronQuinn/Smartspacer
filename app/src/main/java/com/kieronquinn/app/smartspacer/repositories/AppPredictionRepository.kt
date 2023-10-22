package com.kieronquinn.app.smartspacer.repositories

import android.app.prediction.AppTarget
import android.content.Context
import android.content.pm.ParceledListSlice
import com.kieronquinn.app.smartspacer.IAppPredictionOnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement.AppPredictionRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.utils.extensions.getAppPredictionComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

interface AppPredictionRepository {

    val appPredictions: StateFlow<List<String>>
    val appPredictionRequirements: Flow<List<AppPredictionRequirementData>?>

    fun isSupported(): Boolean

}

class AppPredictionRepositoryImpl(
    private val context: Context,
    private val shizuku: ShizukuServiceRepository,
    dataRepository: DataRepository,
    private val scope: CoroutineScope = MainScope()
): AppPredictionRepository {

    companion object {
        private const val APP_PREDICTION_LIMIT = 5
    }

    override val appPredictions = shizuku.isReady.flatMapLatest {
        if(!it){
            return@flatMapLatest flowOf(emptyList())
        }
        loadAppPredictions().map { apps ->
            apps.take(APP_PREDICTION_LIMIT)
        }
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val appPredictionRequirements = dataRepository.getRequirementData(
        RequirementDataType.APP_PREDICTION, AppPredictionRequirementData::class.java
    ).flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    private fun loadAppPredictions() = callbackFlow {
        val callback = object: IAppPredictionOnTargetsAvailableListener.Stub() {
            override fun onTargetsAvailable(targets: ParceledListSlice<*>?) {
                trySend((targets as ParceledListSlice<AppTarget>).list)
            }
        }
        shizuku.runWithService {
            try {
                it.createAppPredictorSession(callback)
            }catch (e: IllegalStateException){
                //Client already been destroyed
            }
        }
        awaitClose {
            shizuku.runWithServiceIfAvailable {
                try {
                    it.destroyAppPredictorSession()
                }catch (e: IllegalStateException){
                    //Client already been destroyed
                }
            }
        }
    }.map { targets ->
        targets.sortedBy { it.rank }.map { it.packageName }
    }

    override fun isSupported(): Boolean {
        return shizuku.isReady.value && context.getAppPredictionComponent() != null
    }

    @VisibleForTesting
    fun setupListener() = scope.launch {
        var lastPredictions = emptyList<String>()
        combine(appPredictions, appPredictionRequirements) { predictions, requirements ->
            val affectedApps = setOf(*(lastPredictions + predictions).toTypedArray())
            lastPredictions = predictions
            requirements?.forEach { requirement ->
                if(affectedApps.contains(requirement.packageName)){
                    notifyRequirement(requirement.id)
                }
            }
        }.collect()
    }

    private fun notifyRequirement(id: String) {
        SmartspacerRequirementProvider.notifyChange(context, AppPredictionRequirement::class.java, id)
    }

    init {
        setupListener()
    }

}