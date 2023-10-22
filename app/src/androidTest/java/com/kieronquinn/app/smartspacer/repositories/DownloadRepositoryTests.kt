package com.kieronquinn.app.smartspacer.repositories

import android.app.DownloadManager
import android.app.DownloadManager.STATUS_FAILED
import android.app.DownloadManager.STATUS_RUNNING
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadRequest
import com.kieronquinn.app.smartspacer.repositories.DownloadRepository.DownloadState
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomLong
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import kotlin.math.roundToInt

class DownloadRepositoryTests: BaseTest<DownloadRepository>() {

    private val downloadManager = mock<DownloadManager>()

    override val sut by lazy {
        DownloadRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every { externalCacheDir } returns actualContext.externalCacheDir
        every { getSystemService(Context.DOWNLOAD_SERVICE) } returns downloadManager
    }

    @Test
    @Ignore("Currently OOMs when verifying download enqueue")
    fun testDownload() = runTest {
        val id = randomLong()
        val downloadUri = Uri.parse("content://downloads/my_downloads")
        every { downloadManager.enqueue(any()) } returns id
        val request = DownloadRequest(
            "https://example.com/download.zip",
            randomString(),
            randomString(),
            randomString()
        )
        var downloadState = MockDownloadState(STATUS_RUNNING, 0, 0.01)
        every { downloadManager.query(any()) } answers { downloadState.toCursor() }
        sut.download(request).test {
            verify(exactly = 1) { downloadManager.enqueue(any()) }
            //Test downloading state from start
            downloadState.compareTo(awaitItem())
            //Test second download state
            downloadState = MockDownloadState(STATUS_RUNNING, 0, 0.5)
            contentResolver.notifyChange(downloadUri)
            downloadState.compareTo(awaitItem())
            //Test success
            downloadState = MockDownloadState(STATUS_SUCCESSFUL, 1, 1.0)
            contentResolver.notifyChange(downloadUri)
            downloadState.compareTo(awaitItem())
            //Test failed
            downloadState = MockDownloadState(STATUS_FAILED, 2, 1.0)
            contentResolver.notifyChange(downloadUri)
            downloadState.compareTo(awaitItem())
        }
    }

    @Test
    fun testDownloadCancel() = runTest {
        val id = randomLong()
        sut.cancelDownload(id)
        verify(exactly = 1) { downloadManager.remove(id) }
    }

    data class MockDownloadState(
        val status: Int,
        val reason: Int = 0,
        val progress: Double
    ) {

        private val mockDownloadSize = randomInt(0, 100)

        fun toCursor(): Cursor {
            return MatrixCursor(
                arrayOf(
                    DownloadManager.COLUMN_STATUS,
                    DownloadManager.COLUMN_REASON,
                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES,
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                )
            ).apply {
                addRow(
                    arrayOf(
                        status,
                        reason,
                        mockDownloadSize,
                        (mockDownloadSize * progress).roundToInt()
                    )
                )
            }
        }

        fun compareTo(downloadState: DownloadState): Boolean {
            val state = when(status) {
                STATUS_RUNNING -> downloadState as? DownloadState.Progress
                STATUS_FAILED -> downloadState as? DownloadState.Failed
                STATUS_SUCCESSFUL -> downloadState as? DownloadState.DownloadComplete
                else -> return false
            } ?: return false
            when(state) {
                is DownloadState.Progress -> {
                    if(state.percentage != (progress * 100).roundToInt()) return false
                }
                is DownloadState.Failed -> {
                    if(state.reason != reason) return false
                }
                else -> {
                    // No-op
                }
            }
            return true
        }

    }

}