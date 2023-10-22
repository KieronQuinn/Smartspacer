package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.net.Uri
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener as DatabaseBroadcastListener

class BroadcastRepositoryTests: BaseTest<BroadcastRepository>() {

    companion object {
        private fun getMockBroadcastListeners() = listOf(
            DatabaseBroadcastListener(randomString(), randomString(), randomString()),
            DatabaseBroadcastListener(randomString(), randomString(), randomString()),
            DatabaseBroadcastListener(randomString(), randomString(), randomString())
        )
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()

    override val sut by lazy {
        BroadcastRepositoryImpl(contextMock, databaseRepositoryMock, scope)
    }

    override fun Context.context() {
        every { contentResolver.acquireUnstableContentProviderClient(any<Uri>()) } returns mock()
    }

    @Test
    fun testListeners() = runTest {
        val firstMockListeners = getMockBroadcastListeners()
        val broadcastListeners = MutableStateFlow(firstMockListeners)
        every { databaseRepositoryMock.getBroadcastListeners() } returns broadcastListeners
        sut.currentListeners.zip(firstMockListeners).forEach {
            val listener = it.first
            val databaseListener = it.second
            assertTrue(listener.id == databaseListener.id)
            assertTrue(listener.authority == databaseListener.authority)
            assertTrue(listener.packageName == databaseListener.packageName)
        }
    }

}