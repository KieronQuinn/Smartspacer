package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements

import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.core.os.bundleOf
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup.RequirementType
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseAction
import com.kieronquinn.app.smartspacer.model.database.Requirement as DatabaseRequirement
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget

@Suppress("CloseRequirement")
class RestoreRequirementsViewModelTests: BaseTest<RestoreRequirementsViewModel>() {

    companion object {
        //For simplicity, all restore Requirements belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun createMockRestoreConfig(): RestoreConfig {
            return RestoreConfig(
                shouldRestoreRequirements = true,
                hasRequirements = true,
                hasTargets = false,
                hasComplications = false,
                hasExpandedCustomWidgets = false,
                hasSettings = false,
                shouldRestoreComplications = false,
                shouldRestoreTargets = false,
                shouldRestoreSettings = false,
                shouldRestoreExpandedCustomWidgets = false,
                backup = createMockBackup()
            )
        }

        private fun createMockBackup(): BackupRepository.SmartspacerBackup {
            return BackupRepository.SmartspacerBackup(
                requirementBackups = listOf(
                    createMockRequirementBackup(),
                    createMockRequirementBackup(),
                    createMockRequirementBackup()
                ),
                targetBackups = emptyList(),
                expandedCustomWidgets = emptyList(),
                complicationBackups = emptyList(),
                settings = emptyMap()
            )
        }

