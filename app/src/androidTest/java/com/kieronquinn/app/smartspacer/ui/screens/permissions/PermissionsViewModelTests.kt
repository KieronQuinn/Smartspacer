package com.kieronquinn.app.smartspacer.ui.screens.permissions

import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.AllowAskEveryTimeOptions
import com.kieronquinn.app.smartspacer.ui.screens.permissions.PermissionsViewModel.PermissionItem.AllowDenyOptions
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PermissionsViewModelTests: BaseTest<PermissionsViewModel>() {

    private val grantRepositoryMock = mock<GrantRepository>()

    override val sut by lazy {
        PermissionsViewModelImpl(
            contextMock,
            grantRepositoryMock,
            scope
        )
    }

    @Test
    fun testOnWidgetPermissionSet() = runTest {
        val packageName = randomString()
        sut.onWidgetPermissionSet(packageName, AllowAskEveryTimeOptions.ALLOW)
        coVerify(inverse = true) {
            grantRepositoryMock.addGrant(any())
        }
        sut.onWidgetPermissionSet(packageName, AllowAskEveryTimeOptions.ASK_EVERY_TIME)
        coVerify {
            grantRepositoryMock.addGrant(any())
        }
    }

    @Test
    fun testOnNotificationsPermissionSet() = runTest {
        val packageName = randomString()
        sut.onNotificationsPermissionSet(packageName, AllowAskEveryTimeOptions.ALLOW)
        coVerify(inverse = true) {
            grantRepositoryMock.addGrant(any())
        }
        sut.onNotificationsPermissionSet(packageName, AllowAskEveryTimeOptions.ASK_EVERY_TIME)
        coVerify {
            grantRepositoryMock.addGrant(any())
        }
    }

    @Test
    fun testOnSmartspacePermissionSet() = runTest {
        val packageName = randomString()
        sut.onSmartspacePermissionSet(packageName, AllowDenyOptions.ALLOW)
        coVerify(inverse = true) {
            grantRepositoryMock.addGrant(any())
        }
        sut.onSmartspacePermissionSet(packageName, AllowDenyOptions.DENY)
        coVerify {
            grantRepositoryMock.addGrant(any())
        }
    }

}