package com.kieronquinn.app.smartspacer.repositories

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.TimeDateRequirement.TimeDateRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class AlarmRepositoryTests: BaseTest<AlarmRepository>() {

    companion object {
        private fun getMockRequirementData(): List<TimeDateRequirementData> {
            val now = LocalDateTime.now()
            return listOf(
                TimeDateRequirementData(
                    "2",
                    now.toLocalTime().plusHours(3),
                    now.toLocalTime().plusHours(4)
                ),
                TimeDateRequirementData(
                    "3",
                    now.toLocalTime().minusHours(2),
                    now.toLocalTime().minusHours(1)
                ),
                TimeDateRequirementData(
                    "1",
                    now.toLocalTime().plusHours(1),
                    now.toLocalTime().plusHours(2)
                )
            )
        }
    }

    private val alarmManagerMock = mock<AlarmManager>()
    private val powerManagerMock = mock<PowerManager>()
    private val dataRepositoryMock = mock<DataRepository>()

    private val notificationRepositoryMock = mock<NotificationRepository>()

    override fun Context.context() {
        every { getSystemService(Context.ALARM_SERVICE) } returns alarmManagerMock
        every { getSystemService(Context.POWER_SERVICE) } returns powerManagerMock
    }

    override val sut by lazy {
        AlarmRepositoryImpl(
            contextMock,
            notificationRepositoryMock,
            mock(),
            mock(),
            dataRepositoryMock,
            scope
        )
    }

    @Test
    fun canScheduleAlarmWithBatteryOptimisationDisabled() {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns true
        assertTrue(sut.canScheduleExactAlarm())
    }

    @Test
    fun cannotScheduleAlarmWithBatteryOptimisationEnabled() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns false
        assertFalse(sut.canScheduleExactAlarm())
    }

    @Test
    fun verifyTimeDateAlarmBusEmittedWhenEnqueueCalled() = runTest {
        sut.scheduleTimeDateRequirementAlarmBus.test {
            val startTime = awaitItem()
            sut.enqueueNextTimeDateRequirementReceiver()
            assertTrue(awaitItem() != startTime)
        }
    }

    @Test
    fun verifyCalendarAlarmBusEmittedWhenEnqueueCalled() = runTest {
        sut.scheduleCalendarTargetAlarmBus.test {
            val startTime = awaitItem()
            sut.enqueueNextCalendarTargetReceiver()
            assertTrue(awaitItem() != startTime)
        }
    }

    @Test
    fun verifyNotificationShownWhenDailyUpdateReceiverEnqueuedWithoutOptimisationDisabled() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns false
        sut.enqueueDailyUpdateReceiver()
        verify {
            notificationRepositoryMock.showNotification(
                NotificationId.BATTERY_OPTIMISATION,
                NotificationChannel.ERROR,
                any()
            )
        }
        verify(inverse = true) {
            alarmManagerMock.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun verifyDailyUpdateScheduledWhenOptimisationDisabled() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns true
        sut.enqueueDailyUpdateReceiver()
        verify(inverse = true) {
            notificationRepositoryMock.showNotification(
                NotificationId.BATTERY_OPTIMISATION,
                NotificationChannel.ERROR,
                any()
            )
        }
        val time = LocalDate.now().atStartOfDay().plusDays(1).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        verify {
            alarmManagerMock.setExactAndAllowWhileIdle(AlarmManager.RTC, time, any())
        }
    }

    @Test
    fun verifyTriggersCalledOnRequirementChanged() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns true
        sut.scheduleTimeDateRequirementAlarmBus.test {
            val startTime = awaitItem()
            sut.onRequirementChanged()
            assertTrue(awaitItem() != startTime)
        }
        sut.scheduleCalendarTargetAlarmBus.test {
            val startTime = awaitItem()
            sut.onRequirementChanged()
            assertTrue(awaitItem() != startTime)
        }
        val time = LocalDate.now().atStartOfDay().plusDays(1).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        verify {
            alarmManagerMock.setExactAndAllowWhileIdle(AlarmManager.RTC, time, any())
        }
    }

    @Test
    fun verifyTimeDateRequirements() = runTest {
        val requirements = MutableSharedFlow<List<TimeDateRequirementData>>()
        val mockData = getMockRequirementData()
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.TIME_DATE, TimeDateRequirementData::class.java
            )
        } returns requirements
        sut.timeDateRequirements.test {
            //Should start null
            assertTrue(awaitItem() == null)
            //Emit no items
            requirements.emit(emptyList())
            assertTrue(awaitItem()!!.isEmpty())
            //Emit mock items
            requirements.emit(mockData)
            assertTrue(awaitItem()!! == mockData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun verifyBatteryOptimisationChangedCall() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns true
        sut.setupBatteryOptimisationChange()
        val now = System.currentTimeMillis()
        contextMock.sendBroadcast(Intent("android.os.action.POWER_SAVE_WHITELIST_CHANGED"))
        //Verify busses have been updated to have a time newer than when the SUT was created
        assertTrue(sut.scheduleTimeDateRequirementAlarmBus.value > now)
        assertTrue(sut.scheduleCalendarTargetAlarmBus.value > now)
        //Verify an alarm call was made
        val time = LocalDate.now().atStartOfDay().plusDays(1).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        verify {
            alarmManagerMock.setExactAndAllowWhileIdle(AlarmManager.RTC, time, any())
        }
    }

    @Test
    fun verifyNextTimeDateAlarmScheduled() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns true
        val mockData = getMockRequirementData()
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.TIME_DATE, TimeDateRequirementData::class.java
            )
        } returns flowOf(mockData)
        sut.setupTimeDateRequirementAlarms()
        val time = with(sut) {
            mockData[2].getNextStartTriggerTime()
        }
        verify {
            alarmManagerMock.setExactAndAllowWhileIdle(AlarmManager.RTC, time, any())
        }
    }

    @Test
    fun verifyCorrectTriggerStartTimeForTimeDateRequirementToday() {
        val now = LocalDateTime.now()
        val todayRequirement = TimeDateRequirementData(
            UUID.randomUUID().toString(),
            now.toLocalTime().plusHours(1),
            now.toLocalTime().plusHours(2)
        )
        val triggerTime = now.plusHours(1).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        with(sut){
            val nextTriggerTime = todayRequirement.getNextStartTriggerTime()
            assertTrue(triggerTime == nextTriggerTime)
        }
    }

    @Test
    fun verifyCorrectTriggerEndTimeForTimeDateRequirementToday() {
        val now = LocalDateTime.now()
        val todayRequirement = TimeDateRequirementData(
            UUID.randomUUID().toString(),
            now.toLocalTime().minusHours(1),
            now.toLocalTime().plusHours(1)
        )
        val triggerTime = now.plusHours(1).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        with(sut){
            val nextTriggerTime = todayRequirement.getNextEndTriggerTime()
            assertTrue(triggerTime == nextTriggerTime)
        }
    }

    @Test
    fun verifyCorrectTriggerStartTimeForTimeDateRequirementTomorrow() {
        val now = LocalDateTime.now()
        val todayRequirement = TimeDateRequirementData(
            UUID.randomUUID().toString(),
            now.toLocalTime().minusHours(2),
            now.toLocalTime().minusHours(1)
        )
        val triggerTime = now.minusHours(2).plusDays(1)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        with(sut){
            val nextTriggerTime = todayRequirement.getNextStartTriggerTime()
            assertTrue(triggerTime == nextTriggerTime)
        }
    }

    @Test
    fun verifyNoRequirementsReturnedWhenEmpty() = runTest {
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.TIME_DATE, TimeDateRequirementData::class.java
            )
        } returns flowOf(emptyList())
        sut.getNextTimeDateRequirements().test {
            assertTrue(awaitItem() == null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun verifyRequirementsReturnedInOrder() = runTest {
        val requirements = getMockRequirementData()
        every {
            dataRepositoryMock.getRequirementData(
                RequirementDataType.TIME_DATE, TimeDateRequirementData::class.java
            )
        } returns flowOf(requirements)
        sut.getNextTimeDateRequirements().test {
            val item = awaitItem()!!
            assertTrue(item.second.first().id == "1")
            cancelAndIgnoreRemainingEvents()
        }
    }

}