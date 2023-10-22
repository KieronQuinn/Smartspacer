package com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker

import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.notification.apppicker.NotificationTargetConfigurationAppPickerViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationTargetConfigurationAppPickerViewModelTests: BaseTest<NotificationTargetConfigurationAppPickerViewModel>() {

    companion object {
        private const val AUTHORITY_NOTIFICATION = "${BuildConfig.APPLICATION_ID}.target.notification"

        private fun getMockTargetData(): TargetData {
            return TargetData(channels = setOf("channel"))
        }

        private fun getMockChannels(): List<NotificationChannel> {
            return listOf(mock(), mock(), mock())
        }

        private fun getMockApps(): List<ListAppsApp> {
            return listOf(
                ListAppsApp(randomString(), randomString()),
                ListAppsApp(randomString(), randomString()),
                ListAppsApp(randomString(), randomString())
            )
        }
    }

    private val channelsMock = getMockChannels()
    private val appsMock = getMockApps()
    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val mockId = randomString()
    private val navigationMock = mock<ConfigurationNavigation>()

    private val notificationRepositoryMock = mock<NotificationRepository> {
        every { getNotificationChannelsForPackage(any()) } returns channelsMock
        every { getNotificationChannelsAvailable() } returns false
        every { isNotificationListenerEnabled() } returns false
    }

    private val packageRepositoryMock = mock<PackageRepository> {
        coEvery { getInstalledApps(true) } returns appsMock
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
        NotificationTargetConfigurationAppPickerViewModelImpl(
            navigationMock,
            notificationRepositoryMock,
            dataRepositoryMock,
            packageRepositoryMock,
            scope
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
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.setSearchTerm(appsMock.first().packageName)
            val packageItem = awaitItem()
            assertTrue(packageItem is State.Loaded)
            packageItem as State.Loaded
            assertTrue(packageItem.apps == listOf(appsMock.first()))
            sut.setSearchTerm(appsMock[1].label.toString())
            val labelItem = awaitItem()
            assertTrue(labelItem is State.Loaded)
            labelItem as State.Loaded
            assertTrue(labelItem.apps == listOf(appsMock[1]))
            sut.setSearchTerm("")
            cancelAndIgnoreRemainingEvents()
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
            TestCase.assertFalse(awaitItem())
            sut.setSearchTerm(randomString())
            assertTrue(awaitItem())
        }
    }

    @Test
    fun testOnAppSelected() = runTest {
        val id = randomString()
        val packageName = randomString()
        sut.onAppSelected(id, packageName)
        assertTrue(targetDataMock.value.packageName == packageName)
        assertTrue(targetDataMock.value.channels.isEmpty())
        coVerify {
            navigationMock.navigateBack()
        }
    }

}