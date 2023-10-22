package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications

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
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.Action.ComplicationBackup
import com.kieronquinn.app.smartspacer.model.smartspace.Action.Config
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackup
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
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
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseAction

@Suppress("CloseAction")
class RestoreComplicationsViewModelTests: BaseTest<RestoreComplicationsViewModel>() {

    companion object {
        //For simplicity, all restore Complications belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun createMockRestoreConfig(): RestoreConfig {
            return RestoreConfig(
                shouldRestoreComplications = true,
                hasComplications = true,
                hasTargets = false,
                hasRequirements = false,
                hasExpandedCustomWidgets = false,
                hasSettings = false,
                shouldRestoreTargets = false,
                shouldRestoreRequirements = false,
                shouldRestoreSettings = false,
                shouldRestoreExpandedCustomWidgets = false,
                backup = createMockBackup()
            )
        }

        private fun createMockBackup(): SmartspacerBackup {
            return SmartspacerBackup(
                complicationBackups = listOf(
                    createMockComplicationBackup(),
                    createMockComplicationBackup(),
                    createMockComplicationBackup()
                ),
                targetBackups = emptyList(),
                expandedCustomWidgets = emptyList(),
                requirementBackups = emptyList(),
                settings = emptyMap()
            )
        }

        private fun createMockComplicationBackup(): ComplicationBackup {
            return ComplicationBackup(
                randomString(),
                randomString(),
                Backup(randomString(), randomString()),
                Config(randomBoolean(), randomBoolean(), randomBoolean())
            )
        }
    }

    private val navigationMock = mock<ContainerNavigation>()
    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val targetsRepositoryMock = mock<TargetsRepository>()
    private val widgetRepositoryMock = mock<WidgetRepository>()
    private val grantRepositoryMock = mock<GrantRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()

    private val mockConfig = createMockRestoreConfig()
    private val availableComplications = flowOf(mockConfig.getMockAvailableComplications())
    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()

    override val sut by lazy {
        RestoreComplicationsViewModelImpl(
            navigationMock,
            databaseRepositoryMock,
            contextMock,
            targetsRepositoryMock,
            widgetRepositoryMock,
            grantRepositoryMock,
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
        every { targetsRepositoryMock.getAvailableComplications() } returns availableComplications
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
            assertTrue(awaitItem() == State.Loading)
            sut.setupWithConfig(mockConfig)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            item.items.zip(mockConfig.backup.complicationBackups).forEachIndexed { index, i ->
                val actual = i.first as Item.Complication
                val mock = i.second
                assertTrue(actual.id == mock.id)
                assertTrue(actual.authority == mock.authority)
                assertTrue(actual.packageName == MOCK_PACKAGE)
                assertTrue(actual.backup!!.config == mock.config)
                assertTrue(actual.setupIntent == mockSetupIntent)
                assertTrue(actual.broadcastAuthority == mockBroadcastProvider)
                assertTrue(actual.notificationAuthority == mockNotificationProvider)
                assertTrue(actual.widgetAuthority == mockWidgetProvider)
                //First item should be flagged as duplicate
                if(index == 0){
                    assertTrue(actual.hasCollision)
                }
            }
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
            //Mimic a call to onAdded (called from onDismiss) and check it removes the item
            sut.onAdded(complication)
            val itemsAfterAdded = awaitItem()
            itemsAfterAdded as State.Loaded
            assertTrue(itemsAfterAdded.items == item.items.minus(secondItem))
            //Verify the complication is added to the database
            val config = secondItem.backup!!.config
            val mockItem = DatabaseAction(
                secondItem.id,
                secondItem.authority,
                1,
                secondItem.packageName,
                emptySet(),
                emptySet(),
                config.showOnHomeScreen,
                config.showOnLockScreen,
                config.showOnExpanded,
                config.showOverMusic
            )
            coVerify(exactly = 1) {
                databaseRepositoryMock.addAction(mockItem)
            }
            //Verify the notification authority was added
            val notificationListener = NotificationListener(
                secondItem.id, secondItem.packageName, secondItem.notificationAuthority!!
            )
            coVerify(exactly = 1) {
                databaseRepositoryMock.addNotificationListener(notificationListener)
            }
            //Verify the broadcast authority was added
            val broadcastListener = BroadcastListener(
                secondItem.id, secondItem.packageName, secondItem.broadcastAuthority!!
            )
            coVerify(exactly = 1) {
                databaseRepositoryMock.addBroadcastListener(broadcastListener)
            }
        }
    }

    @Test
    fun testOnNextClickedRequirements() = runTest {
        val config = createMockRestoreConfig().copy(shouldRestoreRequirements = true)
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(
                RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreRequirementsFragment(config)
            )
        }
    }

    @Test
    fun testOnNextClickedWidgets() = runTest {
        val config = createMockRestoreConfig().copy(shouldRestoreExpandedCustomWidgets = true)
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(
                RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreWidgetsFragment(config)
            )
        }
    }

    @Test
    fun testOnNextClickedSettings() = runTest {
        val config = createMockRestoreConfig().copy(shouldRestoreSettings = true)
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(
                RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreSettingsFragment(config)
            )
        }
    }

    @Test
    fun testOnNextClickedComplete() = runTest {
        val config = createMockRestoreConfig().copy()
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(
                RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreCompleteFragment()
            )
        }
    }

    private fun RestoreConfig.getMockAvailableComplications(): List<Action> {
        return backup.complicationBackups.first().let {
            listOf(Action(contextMock, it.authority, it.id, MOCK_PACKAGE, it.config))
        }
    }

}