package com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement.AppPredictionRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction.AppPredictionRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.verify
import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppPredictionRequirementConfigurationViewModelTests: BaseTest<AppPredictionRequirementConfigurationViewModel>() {

    companion object {
        private fun getMockInstalledApps(): List<ListAppsApp> {
            return listOf(
                ListAppsApp(randomString(), randomString()),
                ListAppsApp(randomString(), randomString()),
                ListAppsApp(randomString(), randomString())
            )
        }
    }

    private val dataRepositoryMock = mock<DataRepository>()
    private val mockApps = getMockInstalledApps()

    private val packageRepositoryMock = mock<PackageRepository> {
        coEvery { getInstalledApps() } returns mockApps
    }

    override val sut by lazy {
        AppPredictionRequirementConfigurationViewModelImpl(
            dataRepositoryMock,
            packageRepositoryMock,
            scope
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.apps == mockApps)
        }
    }

    @Test
    fun testOnAppClicked() = runTest {
        val id = randomString()
        val app = mockApps.first()
        sut.onAppClicked(id, app)
        verify {
            dataRepositoryMock.updateRequirementData(
                id,
                AppPredictionRequirementData::class.java,
                RequirementDataType.APP_PREDICTION,
                any(),
                any()
            )
        }
    }

    @Test
    fun testSearchTerm() = runTest {
        sut.searchTerm.test {
            assertTrue(awaitItem().isEmpty())
            val random = randomString()
            sut.setSearchTerm(random)
            assertTrue(awaitItem() == random)
            assertTrue(sut.getSearchTerm() == random)
        }
    }

    @Test
    fun testShowSearchClear() = runTest {
        sut.showSearchClear.test {
            sut.setSearchTerm("")
            TestCase.assertFalse(awaitItem())
            sut.setSearchTerm(randomString())
            assertTrue(awaitItem())
        }
    }

}