package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets

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
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModel.State
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
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget

@Suppress("CloseTarget")
class RestoreTargetsViewModelTests: BaseTest<RestoreTargetsViewModel>() {

    companion object {
        //For simplicity, all restore Targets belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun createMockRestoreConfig(): BackupRepository.RestoreConfig {
            return BackupRepository.RestoreConfig(
                shouldRestoreTargets = true,
                hasTargets = true,
                shouldRestoreComplications = false,
                hasComplications = false,
                hasRequirements = false,
                hasExpandedCustomWidgets = false,
                hasSettings = false,
                shouldRestoreRequirements = false,
                shouldRestoreSettings = false,
                shouldRestoreExpandedCustomWidgets = false,
                backup = createMockBackup()
            )
        }

        private fun createMockBackup(): BackupRepository.SmartspacerBackup {
            return BackupRepository.SmartspacerBackup(
                targetBackups = listOf(
                    createMockTargetsBackup(),
                    createMockTargetsBackup(),
                    createMockTargetsBackup()
                ),
                complicationBackups = emptyList(),
                expandedCustomWidgets = emptyList(),
                requirementBackups = emptyList(),
                settings = emptyMap()
            )
        }

        private fun createMockTargetsBackup(): Target.TargetBackup {
            return Target.TargetBackup(
                randomString(),
                randomString(),
                Backup(randomString(), randomString()),
                Target.Config(randomBoolean(), randomBoolean(), randomBoolean())
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
    private val availableTargets = flowOf(mockConfig.getMockAvailableTargets())
    private val mockSetupIntent = mock<Intent>()
    private val mockWidgetProvider = randomString()
    private val mockNotificationProvider = randomString()
    private val mockBroadcastProvider = randomString()

    override val sut by lazy {
        RestoreTargetsViewModelImpl(
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
        every { call("get_targets_config", any(), any()) } answers {
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
        every { targetsRepositoryMock.getAvailableTargets() } returns availableTargets
        every { databaseRepositoryMock.getTargets() } returns flowOf(emptyList())
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
            assertTrue(awaitItem() == State.Loading)
            sut.setupWithConfig(mockConfig)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            item.items.zip(mockConfig.backup.targetBackups).forEachIndexed { index, i ->
                val actual = i.first as Item.Target
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
            //Mimic a call to onAdded (called from onDismiss) and check it removes the item
            sut.onAdded(target)
            val itemsAfterAdded = awaitItem()
            itemsAfterAdded as State.Loaded
            assertTrue(itemsAfterAdded.items == item.items.minus(secondItem))
            //Verify the target is added to the database
            val config = secondItem.backup!!.config
            val mockItem = DatabaseTarget(
                secondItem.id,
                secondItem.authority,
                1,
                secondItem.packageName,
                emptySet(),
                emptySet(),
                config.showOnHomeScreen,
                config.showOnLockScreen,
                config.showOverMusic
            )
            coVerify(exactly = 1) {
                databaseRepositoryMock.addTarget(any())
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
    fun testOnNextClickedComplications() = runTest {
        val config = createMockRestoreConfig().copy(shouldRestoreComplications = true)
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(
                RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreComplicationsFragment(config)
            )
        }
    }

    @Test
    fun testOnNextClickedRequirements() = runTest {
        val config = createMockRestoreConfig().copy(shouldRestoreRequirements = true)
        sut.setupWithConfig(config)
        sut.onNextClicked()
        coVerify(exactly = 1) {
            navigationMock.navigate(
                RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreRequirementsFragment(config)
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
                RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreWidgetsFragment(config)
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
                RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreSettingsFragment(config)
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
                RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreCompleteFragment()
            )
        }
    }

    private fun BackupRepository.RestoreConfig.getMockAvailableTargets(): List<Target> {
        return backup.targetBackups.first().let {
            listOf(Target(contextMock, it.authority, it.id, MOCK_PACKAGE, it.config))
        }
    }

}