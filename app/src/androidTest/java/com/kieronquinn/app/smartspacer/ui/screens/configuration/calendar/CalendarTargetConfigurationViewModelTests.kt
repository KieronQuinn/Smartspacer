package com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import androidx.activity.result.ActivityResultLauncher
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.Calendar
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar.CalendarTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CalendarTargetConfigurationViewModelTests: BaseTest<CalendarTargetConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_CALENDAR = "${BuildConfig.APPLICATION_ID}.target.calendar"

        private fun getMockTargetData(): TargetData {
            return TargetData(randomString(), dismissedEvents = setOf("test"))
        }

        private fun getMockCalendars(): List<Calendar> {
            return listOf(
                Calendar(randomString(), randomString(), randomString()),
                Calendar(randomString(), randomString(), randomString())
            )
        }
    }

    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val calendarsMock = getMockCalendars()
    private val mockId = randomString()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.CALENDAR,
                any(),
                any()
            )
        } coAnswers  {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(targetDataMock.value)
            targetDataMock.emit(newData as TargetData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    private val calendarRepositoryMock = mock<CalendarRepository> {
        every { getCalendars() } returns flowOf(calendarsMock)
    }

    private val navigationMock = mock<ConfigurationNavigation>()

    override val sut by lazy {
        CalendarTargetConfigurationViewModelImpl(
            dataRepositoryMock,
            calendarRepositoryMock,
            navigationMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_CALENDAR
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.targetData == targetDataMock.value)
        }
    }

    @Test
    fun testReload() = runTest {
        sut.hasRequestedPermission = false
        sut.reload()
        verify {
            calendarRepositoryMock.checkPermission()
        }
        //Verify closes when previously asked for permission
        sut.hasRequestedPermission = true
        sut.reload()
        coVerify {
            navigationMock.finish()
        }
    }

    @Test
    fun testRequestPermission() = runTest {
        val launcher = mock<ActivityResultLauncher<String>>()
        sut.requestPermission(launcher)
        verify {
            launcher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    @Test
    fun testOnPermissionResultGranted() = runTest {
        sut.onPermissionResult(contextMock, true)
        verify {
            calendarRepositoryMock.checkPermission()
        }
    }

    @Test
    fun testOnPermissionResultFinish() = runTest {
        sut.hasRequestedPermission = true
        sut.onPermissionResult(contextMock, false)
        coVerify {
            navigationMock.finish()
        }
    }

    @Test
    fun testOnCalendarChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val mockCalendarId = calendarsMock.first().id
            sut.onCalendarChanged(mockCalendarId, true)
            val addedItem = awaitItem()
            assertTrue(addedItem is State.Loaded)
            addedItem as State.Loaded
            assertTrue(addedItem.targetData.calendars.contains(mockCalendarId))
            sut.onCalendarChanged(mockCalendarId, false)
            val removedItem = awaitItem()
            assertTrue(removedItem is State.Loaded)
            removedItem as State.Loaded
            assertFalse(removedItem.targetData.calendars.contains(mockCalendarId))
        }
    }

    @Test
    fun testOnShowAllDayChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onShowAllDayChanged(true)
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertTrue(updateItem.targetData.showAllDay)
        }
    }

    @Test
    fun testOnShowLocationChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onShowLocationChanged(false)
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertFalse(updateItem.targetData.showLocation)
        }
    }

    @Test
    fun testOnShowUnconfirmedChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onShowUnconfirmedChanged(true)
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertTrue(updateItem.targetData.showUnconfirmed)
        }
    }

    @Test
    fun testOnUseAlternativeIdsChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onUseAlternativeIdsChanged(true)
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertTrue(updateItem.targetData.useAlternativeEventIds)
        }
    }

    @Test
    fun testOnPreEventTimeChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onPreEventTimeChanged(TargetData.PreEventTime.ONE_HOUR)
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertTrue(updateItem.targetData.preEventTime == TargetData.PreEventTime.ONE_HOUR)
        }
    }

    @Test
    fun testOnPostEventTimeChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onPostEventTimeChanged(TargetData.PostEventTime.ONE_HOUR)
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertTrue(updateItem.targetData.postEventTime == TargetData.PostEventTime.ONE_HOUR)
        }
    }

    @Test
    fun testOnClearDismissEventsClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onClearDismissEventsClicked()
            val updateItem = awaitItem()
            assertTrue(updateItem is State.Loaded)
            updateItem as State.Loaded
            assertTrue(updateItem.targetData.dismissedEvents.isEmpty())
        }
    }

}