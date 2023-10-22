package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.searchprovider

import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpandedSettingsSearchProviderViewModelTests: BaseTest<ExpandedSettingsSearchProviderViewModel>() {

    companion object {
        private fun getMockSearchApp(): SearchApp {
            return SearchApp(
                randomString(),
                randomString(),
                null,
                randomBoolean(),
                randomBoolean(),
                Intent()
            )
        }
    }

    private val mockSearchApps = listOf(getMockSearchApp(), getMockSearchApp(), getMockSearchApp())

    private val searchRepositoryMock = mock<SearchRepository> {
        every { getAllSearchApps() } returns mockSearchApps
        every { expandedSearchApp } returns flowOf(mockSearchApps.first())
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()

    override val sut by lazy {
        ExpandedSettingsSearchProviderViewModelImpl(
            searchRepositoryMock,
            settingsRepositoryMock,
            scope
        )
    }

    @Test
    fun testOnSearchAppClicked() = runTest {
        sut.onSearchAppClicked(mockSearchApps.first())
        coVerify {
            settingsRepositoryMock.expandedSearchPackage.set(mockSearchApps.first().packageName)
        }
    }

}