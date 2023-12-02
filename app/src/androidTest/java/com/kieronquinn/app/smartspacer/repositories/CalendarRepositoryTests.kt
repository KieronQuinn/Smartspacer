package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData.PreEventTime
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.Calendar
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.CalendarData
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.CalendarEvent
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CalendarRepositoryTests: BaseTest<CalendarRepository>() {

    companion object {
        private const val AUTHORITY_CALENDAR = "${BuildConfig.APPLICATION_ID}.target.calendar"

        private fun getMockCalendarData() = mapOf(
            getMockTargetData() to getMockCalendarEvents(),
            getMockTargetData(showAllDay = true) to getMockCalendarEvents(),
            getMockTargetData(showUnconfirmed = true) to getMockCalendarEvents(),
            getMockTargetData(preEventTime = PreEventTime.TWO_HOURS) to getMockCalendarEvents(),
        ).map {
            CalendarData(it.key, it.value)
        }

        private fun getMockTargetData(
            showAllDay: Boolean = false,
            showUnconfirmed: Boolean = false,
            preEventTime: PreEventTime = PreEventTime.ONE_HOUR
        ) = TargetData(
            randomString(),
            preEventTime,
            TargetData.PostEventTime.AT_END,
            showAllDay,
            true,
            showUnconfirmed,
            false,
            emptySet(),
            setOf()
        )

        private fun getMockCalendarEvents(): List<CalendarEvent> {
            val now = Instant.now()
            val startOfDay = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault())
                .toInstant()
            val endOfDay = LocalDate.now().plusDays(1).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toInstant()
            return listOf(
                //Event in the past (should never be shown)
                CalendarEvent(
                    Calendar(randomString(), randomString(), randomString()),
                    "past",
                    randomString(),
                    now.minus(1, ChronoUnit.HOURS),
                    now.minus(30, ChronoUnit.MINUTES),
                    randomString(),
                    false,
                    CalendarContract.Events.STATUS_CONFIRMED
                ),
                //Event in the future, confirmed (should always be shown)
                CalendarEvent(
                    Calendar(randomString(), randomString(), randomString()),
                    "confirmed",
                    randomString(),
                    now.plus(10, ChronoUnit.MINUTES),
                    now.plus(20, ChronoUnit.MINUTES),
                    randomString(),
                    false,
                    CalendarContract.Events.STATUS_CONFIRMED
                ),
                //Event in the future, unconfirmed (depends on calendar config)
                CalendarEvent(
                    Calendar(randomString(), randomString(), randomString()),
                    "unconfirmed",
                    randomString(),
                    now.plus(20, ChronoUnit.MINUTES),
                    now.plus(30, ChronoUnit.MINUTES),
                    randomString(),
                    false,
                    CalendarContract.Events.STATUS_TENTATIVE
                ),
                //All day event (depends on calendar config)
                CalendarEvent(
                    Calendar(randomString(), randomString(), randomString()),
                    "all-day",
                    randomString(),
                    startOfDay,
                    endOfDay,
                    randomString(),
                    true,
                    CalendarContract.Events.STATUS_CONFIRMED
                ),
                //Too far ahead (should not be shown unless time is extended)
                CalendarEvent(
                    Calendar(randomString(), randomString(), randomString()),
                    "far-ahead",
                    randomString(),
                    now.plus(70, ChronoUnit.MINUTES),
                    now.plus(80, ChronoUnit.MINUTES),
                    randomString(),
                    false,
                    CalendarContract.Events.STATUS_CONFIRMED
                )
            )
        }

        private fun getMockCalendars(): List<Calendar> {
            return listOf(
                Calendar(randomString(), randomString(), randomString()),
                Calendar(randomString(), randomString(), randomString()),
                Calendar(randomString(), randomString(), randomString())
            )
        }

        private fun List<Calendar>.asCursor(): Cursor {
            return MatrixCursor(
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME
                )
            ).apply {
                forEach {
                    addRow(arrayOf(it.id, it.name, it.account))
                }
            }
        }
    }

    private val dataRepositoryMock = mock<DataRepository>()
    private val contentProviderClient = mock<ContentProviderClient>()

    override val sut by lazy {
        CalendarRepositoryImpl(contextMock, dataRepositoryMock, scope)
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
    }

    @Test
    fun testHasPermission() = runTest {
        assertFalse(sut.hasPermission())
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        assertTrue(sut.hasPermission())
    }

    @Test
    fun testCheckPermission() = runTest {
        sut
        assertFalse(sut.hasPermission.value)
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        sut.checkPermission()
        assertTrue(sut.hasPermission.value)
    }

    @Test
    fun testReloadEvents() = runTest {
        val start = sut.reloadEventsBus.value
        sut.reloadEvents()
        assertFalse(sut.reloadEventsBus.value == start)
    }

    @Test
    fun testUpdateActiveCalendarEvents() = runTest {
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_CALENDAR
            }
        }
        sut.allCalendarEvents.clear()
        val mockData = getMockCalendarData()
        val mockCalendars = mockData.map {
            Pair(it.data.id, it)
        }.sortedBy {
            it.first
        }
        mockCalendars.forEach {
            sut.allCalendarEvents[it.first] = it.second
        }
        sut.updateActiveCalendarEvents()
        val actualCalendars = sut.activeCalendarEvents.entries.sortedBy {
            it.key
        }
        actualCalendars.zip(mockCalendars).forEach {
            val actual = it.first.value.events
            val mock = it.second.second.events
            val config = it.second.second.data
            val uri = createTargetUri(it.second.first)
            //Should never contain mock 1 (in the past)
            assertFalse(actual.contains(mock[0]))
            //Should always contain mock 2 (in the future, accepted, not all day)
            assertTrue(actual.contains(mock[1]))
            //Should only contain unconfirmed if set to show
            assertTrue(actual.contains(mock[2]) == config.showUnconfirmed)
            //Should only show all day if set to show
            assertTrue(actual.contains(mock[3]) == config.showAllDay)
            //Should only show too far ahead if set to higher time
            assertTrue(actual.contains(mock[4]) == (config.preEventTime != PreEventTime.ONE_HOUR))
        }
    }

    @Test
    fun testGetCalendarsNoPermission() = runTest {
        sut.getCalendars().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun testGetCalendars() = runTest {
        var mockCalendars = getMockCalendars()
        every {
            contentProviderClient.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            mockCalendars.asCursor()
        }
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        sut.checkPermission()
        sut.getCalendars().test {
            assertTrue(awaitItem().isEmpty())
            val actual = awaitItem()
            assertTrue(actual == mockCalendars)
            val previousCalendars = mockCalendars
            mockCalendars = getMockCalendars()
            contentResolver.notifyChange(CalendarContract.Calendars.CONTENT_URI)
            val actualAfterUpdate = awaitItem()
            assertFalse(previousCalendars == actualAfterUpdate)
            assertTrue(actualAfterUpdate == mockCalendars)
        }
    }

    @Test
    fun testGetCurrentCalendars() = runTest {
        val mockCalendars = getMockCalendars()
        every {
            contentProviderClient.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            mockCalendars.asCursor()
        }
        //Should start empty
        assertTrue(sut.getCurrentCalendars().isEmpty())
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        sut.checkPermission()
        sut.getCalendars().test {
            assertTrue(awaitItem().isEmpty())
            assertTrue(awaitItem().isNotEmpty())
        }
        //Should be updated to be filled
        assertTrue(sut.getCurrentCalendars() == mockCalendars)
    }

    @Test
    fun testGetNextCalendarTrigger() = runTest {
        sut.allCalendarEvents.clear()
        val mockData = getMockCalendarData()
        val mockCalendars = mockData.map {
            Pair(it.data.id, it)
        }.sortedBy {
            it.first
        }
        mockCalendars.forEach {
            sut.allCalendarEvents[it.first] = it.second
        }
        val actual = sut.getNextCalendarTrigger()
        val mock = mockData.first()
        assertTrue(actual?.time == mock.events[1].startTime)
    }

    @Test
    fun testGetCalendarEvents() = runTest {
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_CALENDAR
            }
        }
        sut.allCalendarEvents.clear()
        val mockData = getMockCalendarData()
        val mockCalendars = mockData.map {
            Pair(it.data.id, it)
        }.sortedBy {
            it.first
        }
        mockCalendars.forEach {
            sut.allCalendarEvents[it.first] = it.second
        }
        sut.updateActiveCalendarEvents()
        val actualCalendars = sut.activeCalendarEvents.entries.sortedBy {
            it.key
        }
        actualCalendars.zip(mockCalendars).forEach {
            val actual = it.first.value.events
            val mock = it.second.second.events
            val config = it.second.second.data
            //Should never contain mock 1 (in the past)
            assertFalse(actual.contains(mock[0]))
            //Should always contain mock 2 (in the future, accepted, not all day)
            assertTrue(actual.contains(mock[1]))
            //Should only contain unconfirmed if set to show
            assertTrue(actual.contains(mock[2]) == config.showUnconfirmed)
            //Should only show all day if set to show
            assertTrue(actual.contains(mock[3]) == config.showAllDay)
            //Should only show too far ahead if set to higher time
            assertTrue(actual.contains(mock[4]) == (config.preEventTime != PreEventTime.ONE_HOUR))
        }
    }

    private fun createTargetUri(id: String): Uri {
        return Uri.Builder().apply {
            scheme("content")
            authority(AUTHORITY_CALENDAR)
            appendPath(id)
        }.build()
    }

}