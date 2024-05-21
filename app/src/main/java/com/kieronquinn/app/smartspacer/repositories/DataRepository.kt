package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.model.database.ActionData
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.model.database.BaseData
import com.kieronquinn.app.smartspacer.model.database.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.model.database.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface DataRepository {

    fun <T> getTargetData(id: String, type: Class<T>): T?
    fun <T> getActionData(id: String, type: Class<T>): T?
    fun <T> getRequirementData(id: String, type: Class<T>): T?

    fun <T> getTargetDataFlow(id: String, type: Class<T>): Flow<T?>
    fun <T> getActionDataFlow(id: String, type: Class<T>): Flow<T?>
    fun <T> getRequirementDataFlow(id: String, type: Class<T>): Flow<T?>

    fun <T> getTargetData(dataType: TargetDataType, type: Class<T>): Flow<List<T>>
    fun <T> getActionData(dataType: ActionDataType, type: Class<T>): Flow<List<T>>
    fun <T> getRequirementData(dataType: RequirementDataType, type: Class<T>): Flow<List<T>>

    suspend fun addTargetData(targetData: TargetData)
    suspend fun addActionData(actionData: ActionData)
    suspend fun addRequirementData(requirementData: RequirementData)

    fun <T> updateTargetData(
        id: String,
        type: Class<T>,
        dataType: TargetDataType,
        onComplete: ((context: Context, smartspacerId: String) -> Unit)? = null,
        update: (T?) -> T
    )

    fun <T> updateActionData(
        id: String,
        type: Class<T>,
        dataType: ActionDataType,
        onComplete: ((context: Context, smartspacerId: String) -> Unit)? = null,
        update: (T?) -> T
    )

    fun <T> updateRequirementData(
        id: String,
        type: Class<T>,
        dataType: RequirementDataType,
        onComplete: ((context: Context, smartspacerId: String) -> Unit)? = null,
        update: (T?) -> T
    )

    fun deleteTargetData(id: String)
    fun deleteActionData(id: String)
    fun deleteRequirementData(id: String)

}

class DataRepositoryImpl(
    private val context: Context,
    private val gson: Gson,
    private val databaseRepository: DatabaseRepository
): DataRepository {

    private val scope = MainScope()

    private val targetData = databaseRepository.getTargetData()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val actionData = databaseRepository.getActionData()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val requirementData = databaseRepository.getRequirementData()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override fun <T> getTargetData(id: String, type: Class<T>): T? = runBlocking {
        targetData.firstNotNull().getItem(id, type)
    }

    override fun <T> getActionData(id: String, type: Class<T>): T? = runBlocking {
        actionData.firstNotNull().getItem(id, type)
    }

    override fun <T> getRequirementData(id: String, type: Class<T>): T? = runBlocking {
        requirementData.firstNotNull().getItem(id, type)
    }

    override fun <T> getTargetDataFlow(id: String, type: Class<T>): Flow<T?> {
        return targetData.filterNotNull().map {
            it.getItem(id, type)
        }
    }

    override fun <T> getActionDataFlow(id: String, type: Class<T>): Flow<T?> {
        return actionData.filterNotNull().map {
            it.getItem(id, type)
        }
    }

    override fun <T> getRequirementDataFlow(id: String, type: Class<T>): Flow<T?> {
        return requirementData.filterNotNull().map {
            it.getItem(id, type)
        }
    }

    override fun <T> getTargetData(dataType: TargetDataType, type: Class<T>): Flow<List<T>> {
        return targetData.filterNotNull().map {
            it.mapNotNull { data ->
                if(data.type != dataType.name) return@mapNotNull null
                gson.fromJson(data.data, type)
            }
        }
    }

    override fun <T> getActionData(dataType: ActionDataType, type: Class<T>): Flow<List<T>> {
        return actionData.filterNotNull().map {
            it.mapNotNull { data ->
                if(data.type != dataType.name) return@mapNotNull null
                gson.fromJson(data.data, type)
            }
        }
    }

    override fun <T> getRequirementData(
        dataType: RequirementDataType,
        type: Class<T>
    ): Flow<List<T>> {
        return requirementData.filterNotNull().map {
            it.mapNotNull { data ->
                if(data.type != dataType.name) return@mapNotNull null
                gson.fromJson(data.data, type)
            }
        }
    }

    override suspend fun addTargetData(targetData: TargetData) {
        databaseRepository.addTargetData(targetData)
    }

    override suspend fun addActionData(actionData: ActionData) {
        databaseRepository.addActionData(actionData)
    }

    override suspend fun addRequirementData(requirementData: RequirementData) {
        databaseRepository.addRequirementData(requirementData)
    }

    override fun deleteTargetData(id: String) {
        databaseRepository.deleteTargetData(id)
    }

    override fun deleteActionData(id: String) {
        databaseRepository.deleteActionData(id)
    }

    override fun deleteRequirementData(id: String) {
        databaseRepository.deleteRequirementData(id)
    }

    override fun <T> updateTargetData(
        id: String,
        type: Class<T>,
        dataType: TargetDataType,
        onComplete: ((context: Context, smartspacerId: String) -> Unit)?,
        update: (T?) -> T
    ) {
        scope.launch {
            val targetData = getTargetData(id, type)
            val updated = update(targetData)
            val data = TargetData(id, dataType.name, gson.toJson(updated))
            addTargetData(data)
            onComplete?.invoke(context, id)
        }
    }

    override fun <T> updateActionData(
        id: String,
        type: Class<T>,
        dataType: ActionDataType,
        onComplete: ((context: Context, smartspacerId: String) -> Unit)?,
        update: (T?) -> T
    ) {
        scope.launch {
            val actionData = getActionData(id, type)
            val updated = update(actionData)
            val data = ActionData(id, dataType.name, gson.toJson(updated))
            addActionData(data)
            onComplete?.invoke(context, id)
        }
    }

    override fun <T> updateRequirementData(
        id: String,
        type: Class<T>,
        dataType: RequirementDataType,
        onComplete: ((context: Context, smartspacerId: String) -> Unit)?,
        update: (T?) -> T
    ) {
        scope.launch {
            val requirementData = getRequirementData(id, type)
            val updated = update(requirementData)
            val data = RequirementData(id, dataType.name, gson.toJson(updated))
            addRequirementData(data)
            onComplete?.invoke(context, id)
        }
    }

    private fun <T> List<BaseData>.getItem(id: String, type: Class<T>): T? {
        return firstOrNull {
            it.id == id
        }?.let {
            gson.fromJson(it.data, type)
        }
    }

}