package com.kieronquinn.app.smartspacer.ui.screens.setup.targets

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
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
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
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget

@Suppress("CloseTarget")
class SetupTargetsViewModelTests: BaseTest<SetupTargetsViewModel>() {

    companion object {
        //For simplicity, all Targets belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun createMockTarget(context: Context): Target {
            return Target(
                context,
                randomString(),
                randomString(),
                config = Target.Config(randomBoolean(), randomBoolean(), randomBoolean())
            )
        }
    }

    private val navigationMock = mock<SetupNavigation>()
    private val targetsRepositoryMock = mock<TargetsRepository>()
    private val widgetRepositoryMock = mock<WidgetRepository>()
    private val grantRepositoryMock = mock<GrantRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getTargets() } returns flowOf(emptyList())
    }

    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()
    private val mockTargets = MutableStateFlow(emptyList<DatabaseTarget>())

    private val mockTargetList by lazy {
        listOf(
            createMockTarget(contextMock),
            createMockTarget(contextMock),
            createMockTarget(contextMock),
            createMockTarget(contextMock)
        )
    }

    override val sut by lazy {
        SetupTargetsViewModelImpl(
            contextMock,
            targetsRepositoryMock,
            databaseRepositoryMock,
            widgetRepositoryMock,
            grantRepositoryMock,
            navigationMock,
            notificationRepositoryMock,
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

    override fun setup() {
        super.setup()
        every { targetsRepositoryMock.getAvailableTargets() } returns flowOf(mockTargetList)
        coEvery { targetsRepositoryMock.getRecommendedTargets() } returns mockTargetList
        coEvery { databaseRepositoryMock.addTarget(any()) } coAnswers {
            val current = mockTargets.value
            mockTargets.emit(current.plus(firstArg<DatabaseTarget>()))
        }
        every { databaseRepositoryMock.getTargets() } returns mockTargets
        every { databaseRepositoryMock.getTargetById(any()) } returns flowOf(null)
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
            val secondItem = item.items[1] as Item.Target
            sut.onTargetClicked(secondItem)
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
            sut.addState.assertOutputs<AddState, AddState.ConfigureTarget>()
            sut.onTargetConfigureResult(true)
            //Verify bind widget
            sut.addState.assertOutputs<AddState, AddState.BindWidget>()
            sut.onWidgetBindResult(true)
            //Verify configure widget
            sut.addState.assertOutputs<AddState, AddState.ConfigureWidget>()
            sut.onWidgetConfigureResult(true)
            //Verify completed
            sut.addState.assertOutputs<AddState, AddState.Dismiss>()
            val target = (sut.addState.value as AddState.Dismiss).target
            //Mimic a call to addTarget (called from onDismiss) and check it removes the item
            sut.addTarget(target)
            val itemsAfterAdded = awaitItem()
            itemsAfterAdded as State.Loaded
            val itemAfterAdded = itemsAfterAdded.items.first {
                it is Item.Target && it.id == secondItem.id
            } as Item.Target
            assertTrue(itemAfterAdded.isAdded)
            val mockItem = DatabaseTarget(
                secondItem.id,
                secondItem.authority,
                1,
                secondItem.packageName,
                emptySet(),
                emptySet()
            )
            coVerify(exactly = 1) {
                databaseRepositoryMock.addTarget(mockItem)
            }
        }
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupTargetsFragmentDirections.actionSetupTargetsFragmentToSetupComplicationsFragment()
            )
        }
    }

}