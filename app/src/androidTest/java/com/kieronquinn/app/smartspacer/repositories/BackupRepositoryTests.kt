package com.kieronquinn.app.smartspacer.repositories

import android.content.ContentProviderClient
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.documentfile.provider.DocumentFile
import androidx.test.annotation.UiThreadTest
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup.RequirementType
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.LoadBackupResult
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingComplicationsBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingCustomWidgetsBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingRequirementsBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingSettingsBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingTargetsBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.ErrorReason
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.Finished
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.WritingFile
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.ExpandedCustomWidgetBackup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.io.File
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseComplication
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget
import com.kieronquinn.app.smartspacer.model.smartspace.Action as Complication

@Suppress("CloseTarget", "CloseRequirement")
class BackupRepositoryTests: BaseTest<BackupRepository>() {

    companion object: KoinComponent {
        private val gson by inject<Gson>()

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        /**
         *  Create a Content Provider Client which responds to `backup` calls with a mock bundle
         *  where the name is the [uri], and the backup data is a JSON formatted instance of
         *  [MockBackup], with [MockBackup.data] set to the [uri].
         *
         *  `restore` calls then verify this bundle is valid.
         *
         *  Config lookup calls are replaced with a mock with dummy data to show as compatible
         */
        private fun createMockContentProviderClient(uri: String) = mock<ContentProviderClient> {
            val backupBundle = bundleOf(
                "name" to uri,
                "data" to gson.toJson(MockBackup(uri))
            )
            every { call("backup", any(), any()) } returns bundleOf(
                "backup" to backupBundle
            )
            every { call("restore", any(), any()) } answers {
                val extras = thirdArg<Bundle>()
                val result = if(extras.getString("name") == uri){
                    val dataJson = extras.getString("data")
                    val data = gson.fromJson(dataJson, MockBackup::class.java)
                    data.data == uri
                }else false
                bundleOf("success" to result)
            }
            every { call("get_targets_config", any(), any()) } answers {
                SmartspacerTargetProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON
                ).toBundle()
            }
            every { call("get_actions_config", any(), any()) } answers {
                SmartspacerComplicationProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON
                ).toBundle()
            }
            every { call("get_requirement_config", any(), any()) } answers {
                SmartspacerRequirementProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON
                ).toBundle()
            }
        }
    }

    private val targetsRepositoryMock = mock<TargetsRepository>()
    private val requirementsRepositoryMock = mock<RequirementsRepository>()
    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()
    private val expandedRepositoryMock = mock<ExpandedRepository>()

    private val testFile by lazy {
        File(actualContext.cacheDir, "backup.tmp")
    }

    private val testFileUri by lazy {
        DocumentFile.fromFile(testFile).uri
    }

    override val sut by lazy {
        BackupRepositoryImpl(
            actualContext, //Required for file reading/writing
            get(),
            targetsRepositoryMock,
            requirementsRepositoryMock,
            databaseRepositoryMock,
            settingsRepositoryMock,
            expandedRepositoryMock
        )
    }

    override fun Context.context() {
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            createMockContentProviderClient(firstArg<Uri>().toString())
        }
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            createMockContentProviderClient(firstArg())
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<String>())
        } answers {
            createMockContentProviderClient(firstArg<String>().toString())
        }
        every {
            contentResolver.registerContentObserver(any(), any(), any())
        } just Runs
    }

    @Test
    @UiThreadTest
    fun testCreateBackupFailedToCreateFile() = runTest {
        mockkStatic(DocumentFile::class)
        every { DocumentFile.fromSingleUri(any(), any()) } returns null
        sut.createBackup(testFileUri).test {
            assertTrue(awaitItem() == CreatingBackup)
            assertTrue(
                awaitItem() == SmartspacerBackupProgress.Error(ErrorReason.FAILED_TO_CREATE_FILE)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @UiThreadTest
    fun testCreateRestoreBackupSuccess() = runTest {
        val mockTargets = getMockTargets()
        val mockComplications = getMockComplications()
        val mockRequirements = getMockRequirements()
        val mockDatabaseTargets = getMockDatabaseTargets(mockTargets, mockRequirements.take(2))
        val mockDatabaseComplications =
            getMockDatabaseComplications(mockComplications, mockRequirements.subList(2, 4))
        val mockCustomWidgets = getMockExpandedCustomWidgets()
        val mockSettings = getMockSettingsBackup()
        every { targetsRepositoryMock.getAvailableTargets() } returns flowOf(mockTargets)
        every { targetsRepositoryMock.getAvailableComplications() } returns flowOf(mockComplications)
        every { requirementsRepositoryMock.getAllInUseRequirements() } returns flowOf(mockRequirements)
        every { databaseRepositoryMock.getTargets() } returns flowOf(mockDatabaseTargets)
        every { databaseRepositoryMock.getActions() } returns flowOf(mockDatabaseComplications)
        coEvery { expandedRepositoryMock.getExpandedCustomWidgetBackups() } returns mockCustomWidgets
        coEvery { settingsRepositoryMock.getBackup() } returns mockSettings
        sut.createBackup(testFileUri).test {
            assertTrue(awaitItem() == CreatingBackup)
            var next: SmartspacerBackupProgress
            next = assertProgressItem<CreatingTargetsBackup, CreatingComplicationsBackup>(
                awaitItem()
            ) { it.progress }
            next = assertProgressItem<CreatingComplicationsBackup, CreatingRequirementsBackup>(
                next
            ) { it.progress }
            next = assertProgressItem<CreatingRequirementsBackup, CreatingCustomWidgetsBackup>(
                next
            ) { it.progress }
            assertTrue(next is CreatingCustomWidgetsBackup)
            assertTrue(awaitItem() is CreatingSettingsBackup)
            assertTrue(awaitItem() is WritingFile)
            assertTrue(awaitItem() is Finished)
            awaitComplete()
        }
        val backup = sut.loadBackup(testFileUri)
        assertTrue(backup is LoadBackupResult.Success)
        backup as LoadBackupResult.Success
        with(backup.backup) {
            assertTrue(targetBackups.isNotEmpty())
            assertTrue(complicationBackups.isNotEmpty())
            assertTrue(requirementBackups.isNotEmpty())
            assertTrue(expandedCustomWidgets.isNotEmpty())
            assertTrue(settings.isNotEmpty())
            targetBackups.zip(mockTargets).forEach {
                val actual = it.first
                val mock = it.second
                assertTrue(actual == mock.createBackup())
            }
            complicationBackups.zip(mockComplications).forEach {
                val actual = it.first
                val mock = it.second
                assertTrue(actual == mock.createBackup())
            }
            requirementBackups.zip(mockRequirements).forEach {
                val actual = it.first
                val mock = it.second
                val target = mockDatabaseTargets.firstOrNull { target ->
                    target.allRequirements.contains(mock.id) ||
                            target.anyRequirements.contains(mock.id)
                }
                val complication = mockDatabaseComplications.firstOrNull { complication ->
                    complication.allRequirements.contains(mock.id) ||
                            complication.anyRequirements.contains(mock.id)
                }
                val requirementType = when {
                    target != null && target.allRequirements.contains(mock.id) -> {
                        RequirementType.ALL
                    }
                    target != null && target.anyRequirements.contains(mock.id) -> {
                        RequirementType.ANY
                    }
                    complication != null && complication.allRequirements.contains(mock.id) -> {
                        RequirementType.ALL
                    }
                    complication != null && complication.anyRequirements.contains(mock.id) -> {
                        RequirementType.ANY
                    }
                    else -> throw AssertionError("Failed to find requirement")
                }
                val mockBackup = mock.createBackup(
                    requirementType,
                    target?.id ?: complication?.id!!
                )
                assertTrue(actual == mockBackup)
            }
            expandedCustomWidgets.zip(mockCustomWidgets).forEach {
                val actual = it.first
                val mock = it.second
                assertTrue(actual == mock)
            }
            settings.entries.zip(mockSettings.entries).forEach {
                assertTrue(it.first.key == it.second.key)
                assertTrue(it.first.value == it.first.value)
            }
        }
    }

    private fun getMockTargets(): List<Target> {
        return listOf(
            createMockTarget(),
            createMockTarget(),
            createMockTarget()
        )
    }

    private fun createMockTarget(): Target {
        return Target(
            contextMock,
            randomString(),
            randomString(),
            randomString(),
            Target.Config(
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),
                randomBoolean()
            )
        )
    }

    /**
     *  Creates a list of mock database targets equivalent to [mockTargets], linking the
     *  [mockRequirements] to their index's database target as the any requirements.
     */
    private fun getMockDatabaseTargets(
        mockTargets: List<Target>,
        mockRequirements: List<Requirement>
    ): List<DatabaseTarget> {
        return mockTargets.mapIndexed { index, target ->
            createMockDatabaseTarget(index, target, mockRequirements.getOrNull(index))
        }
    }

    private fun createMockDatabaseTarget(
        index: Int,
        mockTarget: Target,
        mockRequirement: Requirement?
    ): DatabaseTarget {
        return DatabaseTarget(
            mockTarget.id ?: randomString(),
            mockTarget.authority,
            index,
            mockTarget.sourcePackage,
            setOfNotNull(mockRequirement?.id),
            emptySet(),
            mockTarget.config.showOnHomeScreen,
            mockTarget.config.showOnLockScreen,
            mockTarget.config.showOverMusic,
            mockTarget.config.showRemoteViews,
            mockTarget.config.showWidget,
            mockTarget.config.showShortcuts,
            mockTarget.config.showAppShortcuts
        )
    }

    /**
     *  Creates a list of mock database complications equivalent to [mockComplications], linking the
     *  [mockRequirements] to their index's database complication as the all requirements.
     */
    private fun getMockDatabaseComplications(
        mockComplications: List<Complication>,
        mockRequirements: List<Requirement>
    ): List<DatabaseComplication> {
        return mockComplications.mapIndexed { index, action ->
            createMockDatabaseComplication(index, action, mockRequirements.getOrNull(index))
        }
    }

    private fun createMockDatabaseComplication(
        index: Int,
        mockComplication: Complication,
        mockRequirement: Requirement?
    ): DatabaseComplication {
        return DatabaseComplication(
            mockComplication.id ?: randomString(),
            mockComplication.authority,
            index,
            mockComplication.sourcePackage,
            setOfNotNull(mockRequirement?.id),
            emptySet(),
            mockComplication.config.showOnHomeScreen,
            mockComplication.config.showOnLockScreen,
            mockComplication.config.showOverMusic
        )
    }

    private fun getMockComplications(): List<Complication> {
        return listOf(
            createMockComplication(),
            createMockComplication(),
            createMockComplication()
        )
    }

    private fun createMockComplication(): Complication {
        return Complication(
            contextMock,
            randomString(),
            randomString(),
            randomString(),
            Complication.Config(
                randomBoolean(),
                randomBoolean(),
                randomBoolean(),

            )
        )
    }

    private fun getMockRequirements(): List<Requirement> {
        return listOf(
            createMockRequirement(),
            createMockRequirement(),
            createMockRequirement(),
            createMockRequirement()
        )
    }

    private fun createMockRequirement(): Requirement {
        return Requirement(
            contextMock,
            randomString(),
            randomString(),
            randomBoolean(),
            randomString()
        )
    }

    private fun getMockExpandedCustomWidgets(): List<ExpandedCustomWidgetBackup> {
        return listOf(
            createMockCustomWidgetBackup(),
            createMockCustomWidgetBackup(),
            createMockCustomWidgetBackup()
        )
    }

    private fun createMockCustomWidgetBackup(): ExpandedCustomWidgetBackup {
        return ExpandedCustomWidgetBackup(
            randomString(),
            randomString(),
            randomInt(),
            randomInt(),
            randomInt(),
            randomBoolean()
        )
    }

    private fun getMockSettingsBackup(): Map<String, String> {
        return mapOf(
            randomString() to randomString(),
            randomString() to randomString(),
            randomString() to randomString()
        )
    }

    private suspend inline fun <reified P, reified N> ReceiveTurbine<SmartspacerBackupProgress>
            .assertProgressItem(current: SmartspacerBackupProgress, progress: (P) -> Int): N {
        var previousItem: P? = null
        var currentItem = current
        while(true){
            //Break out if the next item type is encountered
            if(currentItem is N) return currentItem
            //Check that the current item is still the progress item
            assertTrue(currentItem is P)
            //Check that the progress of the current item is greater or equal to that of the previous
            previousItem?.let {
                assertTrue(progress(currentItem as P) >= progress(it))
            }
            //Save and move on
            previousItem = currentItem as P
            currentItem = awaitItem()
        }
    }

    data class MockBackup(@SerializedName("data") val data: String)

}