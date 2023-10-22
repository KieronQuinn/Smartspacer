package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.Telephony
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.every
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SmsRepositoryTests: BaseTest<SmsRepository>() {

    companion object {
        private const val AUTHORITY_SMS = "${BuildConfig.APPLICATION_ID}.complication.sms"

        private fun getMockUnreadSms(): Cursor {
            return MatrixCursor(arrayOf(Telephony.Sms.READ)).apply {
                repeat(3) {
                    addRow(arrayOf(0))
                }
            }
        }

        private fun getMockEmptyUnreadSms(): Cursor {
            return MatrixCursor(arrayOf(Telephony.Sms.READ))
        }
    }

    private val contentProviderClient = mock<ContentProviderClient>()

    override val sut by lazy {
        SmsRepositoryImpl(contextMock, scope)
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(Manifest.permission.READ_SMS)
        } returns PackageManager.PERMISSION_DENIED
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<PackageManager.ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_SMS
            }
        }
    }

    @Test
    fun testUnreadSmsCountNoPermission() = runTest {
        every {
            contentProviderClient.query(
                Telephony.Sms.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            getMockUnreadSms()
        }
        sut.smsUnreadCount.test {
            //Should return 0 despite having items without permission
            TestCase.assertTrue(awaitItem() == 0)
        }
    }

    @Test
    fun testUnreadSmsCount() = runTest {
        sut
        every {
            contextMock.checkCallingOrSelfPermission(Manifest.permission.READ_SMS)
        } returns PackageManager.PERMISSION_GRANTED
        var unreadSms = getMockEmptyUnreadSms()
        every {
            contentProviderClient.query(
                Telephony.Sms.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            unreadSms
        }
        sut.smsUnreadCount.test {
            //Should return 0
            TestCase.assertTrue(awaitItem() == 0)
            unreadSms = getMockUnreadSms()
            sut.reload()
            contentResolver.notifyChange(Telephony.Sms.CONTENT_URI)
            //Should now return 1
            TestCase.assertTrue(awaitItem() == 3)
            unreadSms = getMockEmptyUnreadSms()
            //Should now return 0 again without needing reload SMS
            contentResolver.notifyChange(Telephony.Sms.CONTENT_URI)
            TestCase.assertTrue(awaitItem() == 0)
        }
    }

}