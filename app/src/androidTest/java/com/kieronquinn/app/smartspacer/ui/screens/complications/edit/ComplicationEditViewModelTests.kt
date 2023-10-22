package com.kieronquinn.app.smartspacer.ui.screens.complications.edit

import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.model.database.Widget.Type
import com.kieronquinn.app.smartspacer.model.smartspace.Widget
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Companion.PACKAGE_PIXEL_LAUNCHER
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.complications.edit.ComplicationEditViewModel.State
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseRequirement")
class ComplicationEditViewModelTests: BaseTest<ComplicationEditViewModel>() {

    companion object {
        //For simplicity, all restore Complications belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockAction(): Action {
            return Action(
                randomString(),
                randomString(),
                randomInt(),
                randomString(),
                setOf(randomString(), randomString()),
                setOf(randomString(), randomString()),
                showOnHomeScreen = true,
                showOnLockScreen = true,
                showOnExpanded = true,
                showOnMusic = true,
                expandedShowWhenLocked = true
            )
        }

        private val mockCompatibilityReports = listOf(
            CompatibilityReport(PACKAGE_PIXEL_LAUNCHER, 0, emptyList()),
            CompatibilityReport(PACKAGE_KEYGUARD, 0, emptyList())
        )

        private val mockCompatibleApps = listOf(
            ApplicationInfo().apply { packageName = PACKAGE_PIXEL_LAUNCHER },
            ApplicationInfo().apply { packageName = PACKAGE_KEYGUARD }
        )
    }

    private val navigationMock = mock<ContainerNavigation>()
    private val targetsRepositoryMock = mock<TargetsRepository>()

    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()

    private val expandedModeEnabledMock = mockSmartspacerSetting(true)
    private val enhancedModeMock = mockSmartspacerSetting(true)
    private val hasUsedNativeModeMock = mockSmartspacerSetting(true)

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { expandedModeEnabled } returns expandedModeEnabledMock
        every { enhancedMode } returns enhancedModeMock
        every { hasUsedNativeMode } returns hasUsedNativeModeMock
    }

    private val compatibilityRepository = mock<CompatibilityRepository> {
        coEvery { getCompatibilityReports() } returns mockCompatibilityReports
    }

    private val oemRepositoryMock = mock<OemSmartspacerRepository> {
        every { getCompatibleApps() } returns flowOf(mockCompatibleApps)
    }

    private val mockAction = MutableStateFlow(getMockAction())
    private val mockRequirements = getMockRequirements()
    private val mockWidgets = getMockWidgets()

    private val widgetRepositoryMock = mock<WidgetRepository> {
        every { widgets } returns MutableStateFlow(mockWidgets)
    }

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getActionById(any()) } returns mockAction
        every { getRequirementById(any()) } answers {
            flowOf(mockRequirements.firstOrNull { it.id == firstArg() })
        }
        coEvery { updateActionConfig(any(), any()) } coAnswers {
            val lambda = secondArg<(Action) -> Unit>()
            val current = mockAction.value.copy()
            lambda(current)
            mockAction.emit(current)
        }
    }

    private val contentProviderClient = mock<ContentProviderClient> {
        every { call("get_actions_config", any(), any()) } answers {
            SmartspacerComplicationProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON,
                setupActivity = mockSetupIntent,
                widgetProvider = mockWidgetProvider,
                notificationProvider = mockNotificationProvider,
                broadcastProvider = mockBroadcastProvider
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
        every { packageManagerMock.resolveActivity(any(), any<ResolveInfoFlags>()) } answers {
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = PACKAGE_PIXEL_LAUNCHER
                }
            }
        }
        every { packageManagerMock.getApplicationInfo(any(), any<ApplicationInfoFlags>()) } answers {
            ApplicationInfo()
        }
        every { packageManagerMock.getApplicationLabel(any()) } returns "Label"
    }

    override val sut by lazy {
        ComplicationEditViewModelImpl(
            contextMock,
            databaseRepositoryMock,
            navigationMock,
            targetsRepositoryMock,
            widgetRepositoryMock,
            oemRepositoryMock,
            settingsRepositoryMock,
            compatibilityRepository,
            scope,
            Dispatchers.Main
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithComplication(mockAction.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val complication = item.complication.complication
            val smartspaceComplication = item.complication.smartspaceComplication
            assertTrue(complication == mockAction.value)
            assertTrue(smartspaceComplication.id == mockAction.value.id)
            assertTrue(smartspaceComplication.authority == mockAction.value.authority)
            assertTrue(smartspaceComplication.sourcePackage == mockAction.value.packageName)
            assertTrue(complication.anyRequirements == mockAction.value.anyRequirements)
            assertTrue(complication.allRequirements == mockAction.value.allRequirements)
            assertFalse(item.complication.oemHomeAvailable)
            assertFalse(item.complication.oemLockAvailable)
            assertTrue(item.complication.nativeHomeAvailable)
            assertTrue(item.complication.providerPackageLabel == "Label")
            assertTrue(item.complication.complication.showOnHomeScreen)
            assertTrue(item.complication.complication.showOnLockScreen)
            assertTrue(item.complication.complication.showOnMusic)
            assertTrue(item.complication.complication.showOnExpanded)
            assertTrue(item.complication.complication.expandedShowWhenLocked)
            sut.onShowOnHomeChanged(false)
            val itemWithHomeDisabled = awaitItem()
            itemWithHomeDisabled as State.Loaded
            assertFalse(itemWithHomeDisabled.complication.complication.showOnHomeScreen)
            sut.onShowOnLockChanged(false)
            val itemWithLockDisabled = awaitItem()
            itemWithLockDisabled as State.Loaded
            assertFalse(itemWithLockDisabled.complication.complication.showOnLockScreen)
            sut.onShowOnMusicChanged(false)
            val itemWithMusicDisabled = awaitItem()
            itemWithMusicDisabled as State.Loaded
            assertFalse(itemWithMusicDisabled.complication.complication.showOnMusic)
            sut.onShowOnExpandedChanged(false)
            val itemWithShowOnExpandedDisabled = awaitItem()
            itemWithShowOnExpandedDisabled as State.Loaded
            assertFalse(itemWithShowOnExpandedDisabled.complication.complication.showOnExpanded)
            sut.onExpandedShowWhenLockedChanged(false)
            val itemWithExpandedShowWhenLockedDisabled = awaitItem()
            itemWithExpandedShowWhenLockedDisabled as State.Loaded
            assertFalse(itemWithExpandedShowWhenLockedDisabled.complication.complication.expandedShowWhenLocked)
        }
    }

    @Test
    fun testOnRequirementsClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithComplication(mockAction.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onRequirementsClicked()
            val complication = mockAction.value
            coVerify {
                navigationMock.navigate(
                    ComplicationEditFragmentDirections.actionComplicationEditFragmentToComplicationsRequirementsFragment(
                        complication.id
                    )
                )
            }
        }
    }

    @Test
    fun testNotifyChangeAfterDelay() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithComplication(mockAction.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.notifyChangeAfterDelay()
            val complication = mockAction.value
            coVerify {
                targetsRepositoryMock.notifyComplicationChangeAfterDelay(
                    complication.id,
                    complication.authority
                )
            }
        }
    }

    @Test
    fun testOnDeleteClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithComplication(mockAction.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onDeleteClicked()
            val complication = mockAction.value
            verify {
                contentProviderClient.call("on_removed", any(), any())
            }
            coVerify {
                databaseRepositoryMock.deleteAction(complication)
            }
            coVerify {
                mockWidgets.first().onDeleted()
            }
            coVerify {
                databaseRepositoryMock.deleteWidget(any(), Type.COMPLICATION)
            }
            coVerify {
                databaseRepositoryMock.deleteNotificationListener(complication.id)
            }
            coVerify {
                databaseRepositoryMock.deleteBroadcastListener(complication.id)
            }
            complication.allRequirements.forEach {
                verify {
                    databaseRepositoryMock.deleteRequirementData(it)
                }
                coVerify {
                    val requirement = mockRequirements.first { req -> req.id == it }
                    databaseRepositoryMock.deleteRequirement(requirement)
                }
            }
            complication.anyRequirements.forEach {
                verify {
                    databaseRepositoryMock.deleteRequirementData(it)
                }
                coVerify {
                    val requirement = mockRequirements.first { req -> req.id == it }
                    databaseRepositoryMock.deleteRequirement(requirement)
                }
            }
            coVerify { navigationMock.navigateBack() }
        }
    }

    private fun getMockWidgets(): List<Widget> {
        val action = mockAction.value
        return listOf(
            mock {
                every { id } returns action.id
                every { type } returns Type.COMPLICATION
            }
        )
    }

    private fun getMockRequirements(): List<Requirement> {
        val action = mockAction.value
        return action.anyRequirements.map {
            Requirement(it, randomString(), randomString(), randomBoolean())
        } + action.allRequirements.map {
            Requirement(it, randomString(), randomString(), randomBoolean())
        }
    }

}