package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification

import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.NotificationTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationTargetConfigurationViewModelTests: BaseTest<NotificationTargetConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_NOTIFICATION = "${BuildConfig.APPLICATION_ID}.target.notification"

        private fun getMockTargetData(): TargetData {
            return TargetData()
        }

        private fun getMockChannels(): List<NotificationChannel> {
            return listOf(mock(), mock(), mock())
        }
    }

    private val channelsMock = getMockChannels()
    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val mockId = randomString()
    private val navigationMock = mock<ConfigurationNavigation>()

    private val notificationRepositoryMock = mock<NotificationRepository> {
        every { getNotificationChannelsForPackage(any()) } returns channelsMock
        every { getNotificationChannelsAvailable() } returns false
        every { isNotificationListenerEnabled() } returns false
    }

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.NOTIFICATION,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(targetDataMock.value)
            targetDataMock.emit(newData as TargetData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override val sut by lazy {
        NotificationTargetConfigurationViewModelImpl(
            notificationRepositoryMock,
            navigationMock,
            dataRepositoryMock,
            contextMock
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_NOTIFICATION
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.GrantNotificationAccess)
            every { notificationRepositoryMock.isNotificationListenerEnabled() } returns true
            sut.onResume()
            assertTrue(awaitItem() is State.GrantAssociation)
            every { notificationRepositoryMock.getNotificationChannelsAvailable() } returns true
            sut.onResume()
            assertTrue(awaitItem() is State.Settings)
        }
    }

    @Test
    fun testOnReadMoreClicked() {
        sut.onReadMoreClicked()
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnAppClicked() {
        sut.setupWithId(mockId)
        sut.onAppClicked()
        coVerify {
            navigationMock.navigate(
                NotificationTargetConfigurationFragmentDirections.actionNotificationTargetConfigurationFragmentToNotificationTargetConfigurationAppPickerFragment(mockId)
            )
        }
    }

    @Test
    fun testOnChannelChanged() = runTest {
        sut.setupWithId(mockId)
        val channelId = randomString()
        sut.onChannelChanged(channelId, true)
        assertTrue(targetDataMock.value.channels.contains(channelId))
        sut.onChannelChanged(channelId, false)
        assertFalse(targetDataMock.value.channels.contains(channelId))
    }

}