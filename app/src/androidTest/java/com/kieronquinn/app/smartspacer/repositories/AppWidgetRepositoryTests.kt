package com.kieronquinn.app.smartspacer.repositories

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import androidx.test.annotation.UiThreadTest
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSession
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.utils.appWidgetManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test

class AppWidgetRepositoryTests: BaseTest<AppWidgetRepository>() {

    companion object {
        private fun getMockWidgets(): List<AppWidget> {
            return listOf(
                AppWidget(
                    1,
                    "com.example.one",
                    UiSurface.HOMESCREEN,
                    TintColour.AUTOMATIC,
                    multiPage = false,
                    showControls = false
                ),
                AppWidget(
                    2,
                    "com.example.two",
                    UiSurface.LOCKSCREEN,
                    TintColour.AUTOMATIC,
                    multiPage = false,
                    showControls = false
                )
            )
        }
    }

    private var screenOn = false
    private var keyguardLocked = false

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val wallpaperRepositoryMock = mock<WallpaperRepository>()
    private val accessibilityRepositoryMock = mock<AccessibilityRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()

    private val powerManagerMock = mock<PowerManager> {
        every { isInteractive } answers { screenOn }
    }

    private val keyguardManagerMock = mockk<KeyguardManager> {
        every { isKeyguardLocked } answers { keyguardLocked }
    }

