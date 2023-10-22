package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.smartspace.Action.ComplicationBackup
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup.RequirementType
import com.kieronquinn.app.smartspacer.model.smartspace.Target.TargetBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.*
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.ErrorReason
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.ExpandedCustomWidgetBackup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.firstOfInstance
import com.kieronquinn.app.smartspacer.utils.extensions.gzip
import com.kieronquinn.app.smartspacer.utils.extensions.ungzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.LoadBackupResult.ErrorReason as LoadErrorReason

interface BackupRepository {

    fun createBackup(toUri: Uri): Flow<SmartspacerBackupProgress>
    suspend fun loadBackup(fromUri: Uri): LoadBackupResult

    @Parcelize
    data class SmartspacerBackup(
        @SerializedName("version")
        val version: Int = 1,
        @SerializedName("timestamp")
        val timestamp: Long = System.currentTimeMillis(),
        @SerializedName("target_backups")
        val targetBackups: List<TargetBackup>,
        @SerializedName("complication_backups")
        val complicationBackups: List<ComplicationBackup>,
        @SerializedName("requirement_backups")
        val requirementBackups: List<RequirementBackup>,
        @SerializedName("custom_widgets")
        val expandedCustomWidgets: List<ExpandedCustomWidgetBackup>,
        @SerializedName("settings")
        val settings: Map<String, String>
    ): Parcelable

    sealed class BackupProgress {
        data class CreatingBackup(val current: Int, val total: Int): BackupProgress()
        data class Finished<T>(val backups: List<T>): BackupProgress()
    }

    sealed class SmartspacerBackupProgress {
        object CreatingBackup: SmartspacerBackupProgress()
        data class CreatingTargetsBackup(val progress: Int): SmartspacerBackupProgress()
        data class CreatingComplicationsBackup(val progress: Int): SmartspacerBackupProgress()
        data class CreatingRequirementsBackup(val progress: Int): SmartspacerBackupProgress()
        object CreatingCustomWidgetsBackup: SmartspacerBackupProgress()
        object CreatingSettingsBackup: SmartspacerBackupProgress()
        object WritingFile: SmartspacerBackupProgress()
        data class Finished(val filename: String?): SmartspacerBackupProgress()
        data class Error(val reason: ErrorReason): SmartspacerBackupProgress()

        enum class ErrorReason(@StringRes val description: Int) {
            FAILED_TO_CREATE_FILE(R.string.backup_error_failed_to_create),
            FAILED_TO_WRITE_FILE(R.string.backup_error_failed_to_write)
        }
    }

    sealed class LoadBackupResult {
        data class Success(val backup: SmartspacerBackup): LoadBackupResult()
        data class Error(val reason: LoadErrorReason): LoadBackupResult()

        enum class ErrorReason(@StringRes val description: Int) {
            FAILED_TO_READ_FILE(R.string.restore_error_failed_to_read),
            FAILED_TO_LOAD_BACKUP(R.string.restore_error_failed_to_load)
        }
    }

    @Parcelize
    data class RestoreConfig(
        val hasTargets: Boolean,
        val hasComplications: Boolean,
        val hasRequirements: Boolean,
        val hasExpandedCustomWidgets: Boolean,
        val hasSettings: Boolean,
        val shouldRestoreTargets: Boolean,
        val shouldRestoreComplications: Boolean,
        val shouldRestoreRequirements: Boolean,
        val shouldRestoreExpandedCustomWidgets: Boolean,
        val shouldRestoreSettings: Boolean,
        val backup: SmartspacerBackup
    ): Parcelable

}

