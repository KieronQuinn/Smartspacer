package com.kieronquinn.app.smartspacer.ui.screens.setup.complications

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
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
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseAction

@Suppress("CloseAction")
class SetupComplicationsViewModelTests: BaseTest<SetupComplicationsViewModel>() {

    companion object {
        //For simplicity, all Complications belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun createMockComplication(context: Context): Action {
            return Action(
                context,
                randomString(),
                randomString(),
                config = Action.Config(randomBoolean(), randomBoolean(), randomBoolean())
            )
        }
    }

    private val navigationMock = mock<SetupNavigation>()
    private val targetsRepositoryMock = mock<TargetsRepository>()
    private val widgetRepositoryMock = mock<WidgetRepository>()
    private val grantRepositoryMock = mock<GrantRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getActions() } returns flowOf(emptyList())
    }

    private val compatibilityRepositoryMock = mock<CompatibilityRepository> {
        coEvery { getCompatibilityReports() } returns listOf(mock())
    }

    private val systemSmartspaceRepositoryMock = mock<SystemSmartspaceRepository> {
        every { serviceRunning } returns MutableStateFlow(true)
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { enhancedMode } returns mockSmartspacerSetting(true)
    }

    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()
    private val mockActions = MutableStateFlow(emptyList<DatabaseAction>())

    private val mockComplications by lazy {
        listOf(
            createMockComplication(contextMock),
            createMockComplication(contextMock),
            createMockComplication(contextMock),
            createMockComplication(contextMock)
        )
    }

    override val sut by lazy {
        SetupComplicationsViewModelImpl(
            contextMock,
            targetsRepositoryMock,
            databaseRepositoryMock,
            widgetRepositoryMock,
            grantRepositoryMock,
            navigationMock,
            compatibilityRepositoryMock,
            settingsRepositoryMock,
            systemSmartspaceRepositoryMock,
            notificationRepositoryMock,
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

    override fun setup() {
        super.setup()
        every { targetsRepositoryMock.getAvailableComplications() } returns flowOf(mockComplications)
        coEvery { targetsRepositoryMock.getRecommendedComplications() } returns mockComplications
        coEvery { databaseRepositoryMock.addAction(any()) } coAnswers {
            val current = mockActions.value
            mockActions.emit(current.plus(firstArg<DatabaseAction>()))
        }
        every { databaseRepositoryMock.getActions() } returns mockActions
        every { databaseRepositoryMock.getActionById(any()) } returns flowOf(null)
        every { widgetRepositoryMock.getWidgetInfo(any(), any()) } returns AppWidgetProviderInfo()
            .apply {
                configure = ComponentName("package_name", "class_name")
                provider = ComponentName("package_name", "class_name")
            }
    }

    @Test
    fun testState() = runTest {
        every { notificationRepositoryMock.isNotificationListenerEnabled() } returns false
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            //Add second item
            val secondItem = item.items[1] as Item.Complication
            sut.onComplicationClicked(secondItem)
            //Verify asking for notification permission
            sut.addState.assertOutputs<AddState, AddState.GrantNotificationPermission>()
            sut.onNotificationGrantResult(true)
            //Verify asking for notification permission
            sut.addState.assertOutputs<AddState, AddState.GrantNotificationListener>()
            every { notificationRepositoryMock.isNotificationListenerEnabled() } returns true
            sut.onNotificationListenerGrantResult(true)
            //Verify asking for widget permission
            sut.addState.assertOutputs<AddState, AddState.GrantWidgetPermission>()
            sut.onWidgetGrantResult(true)
            //Verify running configure
            sut.addState.assertOutputs<AddState, AddState.ConfigureComplication>()
            sut.onComplicationConfigureResult(true)
            //Verify bind widget
            sut.addState.assertOutputs<AddState, AddState.BindWidget>()
            sut.onWidgetBindResult(true)
            //Verify configure widget
            sut.addState.assertOutputs<AddState, AddState.ConfigureWidget>()
            sut.onWidgetConfigureResult(true)
            //Verify completed
            sut.addState.assertOutputs<AddState, AddState.Dismiss>()
            val complication = (sut.addState.value as AddState.Dismiss).complication
            //Mimic a call to addComplication (called from onDismiss) and check it removes the item
            sut.addComplication(complication)
            val itemsAfterAdded = awaitItem()
            itemsAfterAdded as State.Loaded
            val itemAfterAdded = itemsAfterAdded.items.first {
                it is Item.Complication && it.id == secondItem.id
            } as Item.Complication
            assertTrue(itemAfterAdded.isAdded)
            val mockItem = DatabaseAction(
                secondItem.id,
                secondItem.authority,
                1,
                secondItem.packageName,
                emptySet(),
                emptySet()
            )
            coVerify(exactly = 1) {
                databaseRepositoryMock.addAction(mockItem)
            }
        }
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupComplicationsFragmentDirections.actionSetupComplicationsFragmentToSetupBatteryOptimisationFragment()
            )
        }
    }

}