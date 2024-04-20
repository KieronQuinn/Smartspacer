package com.kieronquinn.app.smartspacer.repositories

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.view.WindowManager
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.model.database.ExpandedAppWidget
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.views.appwidget.ExpandedAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import com.kieronquinn.app.smartspacer.widget.AppWidgetHost
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpandedRepositoryTests: BaseTest<ExpandedRepository>() {

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val widgetRepositoryMock = mock<WidgetRepository>()
    private val windowManagerMock = mock<WindowManager>()
    private val appWidgetManagerMock = mock<AppWidgetManager>()
    private val appWidgetHostMock = mock<AppWidgetHost>()
    private val expandedWidgetUseGoogleSansMock = mockSmartspacerSetting(false)
    private val mockListener = mock<SmartspaceTargetInteractionListener>()

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { expandedWidgetUseGoogleSans } returns expandedWidgetUseGoogleSansMock
    }

    private val shizukuServiceMock = mockShizukuRepository {  }

    override val sut by lazy {
        ExpandedRepositoryImpl(
            contextMock,
            settingsRepositoryMock,
            databaseRepositoryMock,
            widgetRepositoryMock,
            shizukuServiceMock,
            scope
        ).also {
            it.appWidgetHost = appWidgetHostMock
        }
    }

    override fun Context.context() {
        every { getSystemService(Context.WINDOW_SERVICE) } returns windowManagerMock
        every { getSystemService(Context.APPWIDGET_SERVICE) } returns appWidgetManagerMock
        every { mainLooper } returns mock()
    }

    @Test
    fun testCommitExpandedAppWidget() = runTest {
        val mockComponent = ComponentName(randomString(), randomString())
        val provider = AppWidgetProviderInfo().apply {
            provider = mockComponent
        }
        val appWidgetId = randomInt()
        val id = randomString()
        sut.commitExpandedAppWidget(provider, appWidgetId, id, null)
        coVerify(exactly = 1) {
            databaseRepositoryMock.addExpandedAppWidget(
                ExpandedAppWidget(appWidgetId, provider.provider.flattenToString(), id)
            )
        }
    }

    @Test
    fun testCommitExpandedCustomAppWidget() = runTest {
        val mockComponent = ComponentName("package_name", "class_name")
        val provider = AppWidgetProviderInfo().apply {
            provider = mockComponent
        }
        val appWidgetId = randomInt()
        val id = randomString()
        val customConfig = CustomExpandedAppWidgetConfig(
            randomInt(), randomInt(), randomInt(), randomBoolean(), randomBoolean(), randomBoolean()
        )
        sut.commitExpandedAppWidget(provider, appWidgetId, id, customConfig)
        coVerify(exactly = 1) {
            databaseRepositoryMock.addExpandedCustomAppWidget(
                ExpandedCustomAppWidget(
                    id,
                    appWidgetId,
                    provider.provider.flattenToString(),
                    customConfig.index,
                    customConfig.spanX,
                    customConfig.spanY,
                    customConfig.showWhenLocked,
                    customConfig.roundCorners,
                    customConfig.fullWidth
                )
            )
        }
    }

    @Test
    fun testAllocateAppWidgetId() = runTest {
        val id = randomInt()
        every { appWidgetHostMock.allocateAppWidgetId() } returns id
        assertTrue(sut.allocateAppWidgetId() == id)
    }

    @Test
    fun testDeallocateAppWidgetId() = runTest {
        val id = randomInt()
        sut.deallocateAppWidgetId(id)
        verify(exactly = 1) { appWidgetHostMock.deleteAppWidgetId(id) }
    }

    @Test
    fun testRemoveAppWidget() = runTest {
        val id = randomInt()
        sut.removeAppWidget(id)
        verify(exactly = 1) { appWidgetHostMock.deleteAppWidgetId(id) }
        coVerify(exactly = 1) { databaseRepositoryMock.deleteExpandedAppWidget(id) }
    }

    @Test
    fun testCreateHost() = runTest {
        every {
            resourcesMock.getDimensionPixelSize(R.dimen.expanded_smartspace_remoteviews_max_height)
        } returns 1
        val mockComponent = ComponentName(randomString(), randomString())
        val provider = AppWidgetProviderInfo().apply {
            provider = mockComponent
        }
        val mockView = mock<ExpandedAppWidgetHostView>()
        every { appWidgetHostMock.createView(any(), any(), any(), any(), any()) } returns mockView
        val mockWidget = Item.Widget(
            provider,
            randomInt(),
            randomString(),
            3,
            5,
            isCustom = false,
            useGoogleSans = false,
            config = null,
            isDark = true,
            fullWidth = randomBoolean(),
            roundCorners = randomBoolean(),
            spanX = 5,
            spanY = 2
        )
        val sessionId = randomString()
        sut.createHost(contextMock, 1280, mockWidget, sessionId, mockListener)
        verify {
            appWidgetHostMock.createView(
                any(), mockWidget.appWidgetId!!, sessionId, mockWidget.provider, mockListener
            )
        }
        verify {
            mockView.updateSizeIfNeeded(any(), any())
        }
    }

    @Test
    fun testCreateCustomHost() = runTest {
        val mockComponent = ComponentName(randomString(), randomString())
        val provider = AppWidgetProviderInfo().apply {
            provider = mockComponent
        }
        val mockView = mock<ExpandedAppWidgetHostView>()
        every { appWidgetHostMock.createView(any(), any(), any(), any(), any()) } returns mockView
        val mockWidget = Item.Widget(
            provider,
            randomInt(),
            randomString(),
            3,
            5,
            isCustom = true,
            useGoogleSans = false,
            config = null,
            isDark = true,
            fullWidth = randomBoolean(),
            roundCorners = randomBoolean(),
            spanX = 5,
            spanY = 2
        )
        val sessionId = randomString()
        sut.createHost(contextMock, 1280, mockWidget, sessionId, mockListener)
        verify {
            appWidgetHostMock.createView(
                any(), mockWidget.appWidgetId!!, sessionId, mockWidget.provider, mockListener
            )
        }
        verify {
            mockView.updateSizeIfNeeded(any(), any())
        }
    }

    @Test
    fun testWidgetUseGoogleSans() = runTest {
        assertFalse(sut.widgetUseGoogleSans)
        expandedWidgetUseGoogleSansMock.set(true)
        assertTrue(sut.widgetUseGoogleSans)
    }

}