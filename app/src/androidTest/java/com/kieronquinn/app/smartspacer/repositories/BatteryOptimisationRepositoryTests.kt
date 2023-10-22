package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.os.PowerManager
import com.judemanutd.autostarter.AutoStartPermissionHelper
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.extensions.getIgnoreBatteryOptimisationsIntent
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BatteryOptimisationRepositoryTests: BaseTest<BatteryOptimisationRepository>() {

    private val powerManagerMock = mock<PowerManager>()
    private val autoStartPermissionHelperMock = mock<AutoStartPermissionHelper>()

    override val sut by lazy {
        BatteryOptimisationRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every { getSystemService(Context.POWER_SERVICE) } returns powerManagerMock
    }

    override fun setup() {
        super.setup()
        mockkStatic(AutoStartPermissionHelper::class)
        every { AutoStartPermissionHelper.getInstance() } returns autoStartPermissionHelperMock
    }

    @Test
    fun testBatteryOptimisationIntentDisabled() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns false
        val actualIntent = sut.getDisableBatteryOptimisationsIntent()
        val requiredIntent = getIgnoreBatteryOptimisationsIntent()
        assertTrue(actualIntent?.action == requiredIntent.action)
        assertTrue(actualIntent?.data == requiredIntent.data)
    }

    @Test
    fun testBatteryOptimisationIntentEnabled() = runTest {
        every { powerManagerMock.isIgnoringBatteryOptimizations(any()) } returns true
        val intent = sut.getDisableBatteryOptimisationsIntent()
        assertTrue(intent == null)
    }

    @Test
    fun testAreOemOptimisationsAvailableTrue() = runTest {
        every {
            autoStartPermissionHelperMock.isAutoStartPermissionAvailable(any(), any())
        } returns true
        assertTrue(sut.areOemOptimisationsAvailable(contextMock))
    }

    @Test
    fun testAreOemOptimisationsAvailableFalse() = runTest {
        every {
            autoStartPermissionHelperMock.isAutoStartPermissionAvailable(any(), any())
        } returns false
        assertFalse(sut.areOemOptimisationsAvailable(contextMock))
    }

    @Test
    fun testStartOemOptimisationSettings() = runTest {
        sut.startOemOptimisationSettings(contextMock)
        verify(exactly = 1) {
            autoStartPermissionHelperMock.getAutoStartPermission(any(), open = true, newTask = true)
        }
    }

}