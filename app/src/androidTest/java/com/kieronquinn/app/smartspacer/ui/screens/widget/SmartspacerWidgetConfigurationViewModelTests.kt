package com.kieronquinn.app.smartspacer.ui.screens.widget

import android.app.ActivityManager
import android.app.ActivityManager.RunningServiceInfo
import android.content.ComponentName
import android.content.Context
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.ui.screens.widget.SmartspacerWidgetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("DEPRECATION")
class SmartspacerWidgetConfigurationViewModelTests: BaseTest<SmartspacerWidgetConfigurationViewModel>() {

    companion object {
        private fun createMockAppWidget(): AppWidget {
            return AppWidget(
                randomInt(),
                randomString(),
                UiSurface.HOMESCREEN,
                TintColour.AUTOMATIC,
                multiPage = false,
                showControls = false
            )
        }
    }

    private val appWidgetRepositoryMock = mock<AppWidgetRepository> {
        coEvery { getAppWidget(any()) } returns null
    }

    private val mockWidget = createMockAppWidget()

    private val activityManagerMock = mock<ActivityManager> {
        every { getRunningServices(Integer.MAX_VALUE) } returns listOf(
            RunningServiceInfo().apply {
                service = ComponentName(
                    BuildConfig.APPLICATION_ID, SmartspacerAccessibiltyService::class.java.name
                )
            }
        )
    }

    override val sut by lazy {
        SmartspacerWidgetConfigurationViewModelImpl(
            contextMock,
            appWidgetRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every { getSystemService(Context.ACTIVITY_SERVICE) } returns activityManagerMock
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.appWidget == mockWidget)
            assertTrue(item.isAccessibilityServiceEnabled)
            assertTrue(item.isPossiblyLockScreen)
        }
    }

    @Test
    fun testOnHomeClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onLockClicked()
            awaitItem()
            sut.onHomeClicked()
            val homeItem = awaitItem()
            assertTrue(homeItem is State.Loaded)
            homeItem as State.Loaded
            assertTrue(homeItem.appWidget.surface == UiSurface.HOMESCREEN)
        }
    }

    @Test
    fun testOnLockClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onLockClicked()
            val lockItem = awaitItem()
            assertTrue(lockItem is State.Loaded)
            lockItem as State.Loaded
            assertTrue(lockItem.appWidget.surface == UiSurface.LOCKSCREEN)
        }
    }

    @Test
    fun testOnPageSingleClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onPageControlsClicked()
            awaitItem()
            sut.onPageSingleClicked()
            val singleItem = awaitItem()
            assertTrue(singleItem is State.Loaded)
            singleItem as State.Loaded
            assertFalse(singleItem.appWidget.multiPage)
            assertFalse(singleItem.appWidget.showControls)
        }
    }

    @Test
    fun testOnPageControlsClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onPageControlsClicked()
            val controlsItem = awaitItem()
            assertTrue(controlsItem is State.Loaded)
            controlsItem as State.Loaded
            assertTrue(controlsItem.appWidget.multiPage)
            assertTrue(controlsItem.appWidget.showControls)
        }
    }

    @Test
    fun testOnPageNoControlsClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onPageNoControlsClicked()
            val noControlsItem = awaitItem()
            assertTrue(noControlsItem is State.Loaded)
            noControlsItem as State.Loaded
            assertTrue(noControlsItem.appWidget.multiPage)
            assertFalse(noControlsItem.appWidget.showControls)
        }
    }

    @Test
    fun testOnColourAutomaticClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onColourBlackClicked()
            awaitItem()
            sut.onColourAutomaticClicked()
            val automaticItem = awaitItem()
            assertTrue(automaticItem is State.Loaded)
            automaticItem as State.Loaded
            assertTrue(automaticItem.appWidget.tintColour == TintColour.AUTOMATIC)
        }
    }

    @Test
    fun testOnColourBlackClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onColourBlackClicked()
            val blackItem = awaitItem()
            assertTrue(blackItem is State.Loaded)
            blackItem as State.Loaded
            assertTrue(blackItem.appWidget.tintColour == TintColour.BLACK)
        }
    }

    @Test
    fun testOnColourWhiteClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onColourWhiteClicked()
            val whiteItem = awaitItem()
            assertTrue(whiteItem is State.Loaded)
            whiteItem as State.Loaded
            assertTrue(whiteItem.appWidget.tintColour == TintColour.WHITE)
        }
    }

    @Test
    fun testOnApplyClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithAppWidget(mockWidget)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.closeBus.test {
                sut.onApplyClicked()
                coVerify {
                    appWidgetRepositoryMock.addWidget(
                        mockWidget.appWidgetId,
                        mockWidget.ownerPackage,
                        mockWidget.surface,
                        TintColour.AUTOMATIC,
                        multiPage = false,
                        showControls = false
                    )
                }
                assertTrue(awaitItem() == Unit)
            }
        }
    }

}