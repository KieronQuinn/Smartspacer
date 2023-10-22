package com.kieronquinn.app.smartspacer.repositories

import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.ITaskObserver
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.every
import io.mockk.slot
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecentTasksRepositoryTests: BaseTest<RecentTasksRepository>() {

    companion object {
        private const val AUTHORITY_RECENT_TASK =
            "${BuildConfig.APPLICATION_ID}.requirement.recenttask"
    }

    private var mockTaskObserver: ITaskObserver? = null

    private val shizukuServiceRepositoryMock = mockShizukuRepository {
        val callbackCapture = slot<ITaskObserver>()
        every { setTaskObserver(capture(callbackCapture)) } answers {
            mockTaskObserver = callbackCapture.captured
        }
    }

    override val sut by lazy {
        RecentTasksRepositoryImpl(contextMock, shizukuServiceRepositoryMock, scope)
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
    fun testRecentTaskPackagesNoShizuku() = runTest {
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(false)
        sut.recentTaskPackages.test {
            assertTrue(awaitItem().isEmpty())
            expectNoEvents()
        }
    }

    @Test
    fun testRecentTaskPackages() = runTest {
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(true)
        sut.recentTaskPackages.test {
            assertTrue(awaitItem().isEmpty())
            val mock = listOf("com.example.one", "com.example.two")
            mockTaskObserver?.onTasksChanged(mock) ?: return@test
            assertTrue(awaitItem() == mock)
            mockTaskObserver!!.onTasksChanged(emptyList())
            assertTrue(awaitItem().isEmpty())
        }
    }

}