package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.limit

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
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecentTaskRequirementConfigurationLimitBottomSheetViewModelTests: BaseTest<RecentTaskRequirementConfigurationLimitBottomSheetViewModel>() {

    companion object {
        private const val AUTHORITY_RECENT_TASK = "${BuildConfig.APPLICATION_ID}.requirement.recenttask"

        private fun getMockRequirementData(): RequirementData {
            return RequirementData()
        }
    }

    private val dataRepositoryMock = mock<DataRepository> {
        every { getRequirementDataFlow(any(), RequirementData::class.java) } answers {
            requirementDataMock
        }
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

    private val requirementDataMock = MutableStateFlow(getMockRequirementData())
    private val mockId = randomString()
    private val navigationMock = mock<ConfigurationNavigation>()

    override val sut by lazy {
        RecentTaskRequirementConfigurationLimitBottomSheetViewModelImpl(
            navigationMock,
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
    fun testLimit() = runTest {
        sut.limit.test {
            assertTrue(awaitItem() == null)
            sut.setupWithId(mockId)
            sut.onLimitChanged(0)
            assertTrue(awaitItem() != null)
        }
    }

    @Test
    fun testOnLimitChanged() = runTest {
        sut.limit.test {
            assertTrue(awaitItem() == null)
            sut.setupWithId(mockId)
            sut.onLimitChanged(0)
            assertTrue(awaitItem() != null)
            val limit = randomInt(from = 1)
            sut.onLimitChanged(limit)
            assertTrue(awaitItem() == limit)
        }
    }

    @Test
    fun testOnSaveClicked() = runTest {
        sut.limit.test {
            assertTrue(awaitItem() == null)
            sut.setupWithId(mockId)
            assertTrue(awaitItem() != null)
            val limit = randomInt(from = 1)
            sut.onLimitChanged(limit)
            assertTrue(awaitItem() == limit)
            sut.onSaveClicked()
            assertTrue(requirementDataMock.value.limit == limit)
            coVerify {
                navigationMock.navigateBack()
            }
        }
    }

    @Test
    fun testOnCancelClicked() = runTest {
        sut.onCancelClicked()
        coVerify {
            navigationMock.navigateBack()
        }
    }

}