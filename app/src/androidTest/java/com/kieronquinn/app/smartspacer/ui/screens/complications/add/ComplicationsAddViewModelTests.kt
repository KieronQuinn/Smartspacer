package com.kieronquinn.app.smartspacer.ui.screens.complications.add

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseAction")
class ComplicationsAddViewModelTests: BaseTest<ComplicationsAddViewModel>() {

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

    private val navigationMock = mock<ContainerNavigation>()
    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val targetsRepositoryMock = mock<TargetsRepository>()
    private val widgetRepositoryMock = mock<WidgetRepository>()
    private val grantRepositoryMock = mock<GrantRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()

    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()

    private val mockComplications by lazy {
        listOf(
            createMockComplication(contextMock),
            createMockComplication(contextMock),
            createMockComplication(contextMock),
            createMockComplication(contextMock)
        )
    }

    override val sut by lazy {
        ComplicationsAddViewModelImpl(
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
            ResolveInfo()
        }
    }

    override fun setup() {
        super.setup()
        every { targetsRepositoryMock.getAllComplications() } returns mockComplications
        every { databaseRepositoryMock.getActions() } returns flowOf(emptyList())
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
            assertTrue(awaitItem() is State.Loading)
            val itemBeforeCollapsed = awaitItem()
            assertTrue(itemBeforeCollapsed is State.Loaded)
            itemBeforeCollapsed as State.Loaded
            //Verify only item is app (collapsed state)
            assertTrue(itemBeforeCollapsed.items.all { it is Item.App })
            sut.onExpandClicked(itemBeforeCollapsed.items.first() as Item.App)
            val items = awaitItem()
            assertTrue(items is State.Loaded)
            items as State.Loaded
            val itemToSelect = items.items.first { it is Item.Complication }
            sut.onComplicationClicked(itemToSelect as Item.Complication)
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
            //Mimic a call to dismiss and verify the navigation goes back
            sut.dismiss()
            coVerify(exactly = 1) {
                navigationMock.navigateBack()
            }
        }
    }

    @Test
    fun testSearchTerm() = runTest {
        sut.searchTerm.test {
            assertTrue(awaitItem().isEmpty())
            val random = randomString()
            sut.setSearchTerm(random)
            assertTrue(awaitItem() == random)
            assertTrue(sut.getSearchTerm() == random)
        }
    }

    @Test
    fun testShowSearchClear() = runTest {
        sut.showSearchClear.test {
            sut.setSearchTerm("")
            assertFalse(awaitItem())
            sut.setSearchTerm(randomString())
            assertTrue(awaitItem())
        }
    }

    @Test
    fun testShowWidgetPermissionDialog() = runTest {
        val grant = mock<Grant>()
        sut.showWidgetPermissionDialog(grant)
        coVerify(exactly = 1) {
            navigationMock.navigate(
                ComplicationsAddFragmentDirections.actionComplicationsAddFragmentToWidgetPermissionDialog2(grant)
            )
        }
    }

    @Test
    fun testShowNotificationsPermissionDialog() = runTest {
        val grant = mock<Grant>()
        sut.showNotificationsPermissionDialog(grant)
        coVerify(exactly = 1) {
            navigationMock.navigate(
                ComplicationsAddFragmentDirections.actionComplicationsAddFragmentToNotificationPermissionDialogFragment3(grant)
            )
        }
    }

}