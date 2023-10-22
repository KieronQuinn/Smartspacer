package com.kieronquinn.app.smartspacer.repositories

import android.content.ComponentName
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.os.Bundle
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DigitalWellbeingComplication
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DigitalWellbeingTarget
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository.WellbeingState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_CLICK_VIEW
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DigitalWellbeingRepositoryTests: BaseTest<DigitalWellbeingRepository>() {

    companion object {
        private const val AUTHORITY_WELLBEING_TARGET =
            "${BuildConfig.APPLICATION_ID}.target.digitalwellbeing"
        private const val AUTHORITY_WELLBEING_COMPLICATION =
            "${BuildConfig.APPLICATION_ID}.complication.digitalwellbeing"
        private const val AUTHORITY_WIDGET =
            "${BuildConfig.APPLICATION_ID}.appwidgetprovider"
    }

    private val contentProviderClient = mock<ContentProviderClient>()

    override val sut by lazy {
        DigitalWellbeingRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every {
            contentResolver.acquireUnstableContentProviderClient(AUTHORITY_WIDGET)
        } answers {
            contentProviderClient
        }
    }

    override fun setup() {
        super.setup()
        val componentWellbeingTarget = ComponentName(
            actualContext, DigitalWellbeingTarget::class.java
        )
        val componentWellbeingComplication = ComponentName(
            actualContext, DigitalWellbeingComplication::class.java
        )
        every {
            packageManagerMock.getProviderInfo(componentWellbeingTarget, any<ComponentInfoFlags>())
        } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_WELLBEING_TARGET
            }
        }
        every {
            packageManagerMock.getProviderInfo(
                componentWellbeingComplication, any<ComponentInfoFlags>()
            )
        } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_WELLBEING_COMPLICATION
            }
        }
    }

    @Test
    fun testState() = runTest {
        assertTrue(sut.getState() == null)
        val mockState = WellbeingState(
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            mock()
        )
        sut.setState(mockState)
        assertTrue(sut.getState() == mockState)
    }

    @Test
    fun testAddSmartspacerId() = runTest {
        sut
        every {
            contentResolverMock.call(any<String>(), any(), any(), any())
        } returns Bundle()
        val id = randomString()
        assertTrue(sut.ids.isEmpty())
        sut.addSmartspacerIdIfNeeded(id)
        assertTrue(sut.ids.contains(id))
        verify(exactly = 1) {
            contentProviderClient.call(METHOD_CLICK_VIEW, null, any())
        }
        //Adding a second time should not add another to the list and should not re-call remote
        sut.addSmartspacerIdIfNeeded(id)
        assertTrue(sut.ids.contains(id))
        assertTrue(sut.ids.size == 1)
        verify(exactly = 1) {
            contentProviderClient.call(METHOD_CLICK_VIEW, null, any())
        }
    }

    @Test
    fun testRemoveSmartspacerId() = runTest {
        val id = randomString()
        sut.addSmartspacerIdIfNeeded(id)
        assertTrue(sut.ids.contains(id))
        sut.removeSmartspacerId(id)
        assertFalse(sut.ids.contains(id))
    }

    @Test
    fun testRefreshIfNeeded() = runTest {
        sut
        every {
            contentResolverMock.call(any<String>(), any(), any(), any())
        } returns Bundle()
        val id = randomString()
        sut.addSmartspacerIdIfNeeded(id)
        sut.refreshWidgetIfNeeded(id)
        verify {
            contentProviderClient.call(METHOD_CLICK_VIEW, null, any())
        }
    }

}