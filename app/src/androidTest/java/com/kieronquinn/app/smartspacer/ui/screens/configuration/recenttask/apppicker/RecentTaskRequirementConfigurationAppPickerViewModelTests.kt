package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.apppicker

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.apppicker.RecentTaskRequirementConfigurationAppPickerViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecentTaskRequirementConfigurationAppPickerViewModelTests: BaseTest<RecentTaskRequirementConfigurationAppPickerViewModel>() {

    companion object {
        private const val AUTHORITY_RECENT_TASK = "${BuildConfig.APPLICATION_ID}.requirement.recenttask"

        private fun getMockRequirementData(): RecentTaskRequirement.RequirementData {
            return RecentTaskRequirement.RequirementData()
        }

        private fun getMockApps(): List<ListAppsApp> {
            return listOf(
                ListAppsApp(randomString(), randomString()),
                ListAppsApp(randomString(), randomString()),
                ListAppsApp(randomString(), randomString())
            )
        }
    }

    private val requirementDataMock = MutableStateFlow(getMockRequirementData())
    private val mockId = randomString()
    private val navigationMock = mock<ConfigurationNavigation>()
    private val appsMock = getMockApps()

    private val dataRepositoryMock = mock<DataRepository> {
        every { getRequirementDataFlow(any(), RecentTaskRequirement.RequirementData::class.java) } returns requirementDataMock
        every {
            updateRequirementData(
                any(),
                RecentTaskRequirement.RequirementData::class.java,
                RequirementDataType.RECENT_TASK,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(requirementDataMock.value)
            requirementDataMock.emit(newData as RecentTaskRequirement.RequirementData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    private val packageRepositoryMock = mock<PackageRepository> {
        coEvery { getInstalledApps(false) } returns appsMock
    }

    override val sut by lazy {
        RecentTaskRequirementConfigurationAppPickerViewModelImpl(
            dataRepositoryMock,
            navigationMock,
            packageRepositoryMock,
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
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.setSearchTerm(appsMock.first().packageName)
            val packageItem = awaitItem()
            assertTrue(packageItem is State.Loaded)
            packageItem as State.Loaded
            assertTrue(packageItem.apps == listOf(appsMock.first()))
            sut.setSearchTerm(appsMock[1].label.toString())
            val labelItem = awaitItem()
            assertTrue(labelItem is State.Loaded)
            labelItem as State.Loaded
            assertTrue(labelItem.apps == listOf(appsMock[1]))
            sut.setSearchTerm("")
            cancelAndIgnoreRemainingEvents()
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
            sut.setSearchTerm("")
            cancelAndIgnoreRemainingEvents()
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

    @Test
    fun testOnAppClicked() = runTest {
        val id = randomString()
        val appToClick = appsMock.first()
        sut.onAppClicked(id, appToClick)
        assertTrue(requirementDataMock.value.appPackageName == appToClick.packageName)
        coVerify {
            navigationMock.navigateBack()
        }
    }

}