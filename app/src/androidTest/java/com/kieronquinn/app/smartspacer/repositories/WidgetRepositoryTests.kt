package com.kieronquinn.app.smartspacer.repositories

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContentProviderClient
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.view.WindowManager
import android.widget.AdapterView
import androidx.core.os.bundleOf
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.model.database.Widget
import com.kieronquinn.app.smartspacer.model.database.Widget.Type
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_APP_WIDGET_PROVIDER_INFO
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.views.appwidget.HeadlessAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.appWidgetManager
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import com.kieronquinn.app.smartspacer.utils.remoteviews.RemoteViewsFactoryWrapper
import com.kieronquinn.app.smartspacer.widget.HeadlessAppWidgetHost
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("CloseWidget")
class WidgetRepositoryTests: BaseTest<WidgetRepository>() {

    companion object {
        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockAppWidgetProviders(): List<AppWidgetProviderInfo> {
            return listOf(
                createAppWidgetProviderInfo("com.example.one", "one"),
                createAppWidgetProviderInfo("com.example.two", "two"),
                createAppWidgetProviderInfo("com.example.three", "three")
            )
        }

        private fun getWidgets(): List<Widget> {
            return listOf(
                createWidget("com.example.one", "one", Type.TARGET),
                createWidget("com.example.two", "two", Type.COMPLICATION),
                createWidget("com.example.three", "three", Type.TARGET)
            )
        }

        private fun createAppWidgetProviderInfo(
            packageName: String,
            className: String
        ): AppWidgetProviderInfo {
            return AppWidgetProviderInfo().apply {
                this.provider = ComponentName(packageName, className)
            }
        }

        private fun createWidget(
            packageName: String,
            className: String,
            type: Type
        ): Widget {
            return Widget(
                randomString(),
                type,
                ComponentName(packageName, className).flattenToString(),
                randomInt(),
                packageName,
                randomString()
            )
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()
    private val windowManagerMock = mock<WindowManager>()
    private val appWidgetManagerMock = appWidgetManager(mock())
    private val mockProviders = getMockAppWidgetProviders()
    private val appWidgetHostMock = mock<HeadlessAppWidgetHost>()
    private val mockWidgets = getWidgets()

    private val contentProviderClient = mock<ContentProviderClient> {
        every { call("get_targets_config", any(), any()) } answers {
            SmartspacerTargetProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON
            ).toBundle()
        }
        every { call("get_actions_config", any(), any()) } answers {
            SmartspacerComplicationProvider.Config(
                randomString(),
                randomString(),
                DUMMY_ICON
            ).toBundle()
        }
        every { call("get_widget_info", any(), any()) } answers {
            bundleOf(EXTRA_APP_WIDGET_PROVIDER_INFO to mockProviders.first())
        }
    }

    override val sut by lazy {
        WidgetRepositoryImpl(contextMock, databaseRepositoryMock, scope).also {
            it.appWidgetHost = appWidgetHostMock
        }
    }

    override fun Context.context() {
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every { getSystemService(Context.WINDOW_SERVICE) } returns windowManagerMock
    }

    @Test
    fun testWidgets() = runTest {
        every { appWidgetManagerMock.installedProviders } returns mockProviders
        every { databaseRepositoryMock.getWidgets() } returns flowOf(mockWidgets)
        val mockView = mock<HeadlessAppWidgetHostView>()
        every { appWidgetHostMock.createView(any(), any(), any()) } returns mockView
        sut.widgets.test {
            val actual = awaitItem()!!
            mockWidgets.zip(actual).forEach {
                val m = it.first
                val a = it.second
                assertTrue(m.authority == a.authority)
                assertTrue(m.packageName == a.sourcePackage)
                assertTrue(m.id == a.id)
                assertTrue(m.appWidgetId == a.appWidgetId)
            }
        }
    }

    @Test
    fun testProviders() = runTest {
        every { appWidgetManagerMock.installedProviders } returns emptyList()
        sut.providers.test {
            assertTrue(awaitItem().isEmpty())
            every { appWidgetManagerMock.installedProviders } returns mockProviders
            sut.onProvidersChanged()
            assertTrue(awaitItem() == mockProviders)
        }
    }

    @Test
    fun testGetProviders() = runTest {
        every { appWidgetManagerMock.installedProviders } returns mockProviders
        assertTrue(sut.getProviders() == mockProviders)
    }

    @Test
    fun getWidgetInfo() = runTest {
        val providerInfo = sut.getWidgetInfo(randomString(), randomString())
        assertTrue(providerInfo == mockProviders.first())
    }

    @Test
    fun testAllocateAppWidgetId() = runTest {
        sut.allocateAppWidgetId()
        verify(exactly = 1) {
            appWidgetHostMock.allocateAppWidgetId()
        }
    }

    @Test
    fun testDeallocateAppWidgetId() = runTest {
        val id = randomInt()
        sut.deallocateAppWidgetId(id)
        verify(exactly = 1) {
            appWidgetHostMock.deleteAppWidgetId(id)
        }
    }

    @Test
    fun testClickAppWidgetIdView() = runTest {
        val id = randomInt()
        val viewId = randomString()
        val view = mock<HeadlessAppWidgetHostView> {
            every { appWidgetId } returns id
        }
        sut.appWidgetHostPool.add(view)
        sut.clickAppWidgetIdView(id, viewId, null)
        verify(exactly = 1) {
            view.clickView(viewId)
        }
    }

    @Test
    fun testLoadAdapter() = runTest {
        val id = randomInt()
        val smartspacerId = randomString()
        val viewId = randomString()
        val mockAdapter = mock<RemoteViewsFactoryWrapper>()
        val mockAdapterView = mock<AdapterView<*>> {
            every { getClickPendingIntent() } returns null
        }
        val view = mock<HeadlessAppWidgetHostView> {
            every { appWidgetId } returns id
            every { findRemoteViewsAdapter(any(), any()) } returns mockAdapter
            every { findAdapterView(any(), any()) } returns mockAdapterView
        }
        sut.appWidgetHostPool.add(view)
        sut.loadAdapter(smartspacerId, id, viewId, null)
        coVerify(exactly = 1) {
            mockAdapter.awaitConnected()
        }
    }

}