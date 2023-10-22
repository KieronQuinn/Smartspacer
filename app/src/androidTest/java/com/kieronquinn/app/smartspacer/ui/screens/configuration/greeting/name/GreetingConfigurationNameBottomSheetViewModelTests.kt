package com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GreetingTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name.GreetingConfigurationNameBottomSheetViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GreetingConfigurationNameBottomSheetViewModelTests: BaseTest<GreetingConfigurationNameBottomSheetViewModel>() {

    companion object {
        private const val AUTHORITY_GREETING = "${BuildConfig.APPLICATION_ID}.target.greeting"

        private fun getMockTargetData(): TargetData {
            return TargetData(
                hideIfNoComplications = false,
                hideTitleOnAod = false
            )
        }
    }

    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val navigationMock = mock<ConfigurationNavigation>()
    private val mockId = randomString()
    private val settingsRepository = mock<SmartspacerSettingsRepository>()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.GREETING,
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
        GreetingConfigurationNameBottomSheetViewModelImpl(
            dataRepositoryMock,
            navigationMock,
            settingsRepository,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_GREETING
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
        }
    }

    @Test
    fun testSetName() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            val name = randomString()
            sut.setName(name)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.name == name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnPositiveClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            val name = randomString()
            sut.setName(name)
            sut.onPositiveClicked()
            assertTrue(targetDataMock.value.name == name)
            coVerify {
                navigationMock.navigateBack()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnNegativeClicked() = runTest {
        sut.onNegativeClicked()
        coVerify {
            navigationMock.navigateBack()
        }
    }


}