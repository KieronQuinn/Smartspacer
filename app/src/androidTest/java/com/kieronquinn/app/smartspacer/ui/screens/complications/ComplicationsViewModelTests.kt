package com.kieronquinn.app.smartspacer.ui.screens.complications

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
import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ItemHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel.State
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsViewModel.ComplicationHolder
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseAction")
class ComplicationsViewModelTests: BaseTest<ComplicationsViewModel>() {

    companion object {
        //For simplicity, all Complications belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockActions(): List<Action> {
            return listOf(
                getMockAction(0),
                getMockAction(1),
                getMockAction(2)
            )
        }

        private fun getMockAction(index: Int): Action {
            return Action(
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
    private val mockActions = getMockActions()

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getActions() } returns flowOf(mockActions)
    }

    override val sut by lazy {
        ComplicationsViewModelImpl(
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
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded<*>)
            item as State.Loaded<ComplicationHolder>
            //First item should be native start reminder
            assertTrue(item.items.first() is ItemHolder.NativeStartReminder<*>)
            val items = item.items.subList(1, item.items.size)
            items.zip(mockActions).forEach {
                val holder = it.first as ItemHolder.Item<ComplicationHolder>
                val actual = holder.item.complication
                val mock = it.second
                assertTrue(actual.authority == mock.authority)
                assertTrue(actual.packageName == mock.packageName)
            }
        }
    }

    @Test
    fun testReloadClearingCache() = runTest {
        sut.complicationInfoCache["key"] = mock()
        val loadBusValue = sut.loadBus.value
        sut.reloadClearingCache()
        assertTrue(sut.complicationInfoCache.isEmpty())
        assertFalse(sut.loadBus.value == loadBusValue)
    }

    @Test
    fun testMoveItem() = runTest {
        sut.moveItem(0, 1)
        coVerify {
            databaseRepositoryMock.updateAction(mockActions[0])
        }
        coVerify {
            databaseRepositoryMock.updateAction(mockActions[1])
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
            val item = Action(id, authority, 3, packageName)
            databaseRepositoryMock.addAction(item)
        }
    }

    @Test
    fun testOnAddClicked() = runTest {
        sut.onAddClicked()
        coVerify {
            navigationMock.navigate(
                ComplicationsFragmentDirections.actionComplicationsFragmentToComplicationsAddFragment()
            )
        }
    }

    @Test
    fun testOnItemClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            val item = awaitItem()
            assertTrue(item is State.Loaded<*>)
            item as State.Loaded<ComplicationHolder>
            val holder = item.items[1] as ItemHolder.Item<ComplicationHolder>
            val itemToClick = holder.item
            sut.onItemClicked(itemToClick)
            coVerify {
                navigationMock.navigate(
                    ComplicationsFragmentDirections.actionComplicationsFragmentToComplicationEditFragment(
                        itemToClick.complication
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
                ComplicationsFragmentDirections.actionComplicationsFragmentToWallpaperColourPickerBottomSheetFragment2()
            )
        }
    }

}