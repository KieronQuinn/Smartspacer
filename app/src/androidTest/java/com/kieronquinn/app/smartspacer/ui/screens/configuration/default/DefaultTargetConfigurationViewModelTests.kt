package com.kieronquinn.app.smartspacer.ui.screens.configuration.default

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.default.DefaultTargetConfigurationViewModel.HiddenTarget
import com.kieronquinn.app.smartspacer.ui.screens.configuration.default.DefaultTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.ui.screens.configuration.default.DefaultTargetConfigurationViewModel.TargetType
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultTargetConfigurationViewModelTests: BaseTest<DefaultTargetConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_DEFAULT = "${BuildConfig.APPLICATION_ID}.target.default"

        private fun getMockTargetData(): TargetData {
            return TargetData()
        }
    }

    private val navigationMock = mock<ConfigurationNavigation>()

    private val targetDataMock = MutableStateFlow(getMockTargetData())
    private val mockId = randomString()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getTargetDataFlow(any(), TargetData::class.java) } returns targetDataMock
        every {
            updateTargetData(
                any(),
                TargetData::class.java,
                TargetDataType.DEFAULT,
                any(),
                any()
            )
        } coAnswers  {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(targetDataMock.value)
            targetDataMock.emit(newData as TargetData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_DEFAULT
            }
        }
    }

    override val sut by lazy {
        DefaultTargetConfigurationViewModelImpl(
            navigationMock,
            dataRepositoryMock,
            contextMock,
            scope
        )
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
    fun testOnAtAGlanceClicked() = runTest {
        sut.onAtAGlanceClicked()
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnHiddenTargetChanged() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val target = HiddenTarget(TargetType.FLASHLIGHT, true)
            sut.onHiddenTargetChanged(target, true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.settings.contains(target))
            //Reset back to default
            sut.onHiddenTargetChanged(target, true)
        }
    }

}