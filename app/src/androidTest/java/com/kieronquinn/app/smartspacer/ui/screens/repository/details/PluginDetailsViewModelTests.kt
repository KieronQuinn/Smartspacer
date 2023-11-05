package com.kieronquinn.app.smartspacer.ui.screens.repository.details

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PluginApi.RemotePluginInfo
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel.PluginViewState
import com.kieronquinn.app.smartspacer.ui.screens.repository.details.PluginDetailsViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomLong
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PluginDetailsViewModelTests: BaseTest<PluginDetailsViewModel>() {

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

        private fun getMockPluginInfo(): RemotePluginInfo {
            return RemotePluginInfo.UpdateJson(
                randomString(),
                randomString(),
                randomString(),
                emptyList(),
                randomString(),
                randomString(),
                randomString(),
                randomInt(),
                emptyList(),
                randomLong(),
                0L
            )
        }
    }

    private val mockPlugin = getMockPlugin()
    private val mockPluginInfo = getMockPluginInfo()

    private val containerNavigationMock = mock<ContainerNavigation>()

    private val downloadRepositoryMock = mock<DownloadRepository> {
        every { download(any()) } returns flowOf(DownloadRepository.DownloadState.Progress(
            randomLong(), 1, "1%"
        ))
    }

    private val packageRepositoryMock = mock<PackageRepository> {
        every { onPackageChanged(any<String>()) } returns flowOf(0L)
    }

    private val pluginRepositoryMock = mock<PluginRepository> {
        coEvery { getPluginInfo(any()) } returns mockPluginInfo
    }

    override val sut by lazy {
        PluginDetailsViewModelImpl(
            containerNavigationMock,
            packageRepositoryMock,
            downloadRepositoryMock,
            contextMock,
            pluginRepositoryMock
        )
    }

    override fun Context.context() {
        every {
            packageManagerMock.getPackageInfo(any<String>(), any<PackageInfoFlags>())
        } returns PackageInfo()
        every {
            packageManagerMock.getLaunchIntentForPackage(any())
        } returns Intent()
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithPlugin(mockPlugin as Plugin.Remote)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.plugin == mockPlugin)
        }
    }

    @Test
    fun testOnInstallClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithPlugin(mockPlugin as Plugin.Remote)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            sut.onInstallClicked()
            val downloadItem = awaitItem()
            assertTrue(downloadItem is State.Loaded)
            downloadItem as State.Loaded
            assertTrue(downloadItem.viewState is PluginViewState.Downloading)
        }
    }

    @Test
    fun testOnScreenshotClicked() = runTest {
        val url = randomString()
        sut.onScreenshotClicked(url)
        coVerify {
            containerNavigationMock.navigate(
                PluginDetailsFragmentDirections.actionPluginDetailsFragmentToPluginDetailsScreenshotFragment(url)
            )
        }
    }

}