package com.kieronquinn.app.smartspacer.repositories

import android.content.ComponentName
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.ISmartspacerShizukuService
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Feature
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.service.SmartspacerSmartspaceService
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.extensions.Icon_createEmptyIcon
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SystemSmartspaceRepositoryTests: BaseTest<SystemSmartspaceRepository>() {

    companion object {
        private const val FAKE_SERVICE_RESOURCE_ID = 0xCAFE
        private const val FAKE_SERVICE_COMPONENT = "com.example.app/com.example.app.component"

        private fun createMockSmartspaceTarget(): SmartspaceTarget {
            return TargetTemplate.Basic(
                randomString(),
                ComponentName(randomString(), randomString()),
                SmartspaceTarget.FEATURE_UNDEFINED,
                Text(randomString()),
                Text(randomString()),
                Icon(Icon_createEmptyIcon()),
                null,
                null
            ).create()
        }
    }

    private lateinit var shizukuService: ISmartspacerShizukuService

    private val shizukuServiceRepositoryMock = mockShizukuRepository {
        //Save the mock so we can access it in tests
        shizukuService = this
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()
    private val compatibilityRepositoryMock = mock<CompatibilityRepository>()
    private val notificationRepository = mock<NotificationRepository>()

    override val sut by lazy {
        SystemSmartspaceRepositoryImpl(
            contextMock,
            shizukuServiceRepositoryMock,
            settingsRepositoryMock,
            compatibilityRepositoryMock,
            notificationRepository,
            scope
        )
    }

    @Test
    fun testSetService() = runTest {
        sut.setService()
        verify(exactly = 1) {
            shizukuService.setSmartspaceService(
                SmartspacerSmartspaceService.COMPONENT, any(), true, any()
            )
        }
    }

    @Test
    fun testResetService() = runTest {
        sut.notifyServiceRunning()
        assertTrue(sut.serviceRunning.value)
        every {
            resourcesMock.getIdentifier(
                "config_defaultSmartspaceService", "string", "android"
            )
        } returns FAKE_SERVICE_RESOURCE_ID
        every { resourcesMock.getString(FAKE_SERVICE_RESOURCE_ID) } returns FAKE_SERVICE_COMPONENT
        val component = ComponentName.unflattenFromString(FAKE_SERVICE_COMPONENT)
        sut.resetService(false, killSystemUi = false)
        assertFalse(sut.serviceRunning.value)
        sut.notifyServiceRunning()
        assertTrue(sut.serviceRunning.value)
        verify(exactly = 1) {
            shizukuService.setSmartspaceService(component, any(), false, any())
        }
        every {
            resourcesMock.getIdentifier(
                "config_defaultSmartspaceService", "string", "android"
            )
        } returns 0
        sut.resetService(false, killSystemUi = true)
        verify(exactly = 1) {
            shizukuService.clearSmartspaceService(any(), true, any())
        }
        assertFalse(sut.serviceRunning.value)
    }

    @Test
    fun testHomeTargets() = runTest {
        //Disable service start so we can test independently
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(false)
        val enhancedMode = mockSmartspacerSetting(false)
        every {
            settingsRepositoryMock.enhancedMode
        } returns enhancedMode
        val mockTargets = listOf(
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget()
        )
        sut._homeTargets.emit(mockTargets)
        sut.homeTargets.test {
            assertTrue(awaitItem().isEmpty())
            enhancedMode.emit(true)
            assertTrue(awaitItem() == mockTargets)
        }
    }

    @Test
    fun testLockTargets() = runTest {
        //Disable service start so we can test independently
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(false)
        val enhancedMode = mockSmartspacerSetting(false)
        every {
            settingsRepositoryMock.enhancedMode
        } returns enhancedMode
        val mockTargets = listOf(
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget()
        )
        sut._lockTargets.emit(mockTargets)
        sut.lockTargets.test {
            assertTrue(awaitItem().isEmpty())
            enhancedMode.emit(true)
            assertTrue(awaitItem() == mockTargets)
        }
    }

    @Test
    fun testMedia() = runTest {
        //Disable service start so we can test independently
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(false)
        val enhancedModeEnabled = mockSmartspacerSetting(false)
        every {
            settingsRepositoryMock.nativeShowMediaSuggestions
        } returns mockSmartspacerSetting(true)
        every {
            settingsRepositoryMock.enhancedMode
        } returns enhancedModeEnabled
        val mockTargets = listOf(
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget()
        )
        sut._mediaTargets.emit(mockTargets)
        sut.mediaTargets.test {
            assertTrue(awaitItem().isEmpty())
            enhancedModeEnabled.emit(true)
            assertTrue(awaitItem() == mockTargets)
        }
    }

    @Test
    fun testGlanceableHub() = runTest {
        //Disable service start so we can test independently
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(false)
        val enhancedModeEnabled = mockSmartspacerSetting(false)
        every {
            settingsRepositoryMock.enhancedMode
        } returns enhancedModeEnabled
        val mockTargets = listOf(
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget()
        )
        sut._hubTargets.emit(mockTargets)
        sut.hubTargets.test {
            assertTrue(awaitItem().isEmpty())
            enhancedModeEnabled.emit(true)
            assertTrue(awaitItem() == mockTargets)
        }
    }

    @Test
    fun testDismissTarget() = runTest {
        //Disable service start so we can test independently
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(false)
        every {
            settingsRepositoryMock.enhancedMode
        } returns mockSmartspacerSetting(true)
        val mockTargets = listOf(
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget(),
            createMockSmartspaceTarget()
        )
        sut._homeTargets.emit(mockTargets)
        sut._lockTargets.emit(mockTargets)
        val targetId = mockTargets.first().smartspaceTargetId
        sut.dismissDefaultTarget(targetId)
        val mock = mockTargets.subList(1, mockTargets.size)
        assertTrue(mock == sut._homeTargets.value)
        assertTrue(mock == sut._lockTargets.value)
    }

    @Test
    fun testNotifyServiceRunning() = runTest {
        sut.notifyServiceRunning()
        assertTrue(sut.serviceRunning.value)
    }

    @Test
    fun testShowNativeStartReminderIfNeeded() = runTest {
        val enhancedEnabled = mockSmartspacerSetting(false)
        val hasUsedNativeMode = mockSmartspacerSetting(false)
        val immediateStart = mockSmartspacerSetting(false)
        every {
            settingsRepositoryMock.enhancedMode
        } returns enhancedEnabled
        every {
            settingsRepositoryMock.hasUsedNativeMode
        } returns hasUsedNativeMode
        every {
            settingsRepositoryMock.nativeImmediateStart
        } returns immediateStart
        coEvery {
            compatibilityRepositoryMock.getCompatibilityReports()
        } returns emptyList()
        //All disabled, should not run
        sut.showNativeStartReminderIfNeeded()
        verify(inverse = true) {
            notificationRepository.showNotification(
                NotificationId.NATIVE_MODE,
                NotificationChannel.NATIVE_MODE,
                any()
            )
        }
        //Enhanced enabled
        enhancedEnabled.emit(true)
        sut.showNativeStartReminderIfNeeded()
        verify(inverse = true) {
            notificationRepository.showNotification(
                NotificationId.NATIVE_MODE,
                NotificationChannel.NATIVE_MODE,
                any()
            )
        }
        //Has used native enabled
        hasUsedNativeMode.emit(true)
        sut.showNativeStartReminderIfNeeded()
        verify(inverse = true) {
            notificationRepository.showNotification(
                NotificationId.NATIVE_MODE,
                NotificationChannel.NATIVE_MODE,
                any()
            )
        }
        //Compatibility reports contains item
        coEvery {
            compatibilityRepositoryMock.getCompatibilityReports()
        } returns listOf(
            CompatibilityReport(
                randomString(),
                randomString(),
                listOf(CompatibilityRepository.Compatibility(Feature.BASIC, true))
            )
        )
        hasUsedNativeMode.emit(true)
        sut.showNativeStartReminderIfNeeded()
        verify(exactly = 1) {
            notificationRepository.showNotification(
                NotificationId.NATIVE_MODE,
                NotificationChannel.NATIVE_MODE,
                any()
            )
        }
    }

}