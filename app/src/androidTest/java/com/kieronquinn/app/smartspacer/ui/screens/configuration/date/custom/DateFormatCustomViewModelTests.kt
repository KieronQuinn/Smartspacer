package com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.date.custom.DateFormatCustomViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DateFormatCustomViewModelTests: BaseTest<DateFormatCustomViewModel>() {

    override val sut by lazy {
        DateFormatCustomViewModelImpl(scope)
    }

    @Test
    fun testSetup() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            val randomInput = randomString()
            sut.setup(randomInput)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.format == randomInput)
        }
    }

    @Test
    fun testSetFormat() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            val randomInput = randomString()
            sut.setFormat(randomInput)
            sut.setup(randomString())
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.format == randomInput)
        }
    }

    @Test
    fun testValidDate() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            val validDate = "dd/MM/yyyy"
            sut.setup(validDate)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.date != null)
        }
    }

    @Test
    fun testInvalidDate() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            val invalidDate = randomString()
            sut.setup(invalidDate)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.date == null)
        }
    }

}