package com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime

import app.cash.turbine.test
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.TimeDateRequirement.TimeDateRequirementData
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.datetime.TimeDateConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class TimeDateConfigurationViewModelTests: BaseTest<TimeDateConfigurationViewModel>() {

    private val mockTargetData = TimeDateRequirementData(randomString())

    private val dataRepository = mock<DataRepository> {
        every {
            getRequirementData(any<String>(), TimeDateRequirementData::class.java)
        } returns mockTargetData
    }

    override val sut by lazy {
        TimeDateConfigurationViewModelImpl(
            dataRepository,
            Gson()
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.id == mockTargetData.id)
        }
    }

    @Test
    fun testOnChipClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onChipClicked(DayOfWeek.SUNDAY)
            val removedItem = awaitItem()
            assertTrue(removedItem is State.Loaded)
            removedItem as State.Loaded
            assertFalse(removedItem.selectedDays.contains(DayOfWeek.SUNDAY))
            sut.onChipClicked(DayOfWeek.SUNDAY)
            val addedItem = awaitItem()
            assertTrue(addedItem is State.Loaded)
            addedItem as State.Loaded
            assertTrue(addedItem.selectedDays.contains(DayOfWeek.SUNDAY))
        }
    }

    @Test
    fun testOnStartTimeSelectedError() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.errorBus.test {
                val time = LocalTime.of(18, 0)
                sut.onStartTimeSelected(time)
                assertTrue(awaitItem() != 0)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun testOnStartTimeSelectedSuccess() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val time = LocalTime.of(14, 0)
            sut.onStartTimeSelected(time)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.startTime == time)
            //Reset it back
            sut.onStartTimeSelected(LocalTime.of(9, 0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnEndTimeSelectedError() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.errorBus.test {
                val time = LocalTime.of(8, 0)
                sut.onEndTimeSelected(time)
                assertTrue(awaitItem() != 0)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun testOnEndTimeSelectedSuccess() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val time = LocalTime.of(10, 0)
            sut.onEndTimeSelected(time)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.endTime == time)
            //Reset it back
            sut.onEndTimeSelected(LocalTime.of(17, 0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnSaveClickedNoDays() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            //Unselect all days
            DayOfWeek.values().forEach {
                sut.onChipClicked(it)
            }
            sut.errorBus.test {
                sut.onSaveClicked()
                assertTrue(awaitItem() != 0)
            }
            //Reselect all days
            DayOfWeek.values().forEach {
                sut.onChipClicked(it)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnSaveClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockTargetData.id)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onSaveClicked()
            coVerify {
                dataRepository.addRequirementData(any())
            }
            assertTrue(awaitItem() == State.Success)
        }
    }

}