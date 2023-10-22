package com.kieronquinn.app.smartspacer.ui.screens.expanded.options

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.expanded.options.ExpandedWidgetOptionsBottomSheetViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomFloat
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpandedWidgetOptionsBottomSheetViewModelTests: BaseTest<ExpandedWidgetOptionsBottomSheetViewModel>() {

    companion object {
        private fun getMockWidget(): ExpandedCustomAppWidget {
            return ExpandedCustomAppWidget(
                randomString(),
                randomInt(),
                randomString(),
                randomInt(),
                randomInt(),
                randomInt(),
                true
            )
        }
    }

    private val mockWidgets = listOf(getMockWidget(), getMockWidget(), getMockWidget())
    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val powerManagerMock = mock<PowerManager>()
    private val keyguardManagerMock = mock<KeyguardManager>()

    private val expandedRepositoryMock = mock<ExpandedRepository> {
        every { expandedCustomAppWidgets } returns flowOf(mockWidgets)
    }

    override val sut by lazy {
        ExpandedWidgetOptionsBottomSheetViewModelImpl(
            contextMock,
            databaseRepositoryMock,
            expandedRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every { getSystemService(Context.POWER_SERVICE) } returns powerManagerMock
        every { getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManagerMock
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithWidgetId(mockWidgets.first().appWidgetId!!)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
        }
    }

    @Test
    fun testSetSpanX() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithWidgetId(mockWidgets.first().appWidgetId!!)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            val widget = sut.widget.value!!
            val mock = randomFloat()
            sut.setSpanX(mock)
            coVerify {
                databaseRepositoryMock
                    .updateExpandedCustomAppWidget(widget.copy(spanX = mock.toInt()))
            }
        }
    }

    @Test
    fun testSetSpanY() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithWidgetId(mockWidgets.first().appWidgetId!!)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            val widget = sut.widget.value!!
            val mock = randomFloat()
            sut.setSpanY(mock)
            coVerify {
                databaseRepositoryMock
                    .updateExpandedCustomAppWidget(widget.copy(spanY = mock.toInt()))
            }
        }
    }

    @Test
    fun testSetShowWhenLocked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithWidgetId(mockWidgets.first().appWidgetId!!)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            val widget = sut.widget.value!!
            val mock = randomFloat()
            sut.setShowWhenLocked(false)
            coVerify {
                databaseRepositoryMock
                    .updateExpandedCustomAppWidget(widget.copy(showWhenLocked = false))
            }
        }
    }

}