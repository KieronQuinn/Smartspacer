package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.app.KeyguardManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.PowerManager
import android.os.Process
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.AddState
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget.ExpandedAddWidgetBottomSheetViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.extensions.WidgetCategory
import com.kieronquinn.app.smartspacer.utils.extensions.getApplicationInfo
import com.kieronquinn.app.smartspacer.utils.extensions.providerInfo
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpandedAddWidgetBottomSheetViewModelTests: BaseTest<ExpandedAddWidgetBottomSheetViewModel>() {

    companion object {
        //For simplicity, all widgets belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private fun getMockWidget(): AppWidgetProviderInfo {
            return AppWidgetProviderInfo().apply {
                targetCellWidth = 4
                targetCellHeight = 1
                provider = ComponentName(MOCK_PACKAGE, randomString())
                providerInfo = ActivityInfo().apply {
                    nonLocalizedLabel = provider.packageName
                    applicationInfo = ApplicationInfo().apply {
                        uid = Process.myUid()
                    }
                }
                configure = ComponentName(MOCK_PACKAGE, randomString())
            }
        }
    }

    private val mockWidgets = listOf(getMockWidget(), getMockWidget(), getMockWidget())

    private val widgetRepositoryMock = mock<WidgetRepository> {
        every { providers } returns MutableStateFlow(mockWidgets)
    }

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        coEvery { getExpandedCustomAppWidgets() } returns flowOf(emptyList())
    }

    private val expandedRepositoryMock = mock<ExpandedRepository>()
    private val powerManagerMock = mock<PowerManager>()
    private val keyguardManagerMock = mock<KeyguardManager>()

    override val sut by lazy {
        ExpandedAddWidgetBottomSheetViewModelImpl(
            contextMock,
            widgetRepositoryMock,
            expandedRepositoryMock,
            databaseRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every { packageManager.getApplicationInfo(any()) } returns mock()
        every { packageManager.getApplicationLabel(any()) } returns "App"
        every { packageManager.getText(any(), any(), any()) } answers {
            firstArg()
        }
        every { getSystemService(Context.POWER_SERVICE) } returns powerManagerMock
        every { getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManagerMock
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            //Should only be the app dropdown
            assertTrue(item.items.size == 1)
            //Expand the first app
            val app = item.items.first { it is Item.App } as Item.App
            sut.onExpandClicked(app)
            //Should now be widgets size + app + header
            val expandedItem = awaitItem()
            assertTrue(expandedItem is State.Loaded)
            expandedItem as State.Loaded
            assertTrue(expandedItem.items.size == 1 + mockWidgets.size)
        }
    }

    @Test
    fun testAddState() = runTest {
        sut.addState.test {
            val mockWidget = mockWidgets.first()
            val parent = Item.App("Widget", "Widget", "Widget", null)
            val widget = Item.Widget(parent, WidgetCategory.OTHERS, "Widget", "Description", mockWidget)
            sut.onWidgetClicked(widget, 5, 2)
            verify {
                expandedRepositoryMock.allocateAppWidgetId()
            }
            sut.addState.assertOutputs<AddState, AddState.BindWidget>()
            sut.onWidgetBindResult(true)
            sut.addState.assertOutputs<AddState, AddState.ConfigureWidget>()
            sut.onWidgetConfigureResult(true)
            coVerify {
                databaseRepositoryMock.addExpandedCustomAppWidget(any())
            }
            sut.addState.assertOutputs<AddState, AddState.Dismiss>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testBindAppWidgetIfAllowed() = runTest {
        val provider = ComponentName(randomString(), randomString())
        sut.bindAppWidgetIfAllowed(provider, 0)
        verify {
            expandedRepositoryMock.bindAppWidgetIdIfAllowed(0, provider)
        }
    }

    @Test
    fun testCreateConfigIntentSender() = runTest {
        sut.createConfigIntentSender(0)
        verify {
            expandedRepositoryMock.createConfigIntentSender(0)
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
            sut.setSearchTerm("")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testShowSearchClear() = runTest {
        sut.showSearchClear.test {
            sut.setSearchTerm("")
            assertFalse(awaitItem())
            sut.setSearchTerm(randomString())
            assertTrue(awaitItem())
        }
    }

}