package com.kieronquinn.app.smartspacer.ui.screens.update

import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.update.Release
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.update.UpdateViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomLong
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateViewModelTests: BaseTest<UpdateViewModel>() {

    companion object {
        private fun createMockRelease(): Release {
            return Release(
                randomString(),
                randomString(),
                randomString(),
                randomString(),
                "https://google.com",
                randomString()
            )
        }
    }

    private val downloadMock = MutableStateFlow<DownloadState?>(null)
    private val navigationMock = mock<ContainerNavigation>()
    private val mockRelease = createMockRelease()

    private val downloadRepositoryMock = mock<DownloadRepository> {
        every { download(any()) } returns downloadMock.filterNotNull()
    }

    override val sut by lazy {
        UpdateViewModelImpl(
            navigationMock,
            downloadRepositoryMock,
            contextMock
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithRelease(mockRelease)
            val item = awaitItem()
            assertTrue(item is State.Info)
        }
    }

    @Test
    fun testOnDownloadBrowserClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithRelease(mockRelease)
            val item = awaitItem()
            assertTrue(item is State.Info)
            sut.onDownloadBrowserClicked()
            coVerify {
                navigationMock.navigate(Uri.parse(mockRelease.gitHubUrl))
            }
        }
    }

    @Test
    fun testStartDownload() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithRelease(mockRelease)
            val item = awaitItem()
            assertTrue(item is State.Info)
            sut.startDownload()
            val state = DownloadState.Progress(randomLong(), 50, "50%")
            downloadMock.emit(state)
            val downloadItem = awaitItem()
            assertTrue(downloadItem is State.Downloading)
            downloadItem as State.Downloading
            assertTrue(downloadItem.downloadState == state)
        }
    }

    @Test
    fun testStartInstall() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithRelease(mockRelease)
            val item = awaitItem()
            assertTrue(item is State.Info)
            sut.startDownload()
            val state = DownloadState.DownloadComplete(
                randomLong(), Uri.parse("https://example.com")
            )
            downloadMock.emit(state)
            sut.startInstall()
            coVerify {
                navigationMock.navigate(any<Intent>())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

}