    private val activityManagerMock = mockk<ActivityManager> {
        every { getRunningServices(any()) } returns listOf(
            ActivityManager.RunningServiceInfo().apply {
                service = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    SmartspacerAccessibiltyService::class.java.name
                )
            }
        )
    }

    private val appWidgetManagerMock = appWidgetManager(mock())

    override val sut by lazy {
        AppWidgetRepositoryImpl(
            contextMock,
            databaseRepositoryMock,
            wallpaperRepositoryMock,
            accessibilityRepositoryMock,
            notificationRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every { getSystemService(Context.POWER_SERVICE) } returns powerManagerMock
        every { getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManagerMock
        every { getSystemService(Context.ACTIVITY_SERVICE) } returns activityManagerMock
        every { getSystemService(Context.AUDIO_SERVICE) } returns mock<AudioManager>()
    }

    @Test
    fun testLockscreenShowing() = runTest {
        screenOn = false
        keyguardLocked = true
        sut.isLockscreenShowing.test {
            //Screen off, lockscreen not showing
            assertFalse(awaitItem())
            //Screen on, lockscreen showing
            screenOn = true
            contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
            assertTrue(awaitItem())
            //Screen on, lockscreen not showing
            screenOn = true
            keyguardLocked = false
            contextMock.sendBroadcast(Intent(Intent.ACTION_USER_PRESENT))
            assertFalse(awaitItem())
            //Screen off again, lock screen not showing
            screenOn = false
            keyguardLocked = true
            contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testScreenOff() = runTest {
        screenOn = false
        keyguardLocked = false
        sut.screenOff.test {
            assertTrue(awaitItem())
            screenOn = true
            contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
            assertFalse(awaitItem())
            screenOn = false
            contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
            assertTrue(awaitItem())
        }
    }

    @Test
    fun testLookupSession() = runTest {
        sut.widgetSessions.add(createMockSession(1, "com.example.one"))
        sut.widgetSessions.add(createMockSession(2, "com.example.two"))
        assertTrue(sut.getPagedSessionState(1)!!.config.packageName == "com.example.one")
        assertTrue(sut.getPagedSessionState(2)!!.config.packageName == "com.example.two")
    }

    @Test
    fun testHasAppWidget() = runTest {
        val mockAppWidgets = getMockWidgets()
        every { databaseRepositoryMock.getAppWidgets() } returns flowOf(mockAppWidgets)
        assertTrue(sut.hasAppWidget("com.example.one"))
        assertTrue(sut.hasAppWidget("com.example.two"))
        assertFalse(sut.hasAppWidget("com.example.three"))
    }

    @Test
    fun testAddWidget() = runTest {
        sut.addWidget(
            1,
            "com.example.one",
            UiSurface.HOMESCREEN,
            TintColour.AUTOMATIC,
            multiPage = false,
            showControls = false
        )
        coVerify(exactly = 1) {
            databaseRepositoryMock.addAppWidget(
                AppWidget(
                    1,
                    "com.example.one",
                    UiSurface.HOMESCREEN,
                    TintColour.AUTOMATIC,
                    multiPage = false,
                    showControls = false
                )
            )
        }
    }

    @Test
    fun testDeleteWidget() = runTest {
        val mockAppWidgets = getMockWidgets()
        every { databaseRepositoryMock.getAppWidgets() } returns flowOf(mockAppWidgets)
        sut.deleteAppWidget(1)
        coVerify(exactly = 1) {
            databaseRepositoryMock.deleteAppWidget(mockAppWidgets[0])
        }
    }

    @Test
    fun testGetWidget() = runTest {
        val mockAppWidget = getMockWidgets().first()
        coEvery { databaseRepositoryMock.getAppWidgets() } returns flowOf(listOf(mockAppWidget))
        assertTrue(sut.getAppWidget(1) == mockAppWidget)
    }

    @Test
    fun testMigrateWidget() = runTest {
        val mockAppWidgets = getMockWidgets()
        every { databaseRepositoryMock.getAppWidgets() } returns flowOf(mockAppWidgets)
        sut.migrateAppWidget(1, 3)
        coVerify(exactly = 1) {
            databaseRepositoryMock.deleteAppWidget(mockAppWidgets.first())
        }
        val migrated = mockAppWidgets.first().copy(appWidgetId = 3)
        coVerify(exactly = 1) {
            databaseRepositoryMock.addAppWidget(migrated)
        }
    }

    @Test
    fun testSupportsPinWidget() = runTest {
        every { appWidgetManagerMock.isRequestPinAppWidgetSupported } returns true
        assertTrue(sut.supportsPinAppWidget())
        every { appWidgetManagerMock.isRequestPinAppWidgetSupported } returns false
        assertFalse(sut.supportsPinAppWidget())
    }

    @Test
    fun testPinAppWidget() = runTest {
        sut.requestPinAppWidget("")
        verify {
            appWidgetManagerMock.requestPinAppWidget(any(), any(), any())
        }
    }

    @Test
    fun testAppWidgetUpdate() = runTest {
        val mockAppWidgets = getMockWidgets()
        every { databaseRepositoryMock.getAppWidgets() } returns flowOf(mockAppWidgets)
        sut.newAppWidgetIdBus.test {
            //Should not emit when already existing ID
            sut.onAppWidgetUpdate(1)
            expectNoEvents()
            //Should emit when new ID
            sut.onAppWidgetUpdate(3)
            assertTrue(awaitItem() == 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @UiThreadTest
    @Ignore("Currently broken, gets stuck on completion for no apparent reason")
    fun testWidgetSessions() = runTest {
        val mockAppWidgets = getMockWidgets()
        val widgets = MutableStateFlow<List<AppWidget>>(emptyList())
        val wallpaperDarkTextColour = MutableStateFlow(false)
        every { databaseRepositoryMock.getAppWidgets() } returns widgets
        every {
            wallpaperRepositoryMock.homescreenWallpaperDarkTextColour
        } returns wallpaperDarkTextColour
        assertTrue(sut.widgetSessions.isEmpty())
        widgets.emit(mockAppWidgets)
        delay(500L)
        assertTrue(sut.widgetSessions.size == mockAppWidgets.size)
        //Test a change to wallpaper text colour refreshes widgets
        var firstSessionHash = sut.widgetSessions.first().hashCode()
        wallpaperDarkTextColour.emit(true)
        assertFalse(sut.widgetSessions.first().hashCode() == firstSessionHash)
        //Test updating widgets recreates sessions
        firstSessionHash = sut.widgetSessions.first().hashCode()
        val variantMockWidgets = mockAppWidgets.take(1)
        widgets.emit(variantMockWidgets)
        delay(250L)
        assertTrue(sut.widgetSessions.size == 1)
        assertFalse(sut.widgetSessions.first().hashCode() == firstSessionHash)
    }

    @Test
    fun testPackageStateHomescreen() = runTest {
        screenOn = true
        keyguardLocked = false
        val foregroundPackage = MutableStateFlow("")
        every { accessibilityRepositoryMock.foregroundPackage } returns foregroundPackage
        val sessionOne = mock<WidgetSmartspacerSession> {
            every { surface } returns UiSurface.HOMESCREEN
            every { packageName } returns "com.example.one"
        }
        sut.widgetSessions.add(sessionOne)
        val sessionTwo = mock<WidgetSmartspacerSession> {
            every { surface } returns UiSurface.HOMESCREEN
            every { packageName } returns "com.example.two"
        }
        sut.widgetSessions.add(sessionTwo)
        val lockscreenSession = mock<WidgetSmartspacerSession> {
            every { surface } returns UiSurface.LOCKSCREEN
            every { packageName } returns "com.android.systemui"
        }
        sut.widgetSessions.add(lockscreenSession)
        //Make sure no calls to show are made before emitting any package
        verify(inverse = true) {
            sessionOne.notifySmartspaceEvent(any())
        }
        verify(inverse = true) {
            sessionTwo.notifySmartspaceEvent(any())
        }
        verify(inverse = true) {
            lockscreenSession.notifySmartspaceEvent(any())
        }
        //Make sure session is set to visible when the package is set
        foregroundPackage.emit("com.example.one")
        verify(exactly = 1) {
            sessionOne.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
        verify(inverse = true) {
            sessionTwo.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
        verify(inverse = true) {
            lockscreenSession.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
        //Make sure session is set back to hidden when unset
        foregroundPackage.emit("com.example.three")
        verify(exactly = 1) {
            sessionOne.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_HIDDEN)
            )
        }
        //Make sure session is not set back to visible when foreground package is set but screen off
        screenOn = false
        contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        foregroundPackage.emit("com.example.one")
        verify(exactly = 1) {
            sessionOne.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
    }

    @Test
    fun testPackageStateLockscreen() = runTest {
        screenOn = true
        keyguardLocked = false
        val foregroundPackage = MutableStateFlow("")
        every { accessibilityRepositoryMock.foregroundPackage } returns foregroundPackage
        val lockscreenSession = mock<WidgetSmartspacerSession> {
            every { surface } returns UiSurface.LOCKSCREEN
            every { packageName } returns "com.android.systemui"
        }
        sut.widgetSessions.add(lockscreenSession)
        val sessionOne = mock<WidgetSmartspacerSession> {
            every { surface } returns UiSurface.HOMESCREEN
            every { packageName } returns "com.example.one"
        }
        sut.widgetSessions.add(sessionOne)
        //Make sure no calls to show are made before setting screen off
        verify(inverse = true) {
            lockscreenSession.notifySmartspaceEvent(any())
        }
        verify(inverse = true) {
            sessionOne.notifySmartspaceEvent(any())
        }
        //Set screen off + verify shown event is called but only for lockscreen session
        screenOn = true
        keyguardLocked = true
        contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        delay(500L)
        verify {
            lockscreenSession.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
        verify(inverse = true) {
            sessionOne.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
        //Set screen back on + verify shown is called again (lockscreen will be visible)
        screenOn = true
        contextMock.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
        verify(inverse = true) {
            sessionOne.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
        //Set unlocked and verify hide is called
        screenOn = true
        keyguardLocked = false
        contextMock.sendBroadcast(Intent(Intent.ACTION_USER_PRESENT))
        verify {
            lockscreenSession.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_HIDDEN)
            )
        }
        verify(inverse = true) {
            sessionOne.notifySmartspaceEvent(
                createBlankTargetEvent(SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN)
            )
        }
    }

    @Test
    fun testAccessibilityNotification() {
        sut.showAccessibilityNotification()
        verify {
            notificationRepositoryMock.showNotification(
                NotificationId.ENABLE_ACCESSIBILITY,
                NotificationChannel.ACCESSIBILITY,
                any()
            )
        }
    }

    private fun createMockSession(id: Int, packageName: String): PagedWidgetSmartspacerSession {
        val config = SmartspaceConfig(0, UiSurface.HOMESCREEN, packageName)
        val state = PagedWidgetSmartspacerSessionState(
            mock(),
            config,
            animate = true,
            isFirst = true,
            isLast = false,
            isOnlyPage = true,
            showControls = true,
            invisibleControls = false,
            dotConfig = emptyList()
        )
        return mock {
            every { appWidgetId } returns id
            every { this@mock.state } returns state
        }
    }

    private fun createBlankTargetEvent(event: Int): SmartspaceTargetEvent {
        return SmartspaceTargetEvent(null, null, event)
    }

}