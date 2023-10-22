package com.kieronquinn.app.smartspacer.ui.screens.targets

import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ItemHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsViewModel.TargetHolder
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseTarget")
class TargetsViewModelTests: BaseTest<TargetsViewModel>() {

    companion object {
        //For simplicity, all Targets belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockTargets(): List<Target> {
            return listOf(
                getMockTarget(0),
                getMockTarget(1),
                getMockTarget(2)
            )
        }

        private fun getMockTarget(index: Int): Target {
            return Target(
                randomString(),
                randomString(),
                index,
                randomString()
            )
        }
    }

    private val navigationMock = mock<ContainerNavigation>()
    private val smartspaceRepositoryMock = mock<SmartspaceRepository>()

    private val enhancedModeMock = mockSmartspacerSetting(true)
    private val hasUsedNativeModeMock = mockSmartspacerSetting(true)
    private val donatePromptEnabledMock = mockSmartspacerSetting(false)
    private val donatePromptDismissedAtMock = mockSmartspacerSetting(0L)
    private val installTimeMock = mockSmartspacerSetting(0L)

    private val systemSmartspaceRepository = mock<SystemSmartspaceRepository> {
        every { serviceRunning } returns MutableStateFlow(false)
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { enhancedMode } returns enhancedModeMock
        every { hasUsedNativeMode } returns hasUsedNativeModeMock
        every { donatePromptEnabled } returns donatePromptEnabledMock
        every { donatePromptDismissedAt } returns donatePromptDismissedAtMock
        every { installTime } returns installTimeMock
    }

    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()
    private val mockTargets = getMockTargets()

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getTargets() } returns flowOf(mockTargets)
    }

    override val sut by lazy {
        TargetsViewModelImpl(
            contextMock,
            databaseRepositoryMock,
            navigationMock,
            smartspaceRepositoryMock,
            settingsRepositoryMock,
            systemSmartspaceRepository,
            scope,
            Dispatchers.Main
        )
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
        every { packageManagerMock.resolveContentProvider(any(), any<PackageManager.ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                packageName = MOCK_PACKAGE
            }
        }
        every { packageManagerMock.resolveActivity(any(), any<PackageManager.ResolveInfoFlags>()) } answers {
            ResolveInfo()
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            TestCase.assertTrue(awaitItem() is BaseManagerViewModel.State.Loading)
            val item = awaitItem()
            TestCase.assertTrue(item is BaseManagerViewModel.State.Loaded<*>)
            item as BaseManagerViewModel.State.Loaded<TargetHolder>
            //First item should be native start reminder
            TestCase.assertTrue(item.items.first() is ItemHolder.NativeStartReminder<*>)
            val items = item.items.subList(1, item.items.size)
            items.zip(mockTargets).forEach {
                val holder = it.first as ItemHolder.Item<TargetHolder>
                val actual = holder.item.target
                val mock = it.second
                TestCase.assertTrue(actual.authority == mock.authority)
                TestCase.assertTrue(actual.packageName == mock.packageName)
            }
        }
    }

    @Test
    fun testReloadClearingCache() = runTest {
        sut.targetInfoCache["key"] = mock()
        val loadBusValue = sut.loadBus.value
        sut.reloadClearingCache()
        TestCase.assertTrue(sut.targetInfoCache.isEmpty())
        TestCase.assertFalse(sut.loadBus.value == loadBusValue)
    }

    @Test
    fun testMoveItem() = runTest {
        sut.moveItem(0, 1)
        coVerify {
            databaseRepositoryMock.updateTarget(mockTargets[0])
        }
        coVerify {
            databaseRepositoryMock.updateTarget(mockTargets[1])
        }
    }

    @Test
    fun testAddItem() = runTest {
        val authority = randomString()
        val id = randomString()
        val packageName = randomString()
        val notificationAuthority = randomString()
        val broadcastAuthority = randomString()
        sut.addItem(authority, id, packageName, notificationAuthority, broadcastAuthority)
        coVerify {
            val notificationListener = NotificationListener(id, packageName, notificationAuthority)
            databaseRepositoryMock.addNotificationListener(notificationListener)
        }
        coVerify {
            val broadcastListener = BroadcastListener(id, packageName, broadcastAuthority)
            databaseRepositoryMock.addBroadcastListener(broadcastListener)
        }
        coVerify {
            val item = Target(id, authority, 3, packageName)
            databaseRepositoryMock.addTarget(item)
        }
    }

    @Test
    fun testOnAddClicked() = runTest {
        sut.onAddClicked()
        coVerify {
            navigationMock.navigate(
                TargetsFragmentDirections.actionTargetsFragmentToTargetsAddFragment()
            )
        }
    }

    @Test
    fun testOnItemClicked() = runTest {
        sut.state.test {
            TestCase.assertTrue(awaitItem() is BaseManagerViewModel.State.Loading)
            val item = awaitItem()
            TestCase.assertTrue(item is BaseManagerViewModel.State.Loaded<*>)
            item as BaseManagerViewModel.State.Loaded<TargetHolder>
            val holder = item.items[1] as ItemHolder.Item<TargetHolder>
            val itemToClick = holder.item
            sut.onItemClicked(itemToClick)
            coVerify {
                navigationMock.navigate(
                    TargetsFragmentDirections.actionTargetsFragmentToTargetEditFragment(
                        itemToClick.target
                    )
                )
            }
        }
    }

    @Test
    fun testOnStartNativeClicked() {
        sut.onStartNativeClicked()
        coVerify {
            navigationMock.navigate(Uri.parse("smartspacer://native"))
        }
    }

    @Test
    fun testOnWallpaperColourPickerClicked() {
        sut.onWallpaperColourPickerClicked()
        coVerify {
            navigationMock.navigate(
                TargetsFragmentDirections.actionTargetsFragmentToWallpaperColourPickerBottomSheetFragment()
            )
        }
    }
    
}