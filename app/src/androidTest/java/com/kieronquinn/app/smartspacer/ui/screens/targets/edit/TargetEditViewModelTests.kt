package com.kieronquinn.app.smartspacer.ui.screens.targets.edit

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
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.model.database.Widget.Type
import com.kieronquinn.app.smartspacer.model.smartspace.Widget
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Companion.PACKAGE_PIXEL_LAUNCHER
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.targets.edit.TargetEditViewModel.State
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseRequirement")
class TargetEditViewModelTests: BaseTest<TargetEditViewModel>() {

    companion object {
        //For simplicity, all restore Targets belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockTarget(): Target {
            return Target(
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

    private val mockTarget = MutableStateFlow(getMockTarget())
    private val mockRequirements = getMockRequirements()
    private val mockWidgets = getMockWidgets()

    private val widgetRepositoryMock = mock<WidgetRepository> {
        every { widgets } returns MutableStateFlow(mockWidgets)
    }

    private val expandedRepositoryMock = mock<ExpandedRepository> {
        coEvery { removeAppWidget(any<String>()) } just runs
    }

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getTargetById(any()) } returns mockTarget
        every { getRequirementById(any()) } answers {
            flowOf(mockRequirements.firstOrNull { it.id == firstArg() })
        }
        coEvery { updateTargetConfig(any(), any()) } coAnswers {
            val lambda = secondArg<(Target) -> Unit>()
            val current = mockTarget.value.copy()
            lambda(current)
            mockTarget.emit(current)
        }
    }

    private val contentProviderClient = mock<ContentProviderClient> {
        every { call("get_targets_config", any(), any()) } answers {
            SmartspacerTargetProvider.Config(
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
        TargetEditViewModelImpl(
            contextMock,
            databaseRepositoryMock,
            widgetRepositoryMock,
            navigationMock,
            targetsRepositoryMock,
            oemRepositoryMock,
            expandedRepositoryMock,
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
            sut.setupWithTarget(mockTarget.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val target = item.target.target
            val smartspaceTarget = item.target.smartspaceTarget
            assertTrue(target == mockTarget.value)
            assertTrue(smartspaceTarget.id == mockTarget.value.id)
            assertTrue(smartspaceTarget.authority == mockTarget.value.authority)
            assertTrue(smartspaceTarget.sourcePackage == mockTarget.value.packageName)
            assertTrue(target.anyRequirements == mockTarget.value.anyRequirements)
            assertTrue(target.allRequirements == mockTarget.value.allRequirements)
            assertFalse(item.target.oemHomeAvailable)
            assertFalse(item.target.oemLockAvailable)
            assertTrue(item.target.nativeHomeAvailable)
            assertTrue(item.target.providerPackageLabel == "Label")
            assertTrue(item.target.target.showOnHomeScreen)
            assertTrue(item.target.target.showOnLockScreen)
            assertTrue(item.target.target.showOnMusic)
            assertTrue(item.target.target.showOnExpanded)
            assertTrue(item.target.target.expandedShowWhenLocked)
            sut.onShowOnHomeChanged(false)
            val itemWithHomeDisabled = awaitItem()
            itemWithHomeDisabled as State.Loaded
            assertFalse(itemWithHomeDisabled.target.target.showOnHomeScreen)
            sut.onShowOnLockChanged(false)
            val itemWithLockDisabled = awaitItem()
            itemWithLockDisabled as State.Loaded
            assertFalse(itemWithLockDisabled.target.target.showOnLockScreen)
            sut.onShowOnMusicChanged(false)
            val itemWithMusicDisabled = awaitItem()
            itemWithMusicDisabled as State.Loaded
            assertFalse(itemWithMusicDisabled.target.target.showOnMusic)
            sut.onShowOnExpandedChanged(false)
            val itemWithShowOnExpandedDisabled = awaitItem()
            itemWithShowOnExpandedDisabled as State.Loaded
            assertFalse(itemWithShowOnExpandedDisabled.target.target.showOnExpanded)
            sut.onExpandedShowWhenLockedChanged(false)
            val itemWithExpandedShowWhenLockedDisabled = awaitItem()
            itemWithExpandedShowWhenLockedDisabled as State.Loaded
            assertFalse(itemWithExpandedShowWhenLockedDisabled.target.target.expandedShowWhenLocked)
        }
    }

    @Test
    fun testOnRequirementsClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithTarget(mockTarget.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onRequirementsClicked()
            val target = mockTarget.value
            coVerify {
                navigationMock.navigate(
                    TargetEditFragmentDirections.actionTargetEditFragmentToTargetsRequirementsFragment(
                        target.id
                    )
                )
            }
        }
    }

    @Test
    fun testNotifyChangeAfterDelay() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithTarget(mockTarget.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.notifyChangeAfterDelay()
            val target = mockTarget.value
            coVerify {
                targetsRepositoryMock.notifyTargetChangeAfterDelay(
                    target.id,
                    target.authority
                )
            }
        }
    }

    @Test
    fun testOnDeleteClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithTarget(mockTarget.value)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onDeleteClicked()
            val target = mockTarget.value
            verify {
                contentProviderClient.call("on_removed", any(), any())
            }
            coVerify {
                databaseRepositoryMock.deleteTarget(target)
            }
            coVerify {
                mockWidgets.first().onDeleted()
            }
            coVerify {
                expandedRepositoryMock.removeAppWidget(any<String>())
            }
            coVerify {
                databaseRepositoryMock.deleteWidget(any(), Type.TARGET)
            }
            coVerify {
                databaseRepositoryMock.deleteNotificationListener(target.id)
            }
            coVerify {
                databaseRepositoryMock.deleteBroadcastListener(target.id)
            }
            target.allRequirements.forEach {
                verify {
                    databaseRepositoryMock.deleteRequirementData(it)
                }
                coVerify {
                    val requirement = mockRequirements.first { req -> req.id == it }
                    databaseRepositoryMock.deleteRequirement(requirement)
                }
            }
            target.anyRequirements.forEach {
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
        val target = mockTarget.value
        return listOf(
            mock {
                every { id } returns target.id
                every { type } returns Type.TARGET
            }
        )
    }

    private fun getMockRequirements(): List<Requirement> {
        val target = mockTarget.value
        return target.anyRequirements.map {
            Requirement(it, randomString(), randomString(), randomBoolean())
        } + target.allRequirements.map {
            Requirement(it, randomString(), randomString(), randomBoolean())
        }
    }

}