package com.kieronquinn.app.smartspacer.ui.screens.repository

import android.content.Intent
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.repository.PluginRepositoryViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PluginRepositoryViewModelTests: BaseTest<PluginRepositoryViewModel>() {

    companion object {
        private fun getMockPlugin(): Plugin {
            return Plugin.Remote(
                randomBoolean(),
                randomString(),
                randomString(),
                randomString(),
                randomString(),
                randomString(),
                emptyList(),
                randomBoolean(),
                emptyList()
            )
        }
    }

    private val mockPlugins = listOf(getMockPlugin(), getMockPlugin(), getMockPlugin())
    private val containerNavigationMock = mock<ContainerNavigation>()

    private val pluginRepositoryMock = mock<PluginRepository> {
        every { getPlugins() } returns flowOf(mockPlugins)
    }

    override val sut by lazy {
        PluginRepositoryViewModelImpl(
            containerNavigationMock,
            pluginRepositoryMock,
            scope
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.isAvailableTab)
            assertTrue(item.items.size == mockPlugins.size)
            val pluginToSearchFor = mockPlugins.first()
            sut.setSearchTerm(pluginToSearchFor.name.toString())
            val searchItem = awaitItem()
            assertTrue(searchItem is State.Loaded)
            searchItem as State.Loaded
            assertTrue(searchItem.items == listOf(pluginToSearchFor))
        }
    }

    @Test
    fun testOnPluginClicked() = runTest {
        val localPlugin = Plugin.Local(randomString(), randomString(), Intent())
        sut.onPluginClicked(localPlugin)
        coVerify {
            containerNavigationMock.navigate(any<Intent>())
        }
        val remotePlugin = getMockPlugin()
        sut.onPluginClicked(remotePlugin)
        coVerify {
            containerNavigationMock.navigate(
                PluginRepositoryFragmentDirections.actionPluginRepositoryFragmentToPluginDetailsFragment(remotePlugin)
            )
        }
    }

    @Test
    fun testReload() = runTest {
        sut.reload(true)
        verify {
            pluginRepositoryMock.reload(true)
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
            assertFalse(awaitItem())
            sut.setSearchTerm(randomString())
            assertTrue(awaitItem())
        }
    }

}