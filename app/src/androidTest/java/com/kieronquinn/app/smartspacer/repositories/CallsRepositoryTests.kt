package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CallLog
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CallsRepositoryTests: BaseTest<CallsRepository>() {

    companion object {
        private const val AUTHORITY_CALLS = "${BuildConfig.APPLICATION_ID}.complication.missedcalls"

        private fun getMockMissedCalls(): Cursor {
            return MatrixCursor(arrayOf(CallLog.Calls.IS_READ)).apply {
                addRow(arrayOf(1))
                addRow(arrayOf(0))
                addRow(arrayOf(1))
            }
        }

        private fun getMockEmptyMissedCalls(): Cursor {
            return MatrixCursor(arrayOf(CallLog.Calls.IS_READ))
        }
    }

    private val contentProviderClient = mock<ContentProviderClient>()

    override val sut by lazy {
        CallsRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(Manifest.permission.READ_CALL_LOG)
        } returns PackageManager.PERMISSION_DENIED
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_CALLS
            }
        }
    }

    @Test
    fun testMissedCallsCountNoPermission() = runTest {
        every {
            contentProviderClient.query(
                CallLog.Calls.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            getMockMissedCalls()
        }
        sut.missedCallsCount.test {
            //Should return 0 despite having items without permission
            assertTrue(awaitItem() == 0)
        }
    }

    @Test
    fun testMissedCallsCount() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.READ_CALL_LOG)
        } returns PackageManager.PERMISSION_GRANTED
        var missedCalls = getMockEmptyMissedCalls()
        every {
            contentProviderClient.query(
                CallLog.Calls.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            missedCalls
        }
        sut.missedCallsCount.test {
            //Should return 0
            assertTrue(awaitItem() == 0)
            missedCalls = getMockMissedCalls()
            sut.reload()
            contentResolver.notifyChange(CallLog.Calls.CONTENT_URI)
            //Should now return 1
            assertTrue(awaitItem() == 1)
            missedCalls = getMockEmptyMissedCalls()
            //Should now return 0 again without needing reload call
            contentResolver.notifyChange(CallLog.Calls.CONTENT_URI)
            assertTrue(awaitItem() == 0)
        }
    }

}