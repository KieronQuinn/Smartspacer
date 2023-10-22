package com.kieronquinn.app.smartspacer.repositories

import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GmailComplication.ActionData
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.GmailRepository.Label
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.gmail.GmailContract
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.mockkObject
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GmailRepositoryTests: BaseTest<GmailRepository>() {

    companion object {
        private const val AUTHORITY_GMAIL = "${BuildConfig.APPLICATION_ID}.complication.gmail"

        private const val LABEL_ONE = "label_one"
        private const val LABEL_TWO = "label_two"
        private const val LABEL_THREE = "label_three"
        private const val ACCOUNT_NAME = "account_name"

        private fun getMockLabels() = listOf(
            Label(randomString(), randomString()),
            Label(randomString(), randomString()),
            Label(randomString(), randomString())
        )

        private fun List<Label>.asCursor(): Cursor {
           return MatrixCursor(
                arrayOf(
                    GmailContract.Labels.NAME,
                    GmailContract.Labels.CANONICAL_NAME
                )
            ).apply {
                forEach {
                    addRow(arrayOf(it.name, it.canonicalName))
                }
            }
        }

        private fun getMockActionData() = listOf(
            ActionData(randomString(), ACCOUNT_NAME, setOf(LABEL_ONE, LABEL_TWO))
        )

        private fun getMockUnreadLabels(): Cursor {
            return MatrixCursor(
                arrayOf(
                    GmailContract.Labels.CANONICAL_NAME,
                    GmailContract.Labels.NUM_UNREAD_CONVERSATIONS
                )
            ).apply {
                addRow(arrayOf(LABEL_ONE, 1))
                addRow(arrayOf(LABEL_TWO, 2))
                addRow(arrayOf(LABEL_THREE, 4))
            }
        }
    }

    private val dataRepositoryMock = mock<DataRepository>()
    private val contentProviderClient = mock<ContentProviderClient>()

    override val sut by lazy {
        GmailRepositoryImpl(contextMock, dataRepositoryMock, scope, Dispatchers.Main)
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(GmailContract.PERMISSION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
    }

    override fun setup() {
        super.setup()
        mockkObject(GmailContract)
        every { GmailContract.canReadLabels(any()) } returns true
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_GMAIL
            }
        }
    }

    @Test
    fun testReload() = runTest {
        sut.reloadBus.test {
            val current = awaitItem()
            sut.reload()
            assertFalse(awaitItem() == current)
        }
    }

    @Test
    fun testUnreadCounts() = runTest {
        val mockId = randomString()
        val mockCount = randomInt()
        sut.currentUnreadCounts[mockId] = mockCount
        assertTrue(sut.getUnreadCount(mockId) == mockCount)
    }

    @Test
    fun testGetAllLabelsNoPermission() = runTest {
        assertTrue(sut.getAllLabels(randomString()).isEmpty())
    }

    @Test
    fun testGetAllLabels() = runTest {
        val mockLabels = getMockLabels()
        val mockName = randomString()
        sut
        every {
            contextMock.checkCallingOrSelfPermission(GmailContract.PERMISSION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            contentProviderClient.query(
                GmailContract.Labels.getLabelsUri(mockName),
                any(),
                any(),
                any()
            )
        } answers {
            mockLabels.asCursor()
        }
        val actualLabels = sut.getAllLabels(mockName)
        assertTrue(actualLabels.isNotEmpty())
    }

    @Test
    fun testCurrentUnreadCounts() = runTest {
        val mockData = getMockActionData()
        val mockName = mockData.first().accountName!!
        val mockId = mockData.first().id
        every {
            dataRepositoryMock.getActionData(
                ActionDataType.GMAIL, ActionData::class.java
            )
        } returns flowOf(mockData)
        sut
        every {
            contextMock.checkCallingOrSelfPermission(GmailContract.PERMISSION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            contentProviderClient.query(
                GmailContract.Labels.getLabelsUri(mockName),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            getMockUnreadLabels()
        }
        sut.reload()
        sut.unreadCounts.test {
            awaitItem()
        }
        //Should equal the sum of LABEL_ONE and LABEL_TWO unread, since LABEL_THREE is not selected
        assertTrue(sut.getUnreadCount(mockId) == 3)
    }

}