        private fun createMockRequirementBackup(): Requirement.RequirementBackup {
            return Requirement.RequirementBackup(
                randomString(),
                randomString(),
                randomString(),
                Backup(randomString(), randomString()),
                RequirementType.values().random(),
                randomBoolean()
            )
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val requirementsRepositoryMock = mock<RequirementsRepository>()
    private val navigationMock = mock<ContainerNavigation>()

    private val mockConfig = createMockRestoreConfig()
    private val mockRequirements = flowOf(mockConfig.getMockRequirements())
    private val mockTargets = mockConfig.getMockTargets()
    private val mockComplications = mockConfig.getMockComplications()
    private val mockSetupIntent = mock<Intent>()

    override val sut by lazy {
        RestoreRequirementsViewModelImpl(
            databaseRepositoryMock,
            requirementsRepositoryMock,
            navigationMock,
            contextMock,
            scope,
            Dispatchers.Main
        )
    }

    private val contentProviderClient = mock<ContentProviderClient> {
        every { call("get_actions_config", any(), any()) } answers {
            SmartspacerComplicationProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON
            ).toBundle()
        }
        every { call("get_targets_config", any(), any()) } answers {
            SmartspacerTargetProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON
            ).toBundle()
        }
        every { call("get_requirement_config", any(), any()) } answers {
            SmartspacerRequirementProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON,
                setupActivity = mockSetupIntent
            ).toBundle()
        }
    }

    override fun Context.context() {
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<String>())
        } answers {
            contentProviderClient
        }
        every { packageManagerMock.resolveContentProvider(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                packageName = MOCK_PACKAGE
            }
        }
    }

    override fun setup() {
        super.setup()
        every { databaseRepositoryMock.getRequirements() } returns mockRequirements
        every { databaseRepositoryMock.getActions() } returns flowOf(mockComplications)
        every { databaseRepositoryMock.getTargets() } returns flowOf(mockTargets)
        every { databaseRepositoryMock.getActionById(any()) } returns flowOf(mockComplications.first())
        every { databaseRepositoryMock.getTargetById(any()) } returns flowOf(mockTargets.first())
        coEvery {
            requirementsRepositoryMock.performRequirementRestore(any(), any(), any(), any())
        } coAnswers {
            val requirement = Requirement(contextMock, firstArg(), secondArg(), thirdArg())
            requirement.restoreBackup(arg(3)).also {
                requirement.close()
            }
        }
    }

    @Test
    fun testState() = runTest {
        every { contentProviderClient.call("restore", any(), any()) } answers {
            bundleOf("success" to false) //Mimic requiring further setup
        }
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            sut.setupWithConfig(mockConfig)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            item.items.zip(mockConfig.backup.requirementBackups).forEachIndexed { index, i ->
                val actual = i.first
                val mock = i.second
                assertTrue(actual.id == mock.id)
                assertTrue(actual.authority == mock.authority)
                assertTrue(actual.packageName == MOCK_PACKAGE)
                //First item should be for the Target
                if(index == 0){
                    assertTrue(actual.requirementForTarget == mockTargets.first())
                }
                //Second item should be for the Action
                if(index == 1){
                    assertTrue(actual.requirementForComplication == mockComplications.first())
                }
                //Third item should be marked as incompatible as it's not attached
                if(index == 2){
                    assertTrue(actual.compatibilityState is CompatibilityState.Incompatible)
                }
            }
            //Add first item and make sure it gets removed after adding
            val firstItem = item.items[0]
            //Run restore
            sut.onRequirementClicked(firstItem)
            verify(exactly = 1) {
                contentProviderClient.call("restore", any(), any())
            }
            //This should return a configuration requirement as was mocked
            sut.addState.assertOutputs<AddState, AddState.Configure>()
            assertTrue(sut.addState.value is AddState.Configure)
            //Finalise
            sut.onRequirementClicked(firstItem, skipRestore = true, hasRestored = true)
            //Should now have been removed
            val itemsAfterAdded = awaitItem()
            itemsAfterAdded as State.Loaded
            assertTrue(itemsAfterAdded.items == item.items.minus(firstItem))
            //Target should've been updated to have the requirement
            val targetWithRequirement = mockTargets.first()
            coVerify(exactly = 1) {
                databaseRepositoryMock.updateTarget(targetWithRequirement)
            }
            every { contentProviderClient.call("restore", any(), any()) } answers {
                bundleOf("success" to true) //Mimic requiring no further setup
            }
            //Add second item, which is a complication
            val secondItem = item.items[1]
            sut.onRequirementClicked(secondItem)
            //No further config should have been required so we can just check it was updated
            val complicationWithRequirement = mockComplications.first()
            awaitItem()
            coVerify(exactly = 1) {
                databaseRepositoryMock.updateAction(complicationWithRequirement)
            }
        }
    }

    @Test
    fun testOnConfigureResult() = runTest {
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            sut.setupWithConfig(mockConfig)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.addState.value = AddState.Configure(item.items.first())
            sut.onConfigureResult(contextMock, false)
            verify(exactly = 1) {
                contentProviderClient.call("on_removed", any(), any())
            }
            sut.addState.value = AddState.Configure(item.items.first())
            sut.onConfigureResult(contextMock, true)
            coVerify {
                databaseRepositoryMock.addRequirement(any())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnNextClickedWidgets() = runTest {
        val config = mockConfig.copy(
            shouldRestoreExpandedCustomWidgets = true
        )
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(RestoreRequirementsFragmentDirections.actionRestoreRequirementsFragmentToRestoreWidgetsFragment(config))
        }
    }

    @Test
    fun testOnNextClickedSettings() = runTest {
        val config = mockConfig.copy(
            shouldRestoreSettings = true
        )
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(RestoreRequirementsFragmentDirections.actionRestoreRequirementsFragmentToRestoreSettingsFragment(config))
        }
    }

    @Test
    fun testOnNextClickedComplete() = runTest {
        sut.setupWithConfig(mockConfig)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(RestoreRequirementsFragmentDirections.actionRestoreRequirementsFragmentToRestoreCompleteFragment())
        }
    }

    private fun RestoreConfig.getMockRequirements(): List<DatabaseRequirement> {
        return backup.requirementBackups.map {
            DatabaseRequirement(it.id, it.authority, MOCK_PACKAGE, it.invert)
        }
    }

    private fun RestoreConfig.getMockTargets(): List<DatabaseTarget> {
        //First requirement mapped to a Target
        return listOf(
            DatabaseTarget(
                backup.requirementBackups[0].requirementFor,
                randomString(),
                0,
                MOCK_PACKAGE,
                anyRequirements = setOf(backup.requirementBackups[0].id)
            )
        )
    }

    private fun RestoreConfig.getMockComplications(): List<DatabaseAction> {
        //Second requirement mapped to a Complication
        return listOf(
            DatabaseAction(
                backup.requirementBackups[1].requirementFor,
                randomString(),
                0,
                MOCK_PACKAGE,
                allRequirements = setOf(backup.requirementBackups[1].id),
            )
        )
    }

}