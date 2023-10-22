package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Companion.ACTION_REQUIREMENT
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.queryContentProviders
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.annotations.VisibleForTesting

interface RequirementsRepository {

    fun getAllRequirements(): List<Requirement>

    fun getAllInUseRequirements(): Flow<List<Requirement>>

    fun getAnyRequirementsForTarget(id: String?): Flow<List<Requirement>>
    fun getAllRequirementsForTarget(id: String?): Flow<List<Requirement>>
    fun getAnyRequirementsForComplication(id: String?): Flow<List<Requirement>>
    fun getAllRequirementsForComplication(id: String?): Flow<List<Requirement>>

    fun any(requirements: Flow<List<Requirement>>): Flow<Boolean>
    fun all(requirements: Flow<List<Requirement>>): Flow<Boolean>

    fun notifyChangeAfterDelay(id: String, authority: String)
    fun forceReloadAll()

    suspend fun performRequirementRestore(
        authority: String, smartspacerId: String, invert: Boolean, backup: Backup
    ): Boolean

}

class RequirementsRepositoryImpl(
    private val context: Context,
    private val databaseRepository: DatabaseRepository,
    private val scope: CoroutineScope = MainScope()
): RequirementsRepository {

    @VisibleForTesting
    val forceReload = MutableStateFlow(System.currentTimeMillis())

    override fun getAllRequirements(): List<Requirement> {
        val providers = context.packageManager.queryContentProviders(Intent(ACTION_REQUIREMENT))
        return providers.map {
            @Suppress("CloseRequirement")
            Requirement(
                context,
                it.providerInfo.authority,
                null, //ID isn't yet set
                false,
                it.providerInfo.packageName
            )
        }
    }

    override fun getAllInUseRequirements(): Flow<List<Requirement>> {
        var previous = emptyList<Requirement>()
        return databaseRepository.getRequirements().mapLatest {
            it.map { requirement ->
                @Suppress("CloseRequirement")
                Requirement(
                    context,
                    requirement.authority,
                    requirement.id,
                    requirement.invert,
                    requirement.packageName
                )
            }
        }.onEach { reqs ->
            previous.forEach { req -> req.close() }
            previous = reqs
        }
    }

    override fun getAnyRequirementsForTarget(id: String?): Flow<List<Requirement>> {
        if(id == null) return flowOf(emptyList())
        var previous = emptyList<Requirement>()
        return databaseRepository.getTargetById(id).flatMapLatest {
            if(it == null) return@flatMapLatest flowOf(emptyList())
            val requirements = it.anyRequirements.map { requirement ->
                getRequirement(requirement)
            }.toTypedArray()
            combine(*requirements){ reqs ->
                reqs.toList().filterNotNull()
            }.onEach { reqs ->
                previous.forEach { req -> req.close() }
                previous = reqs
            }
        }
    }

    override fun getAllRequirementsForTarget(id: String?): Flow<List<Requirement>> {
        if(id == null) return flowOf(emptyList())
        var previous = emptyList<Requirement>()
        return databaseRepository.getTargetById(id).flatMapLatest {
            if(it == null) return@flatMapLatest flowOf(emptyList())
            val requirements = it.allRequirements.map { requirement ->
                getRequirement(requirement)
            }.toTypedArray()
            combine(*requirements){ reqs ->
                reqs.toList().filterNotNull()
            }.onEach { reqs ->
                previous.forEach { req -> req.close() }
                previous = reqs
            }
        }
    }

    override fun getAnyRequirementsForComplication(id: String?): Flow<List<Requirement>> {
        if(id == null) return flowOf(emptyList())
        var previous = emptyList<Requirement>()
        return databaseRepository.getActionById(id).flatMapLatest {
            if(it == null) return@flatMapLatest flowOf(emptyList())
            val requirements = it.anyRequirements.map { requirement ->
                getRequirement(requirement)
            }.toTypedArray()
            combine(*requirements){ reqs ->
                reqs.toList().filterNotNull()
            }.onEach { reqs ->
                previous.forEach { req -> req.close() }
                previous = reqs
            }
        }
    }

    override fun getAllRequirementsForComplication(id: String?): Flow<List<Requirement>> {
        if(id == null) return flowOf(emptyList())
        var previous = emptyList<Requirement>()
        return databaseRepository.getActionById(id).flatMapLatest {
            if(it == null) return@flatMapLatest flowOf(emptyList())
            val requirements = it.allRequirements.map { requirement ->
                getRequirement(requirement)
            }.toTypedArray()
            combine(*requirements){ reqs ->
                reqs.toList().filterNotNull()
            }.onEach { reqs ->
                previous.forEach { req -> req.close() }
                previous = reqs
            }
        }
    }

    override fun any(requirements: Flow<List<Requirement>>): Flow<Boolean> {
        return requirements.flatMapLatest {
            if(it.isEmpty()){
                return@flatMapLatest flowOf(true)
            }
            combine(*it.toTypedArray()) { items ->
                items.any { met -> met }
            }
        }
    }

    override fun all(requirements: Flow<List<Requirement>>): Flow<Boolean> {
        return requirements.flatMapLatest {
            if(it.isEmpty()){
                return@flatMapLatest flowOf(true)
            }
            combine(*it.toTypedArray()) { items ->
                items.all { met -> met }
            }
        }
    }

    override fun notifyChangeAfterDelay(
        id: String,
        authority: String
    ) {
        scope.launch {
            delay(1000L)
            SmartspacerRequirementProvider.notifyChange(context, authority, id)
        }
    }

    override fun forceReloadAll() {
        scope.launch {
            forceReload.emit(System.currentTimeMillis())
        }
    }

    override suspend fun performRequirementRestore(
        authority: String,
        smartspacerId: String,
        invert: Boolean,
        backup: Backup
    ): Boolean = withContext(Dispatchers.IO) {
        @Suppress("CloseRequirement")
        val requirement = Requirement(context, authority, smartspacerId, invert)
        requirement.restoreBackup(backup).also {
            requirement.close()
        }
    }

    private fun getRequirement(id: String): Flow<Requirement?> {
        return databaseRepository.getRequirementById(id).mapLatest { databaseRequirement ->
            if(databaseRequirement != null) {
                @Suppress("CloseRequirement")
                val requirement = Requirement(
                    context,
                    databaseRequirement.authority,
                    databaseRequirement.id,
                    databaseRequirement.invert,
                    databaseRequirement.packageName
                )
                val config = requirement.getPluginConfig().firstNotNull()
                if(config.compatibilityState == CompatibilityState.Compatible) {
                    requirement
                } else null
            }else null
        }
    }

}