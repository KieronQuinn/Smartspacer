package com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting

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
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.GreetingConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GreetingConfigurationViewModelTests: BaseTest<GreetingConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_GREETING = "${BuildConfig.APPLICATION_ID}.target.greeting"

        private fun getMockTargetData(): TargetData {
            return TargetData(
                hideIfNoComplications = false,
                hideTitleOnAod = false,
                openExpandedOnClick = false
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
        GreetingConfigurationViewModelImpl(
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
            item as State.Loaded
        }
    }

    @Test
    fun testSetHideIfNoComplications() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.setHideIfNoComplications(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.hideIfNoComplications)
            sut.setHideIfNoComplications(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testSetHideTitleOnAod() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.setHideTitleOnAod(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.hideTitleOnAod)
            sut.setHideTitleOnAod(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testSetOpenExpandedOnAod() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.setOpenExpandedOnClick(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.openExpandedOnClick)
            sut.setOpenExpandedOnClick(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnNameClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onNameClicked()
            coVerify {
                navigationMock.navigate(
                    GreetingConfigurationFragmentDirections
                        .actionGreetingConfigurationFragmentToGreetingConfigurationNameBottomSheetFragment(mockId)
                )
            }
        }
    }

}