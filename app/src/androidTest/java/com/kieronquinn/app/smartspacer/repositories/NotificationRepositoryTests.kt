package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.companion.CompanionDeviceManager
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.service.SmartspacerNotificationListenerService
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel as NotificationsNotificationChannel

class NotificationRepositoryTests: BaseTest<NotificationRepository>() {

    companion object {
        private fun getMockNotificationChannels(): List<NotificationChannel> {
            return listOf(
                NotificationChannel(
                    randomString(),
                    randomString(),
                    randomInt(
                        NotificationManager.IMPORTANCE_NONE, NotificationManager.IMPORTANCE_HIGH
                    )
                ),
                NotificationChannel(
                    randomString(),
                    randomString(),
                    randomInt(
                        NotificationManager.IMPORTANCE_NONE, NotificationManager.IMPORTANCE_HIGH
                    )
                ),
                NotificationChannel(
                    randomString(),
                    randomString(),
                    randomInt(
                        NotificationManager.IMPORTANCE_NONE, NotificationManager.IMPORTANCE_HIGH
                    )
                )
            )
        }

        private fun getMockNotifications(): List<StatusBarNotification> {
            return listOf(mock(), mock(), mock())
        }

        private fun getMockNotificationListeners(): List<NotificationListener> {
            return listOf(
                NotificationListener(randomString(), "com.example.one", randomString()),
                NotificationListener(randomString(), "com.example.two", randomString()),
                NotificationListener(randomString(), "com.example.three", randomString())
            )
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val companionDeviceManagerMock = mock<CompanionDeviceManager>()
    private val notificationManagerMock = mock<NotificationManager>()
    private val mockNotificationListeners = MutableStateFlow(getMockNotificationListeners())
    private val contentProviderClient = mock<ContentProviderClient>()
    private val shizukuServiceRepository = mockShizukuRepository {}

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { notificationWidgetServiceEnabled } returns mockSmartspacerSetting(false)
    }

    private var notificationServiceRunning = false

    override val sut by lazy {
        NotificationRepositoryImpl(
            contextMock,
            shizukuServiceRepository,
            settingsRepositoryMock,
            databaseRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED
        every {
            getSystemService(Context.COMPANION_DEVICE_SERVICE)
        } returns companionDeviceManagerMock
        every {
            getSystemService(Context.NOTIFICATION_SERVICE)
        } returns notificationManagerMock
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
    }

    override fun setup() {
        super.setup()
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.getEnabledListenerPackages(any()) } answers {
            if(notificationServiceRunning){
                setOf(BuildConfig.APPLICATION_ID)
            }else emptySet()
        }
        every {
            databaseRepositoryMock.getNotificationListeners()
        } returns mockNotificationListeners
    }

    @Test
    fun testHasNotificationPermissionDenied() = runTest {
        assertFalse(sut.hasNotificationPermission())
    }

    @Test
    fun testHasNotificationPermissionGranted() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(sut.hasNotificationPermission())
    }

    @Test
    fun testDismissNotification() = runTest {
        sut.dismissNotificationBus.test {
            expectNoEvents()
            val mock = mock<StatusBarNotification>()
            sut.dismissNotification(mock)
            assertTrue(awaitItem() == mock)
        }
    }

    @Test
    fun testIsNotificationListenerEnabled() = runTest {
        assertFalse(sut.isNotificationListenerEnabled())
        notificationServiceRunning = true
        assertTrue(sut.isNotificationListenerEnabled())
    }

    @Test
    fun testGetNotificationChannelsAvailable() = runTest {
        every { companionDeviceManagerMock.myAssociations } returns emptyList()
        assertFalse(sut.getNotificationChannelsAvailable())
        every { companionDeviceManagerMock.myAssociations } returns listOf(mock())
        assertTrue(sut.getNotificationChannelsAvailable())
    }

    @Test
    fun testGetNotificationChannelsForPackage() = runTest {
        mockkObject(SmartspacerNotificationListenerService.Companion)
        val packageSlot = slot<String>()
        every {
            SmartspacerNotificationListenerService.getAllNotificationChannels(capture(packageSlot))
        } answers {
            getMockNotificationChannels()
        }
        val actual = sut.getNotificationChannelsForPackage("com.example.one")
        assertTrue(packageSlot.captured == "com.example.one")
        assertTrue(actual.isNotEmpty())
    }

    @Test
    fun testGetNotificationChannelsForPackageWithNoChannels() = runTest {
        mockkObject(SmartspacerNotificationListenerService.Companion)
        val packageSlot = slot<String>()
        every {
            SmartspacerNotificationListenerService.getAllNotificationChannels(capture(packageSlot))
        } answers {
            listOf(
                //Should be filtered out
                NotificationChannel(
                    "miscellaneous",
                    randomString(),
                    randomInt(
                        NotificationManager.IMPORTANCE_NONE, NotificationManager.IMPORTANCE_HIGH
                    )
                )
            )
        }
        val actual = sut.getNotificationChannelsForPackage("com.example.one")
        assertTrue(packageSlot.captured == "com.example.one")
        //Even though there is a channel, "miscellaneous" should be filtered out as invalid
        assertTrue(actual.isEmpty())
    }

    @Test
    fun testHasNotificationListener() = runBlocking {
        assertTrue(sut.hasNotificationListener("com.example.one"))
        assertFalse(sut.hasNotificationListener("com.example.four"))
        //Hack: Prevents "Dispatchers.Main is used concurrently with setting it" exception
        delay(2500L)
    }

    @Test
    fun testSetMirroredNotifications() = runTest {
        assertTrue(sut.mirroredNotifications.isEmpty())
        val id = randomString()
        val mock = getMockNotifications()
        sut.setMirroredNotifications(id, mock)
        assertTrue(sut.mirroredNotifications[id] == mock)
    }

    @Test
    fun testShowNotification() = runTest {
        sut.showNotification(NotificationId.UPDATES, NotificationsNotificationChannel.UPDATES) {}
        verify(exactly = 1) {
            notificationManagerMock.notify(NotificationId.UPDATES.ordinal, any())
        }
    }

    @Test
    fun testUpdateNotifications() = runTest {
        val notifications = getMockNotifications()
        sut.activeNotifications.test {
            assertTrue(awaitItem().isEmpty())
            sut.updateNotifications(notifications)
            assertTrue(awaitItem() == notifications)
        }
    }

    @Test
    fun testCancelNotification() = runTest {
        sut.cancelNotification(NotificationId.UPDATES)
        verify(exactly = 1) {
            notificationManagerMock.cancel(NotificationId.UPDATES.ordinal)
        }
    }

    @Test
    fun testShowShizukuNotification() = runTest {
        sut.showShizukuNotification(0)
        verify(exactly = 1) {
            notificationManagerMock.notify(NotificationId.SHIZUKU.ordinal, any())
        }
    }

}