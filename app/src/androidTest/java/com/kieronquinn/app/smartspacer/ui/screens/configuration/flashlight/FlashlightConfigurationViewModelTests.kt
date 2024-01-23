package com.kieronquinn.app.smartspacer.ui.screens.configuration.flashlight

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.FlashlightTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.flashlight.FlashlightTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FlashlightConfigurationViewModelTests: BaseTest<FlashlightTargetConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_MUSIC = "${BuildConfig.APPLICATION_ID}.target.music"

        private fun getMockTargetData(): TargetData {
            return TargetData()
        }
    }

    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val mockId = randomString()
    private var hasLightSensor = true

    private val sensorManagerMock = mock<SensorManager> {
        every { getDefaultSensor(Sensor.TYPE_LIGHT) } answers {
            if(hasLightSensor) mock() else null
        }
    }

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.FLASHLIGHT,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(targetDataMock.value)
            targetDataMock.emit(newData as TargetData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override val sut by lazy {
        FlashlightTargetConfigurationViewModelImpl(
            dataRepositoryMock,
            contextMock,
            scope
        )
    }

    override fun Context.context() {
        every { getSystemService(Context.SENSOR_SERVICE) } returns sensorManagerMock
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_MUSIC
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
        }
    }

    @Test
    fun testOnRecommendedChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onRecommendedChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.data.recommend)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testCompatible() = runTest {
        hasLightSensor = true
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.compatible)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testNotCompatible() = runTest {
        hasLightSensor = false
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertFalse(item.compatible)
            cancelAndIgnoreRemainingEvents()
        }
    }

}