class BackupRepositoryImpl(
    private val context: Context,
    private val gson: Gson,
    private val targetsRepository: TargetsRepository,
    private val requirementsRepository: RequirementsRepository,
    private val databaseRepository: DatabaseRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    private val expandedRepository: ExpandedRepository
): BackupRepository {

    override fun createBackup(toUri: Uri) = flow {
        emit(SmartspacerBackupProgress.CreatingBackup)
        val outFile = DocumentFile.fromSingleUri(context, toUri) ?: run {
            emit(SmartspacerBackupProgress.Error(ErrorReason.FAILED_TO_CREATE_FILE))
            return@flow
        }
        val outStream = context.contentResolver.openOutputStream(toUri) ?: run {
            emit(SmartspacerBackupProgress.Error(ErrorReason.FAILED_TO_WRITE_FILE))
            return@flow
        }
        val targetsBackup = getTargetBackups().getBackups<TargetBackup> {
            emit(SmartspacerBackupProgress.CreatingTargetsBackup(it))
        }
        val complicationsBackup = getComplicationBackups().getBackups<ComplicationBackup> {
            emit(SmartspacerBackupProgress.CreatingComplicationsBackup(it))
        }
        val requirementsBackup = getRequirementBackups().getBackups<RequirementBackup> {
            emit(SmartspacerBackupProgress.CreatingRequirementsBackup(it))
        }
        emit(SmartspacerBackupProgress.CreatingCustomWidgetsBackup)
        val customWidgets = expandedRepository.getExpandedCustomWidgetBackups()
        emit(SmartspacerBackupProgress.CreatingSettingsBackup)
        val settingsBackup = settingsRepository.getBackup()
        val backup = SmartspacerBackup(
            targetBackups = targetsBackup,
            complicationBackups = complicationsBackup,
            requirementBackups = requirementsBackup,
            expandedCustomWidgets = customWidgets,
            settings = settingsBackup
        )
        emit(SmartspacerBackupProgress.WritingFile)
        outStream.buffered().use {
            it.write(gson.toJson(backup).gzip())
            it.flush()
        }
        outStream.flush()
        outStream.close()
        emit(SmartspacerBackupProgress.Finished(outFile.name))
    }.flowOn(Dispatchers.IO)

    override suspend fun loadBackup(fromUri: Uri): LoadBackupResult = withContext(Dispatchers.IO) {
        val inStream = context.contentResolver.openInputStream(fromUri)
            ?: return@withContext LoadBackupResult.Error(LoadErrorReason.FAILED_TO_READ_FILE)
        val bytes = inStream.buffered().use {
            it.readBytes()
        }
        val backup = try {
            gson.fromJson(bytes.ungzip(), SmartspacerBackup::class.java)
        }catch (e: Exception){
            inStream.close()
            return@withContext LoadBackupResult.Error(LoadErrorReason.FAILED_TO_LOAD_BACKUP)
        }
        inStream.close()
        LoadBackupResult.Success(backup)
    }

    private suspend fun <T> Flow<BackupProgress>.getBackups(
        onProgress: suspend (Int) -> Unit
    ): List<T> {
        return onEach {
            if(it is BackupProgress.CreatingBackup){
                val progress = (it.current / it.total.toFloat() * 100).roundToInt()
                onProgress(progress)
            }
        }.firstOfInstance<BackupProgress.Finished<T>>().backups
    }

    private suspend fun getTargetBackups(): Flow<BackupProgress> = flow {
        val targets = targetsRepository.getAvailableTargets().first()
        val total = targets.size
        val list = targets.filter {
            it.getPluginConfig().firstNotNull().compatibilityState == CompatibilityState.Compatible
        }.onEachIndexed { index, _ ->
            emit(BackupProgress.CreatingBackup(index, total))
        }.mapNotNull {
            try {
                it.createBackup()
            }catch (e: Exception){
                null
            }
        }
        emit(BackupProgress.Finished(list))
    }

    private suspend fun getComplicationBackups(): Flow<BackupProgress> = flow {
        val complications = targetsRepository.getAvailableComplications().first()
        val total = complications.size
        val list = complications.filter {
            it.getPluginConfig().firstNotNull().compatibilityState == CompatibilityState.Compatible
        }.onEachIndexed { index, _ ->
            emit(BackupProgress.CreatingBackup(index, total))
        }.mapNotNull {
            try {
                it.createBackup()
            }catch (e: Exception){
                null
            }
        }
        emit(BackupProgress.Finished(list))
    }

    private suspend fun getRequirementBackups(): Flow<BackupProgress> = flow {
        val requirements = requirementsRepository.getAllInUseRequirements().first()
        val total = requirements.size
        val targets = databaseRepository.getTargets().first()
        val complications = databaseRepository.getActions().first()
        val list = requirements.filter {
            it.getPluginConfig().firstNotNull().compatibilityState == CompatibilityState.Compatible
        }.onEachIndexed { index, _ ->
            emit(BackupProgress.CreatingBackup(index, total))
        }.mapNotNull { req ->
            if(req.id == null) return@mapNotNull null
            //Find the target or complication the requirement is attached to
            val targetRequirement = targets.firstOrNull { target -> target.hasRequirement(req.id) }
            val complicationRequirement = complications.firstOrNull { complication ->
                complication.hasRequirement(req.id)
            }
            val requirementType = when {
                targetRequirement != null && targetRequirement.anyRequirements.contains(req.id) -> {
                    RequirementType.ANY
                }
                targetRequirement != null && targetRequirement.allRequirements.contains(req.id) -> {
                    RequirementType.ALL
                }
                complicationRequirement != null &&
                        complicationRequirement.anyRequirements.contains(req.id) -> {
                    RequirementType.ANY
                }
                complicationRequirement != null &&
                        complicationRequirement.allRequirements.contains(req.id) -> {
                    RequirementType.ALL
                }
                //No longer in the database? Don't back it up.
                else -> return@mapNotNull null
            }
            val requirementFor = targetRequirement?.id ?: complicationRequirement?.id
                ?: return@mapNotNull null
            try {
                req.createBackup(requirementType, requirementFor)
            }catch (e: Exception){
                null
            }.also {
                req.close()
            }
        }
        emit(BackupProgress.Finished(list))
    }

}