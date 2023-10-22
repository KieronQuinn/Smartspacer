package com.kieronquinn.app.smartspacer.repositories

import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GrantRepositoryTests: BaseTest<GrantRepository>() {

    companion object {
        private fun getMockGrants(): List<Grant> {
            return listOf(
                getMockGrant("com.example.one", widget = true),
                getMockGrant("com.example.two", smartspace = true),
                getMockGrant("com.example.three", oem = true),
                getMockGrant("com.example.four", notifications = true),
                getMockGrant(
                    "com.example.five",
                    widget = true,
                    smartspace = true,
                    oem = true,
                    notifications = true
                )
            )
        }

        private fun getMockGrant(
            packageName: String,
            widget: Boolean = false,
            smartspace: Boolean = false,
            oem: Boolean = false,
            notifications: Boolean = false
        ): Grant {
            return Grant(packageName, widget, smartspace, oem, notifications)
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository>()

    override val sut by lazy {
        GrantRepositoryImpl(databaseRepositoryMock, scope)
    }

    @Test
    fun testGetGrantForPackage() = runTest {
        val mocks = getMockGrants()
        every {
            databaseRepositoryMock.getGrants()
        } returns flowOf(mocks)
        val mockSmartspacerGrant = getMockGrant(
            BuildConfig.APPLICATION_ID,
            widget = true,
            smartspace = true,
            oem = true,
            notifications = true
        )
        val actualSmartspacerGrant = sut.getGrantForPackage(BuildConfig.APPLICATION_ID)
        //Smartspacer should always have full perms
        assertTrue(mockSmartspacerGrant == actualSmartspacerGrant)
        mocks.forEach {
            assertTrue(sut.getGrantForPackage(it.packageName) == it)
        }
    }

    @Test
    fun testAddGrant() = runTest {
        val mock = getMockGrants().first()
        sut.addGrant(mock)
        coVerify(exactly = 1) {
            databaseRepositoryMock.addGrant(mock)
        }
    }

}