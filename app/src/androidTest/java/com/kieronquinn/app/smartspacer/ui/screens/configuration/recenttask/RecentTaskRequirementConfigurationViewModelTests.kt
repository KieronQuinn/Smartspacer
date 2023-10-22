package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.RecentTaskRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecentTaskRequirementConfigurationViewModelTests: BaseTest<RecentTaskRequirementConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_RECENT_TASK = "${BuildConfig.APPLICATION_ID}.requirement.recenttask"

        private fun getMockRequirementData(): RequirementData {
            return RequirementData()
        }
    }

    private val requirementDataMock = MutableStateFlow(getMockRequirementData())
    private val mockId = randomString()
    private val navigationMock = mock<ConfigurationNavigation>()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getRequirementDataFlow(any(), RequirementData::class.java) } returns requirementDataMock
        every {
            updateRequirementData(
                any(),
                RequirementData::class.java,
                RequirementDataType.RECENT_TASK,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(requirementDataMock.value)
            requirementDataMock.emit(newData as RequirementData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override val sut by lazy {
        RecentTaskRequirementConfigurationViewModelImpl(
            navigationMock,
            contextMock,
            dataRepositoryMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_RECENT_TASK
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
    fun testOnSelectedAppClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loaded)
            sut.onSelectedAppClicked()
            coVerify {
                navigationMock.navigate(
                    RecentTaskRequirementConfigurationFragmentDirections.actionRecentTaskRequirementConfigurationFragmentToRecentTaskRequirementConfigurationAppPickerFragment(mockId)
                )
            }
        }
    }

    @Test
    fun testOnLimitClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.Loaded)
            sut.onLimitClicked()
            coVerify {
                navigationMock.navigate(
                    RecentTaskRequirementConfigurationFragmentDirections.actionRecentTaskRequirementConfigurationFragmentToRecentTaskRequirementConfigurationLimitBottomSheetFragment(mockId)
                )
            }
        }
    }

}