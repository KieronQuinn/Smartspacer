package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add

import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add.ComplicationsRequirementsAddViewModel.Item
import com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add.ComplicationsRequirementsAddViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import junit.framework.TestCase
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.kieronquinn.app.smartspacer.model.database.Requirement as DatabaseRequirement

@Suppress("CloseRequirement")
class ComplicationsRequirementsAddViewModelTests: BaseTest<ComplicationsRequirementsAddViewModel>() {

    companion object {
        //For simplicity, all Requirements belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockAction(
            any: Set<String> = emptySet(),
            all: Set<String> = emptySet()
        ): Action {
            return mock {
                every { id } returns randomString()
                every { anyRequirements } returns any
                every { allRequirements } returns all
            }
        }

        private fun getMockDatabaseRequirement(id: String = randomString()): DatabaseRequirement {
            return mock {
                every { this@mock.id } returns id
                every { authority } returns randomString()
                every { packageName } returns MOCK_PACKAGE
            }
        }

        private fun getMockRequirement(
            context: Context,
            req: DatabaseRequirement
        ): Requirement {
            return Requirement(
                context,
                req.authority,
                req.id,
                req.invert,
                req.packageName
            )
        }
    }

    private val navigationMock = mock<ContainerNavigation>()
    private val mockSetupIntent = mock<Intent>()

    private val mockExistingRequirements = listOf(
        getMockDatabaseRequirement(),
        getMockDatabaseRequirement(),
        getMockDatabaseRequirement(),
        getMockDatabaseRequirement()
    )

    private val mockRequirements by lazy {
        listOf(
            mockExistingRequirements.first(),
            getMockDatabaseRequirement(),
            getMockDatabaseRequirement(),
            getMockDatabaseRequirement()
        ).map {
            getMockRequirement(contextMock, it)
        }
    }

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getRequirementById(any()) } answers {
            flowOf(getMockDatabaseRequirement(firstArg()))
        }
        every { getActionById(any()) } answers {
            flowOf(
                getMockAction(
                mockExistingRequirements.subList(0, 1).map { it.authority }.toSet(),
                mockExistingRequirements.subList(2, 3).map { it.authority }.toSet()
            )
            )
        }
    }

    private val requirementsRepositoryMock = mock<RequirementsRepository> {
        every { getAllRequirements() } returns mockRequirements
    }

    override fun Context.context() {
        val contentProviderClient = mock<ContentProviderClient> {
            every { call(any(), any(), any()) } answers {
                SmartspacerRequirementProvider.Config(
                    randomString(),
                    randomString(),
                    DUMMY_ICON,
                    setupActivity = mockSetupIntent
                ).toBundle()
            }
        }
        every {
            contentResolver.acquireContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<Uri>())
        } answers {
            contentProviderClient
        }
        every {
            contentResolver.acquireUnstableContentProviderClient(any<String>())
        } answers {
            contentProviderClient
        }
        every { packageManagerMock.resolveContentProvider(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                packageName = MOCK_PACKAGE
            }
        }
        every { packageManagerMock.resolveActivity(any(), any<ResolveInfoFlags>()) } answers {
            ResolveInfo()
        }
    }

    override val sut by lazy {
        ComplicationsRequirementsAddViewModelImpl(
            contextMock,
            requirementsRepositoryMock,
            databaseRepositoryMock,
            navigationMock,
            scope,
            Dispatchers.Main
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(randomString(), PageType.ALL)
            val itemBeforeCollapsed = awaitItem()
            assertTrue(itemBeforeCollapsed is State.Loaded)
            itemBeforeCollapsed as State.Loaded
            //Verify only item is app (collapsed state)
            assertTrue(itemBeforeCollapsed.items.all { it is Item.App })
            sut.onExpandClicked(itemBeforeCollapsed.items.first() as Item.App)
            val items = awaitItem()
            assertTrue(items is State.Loaded)
            items as State.Loaded
            assertTrue(items.items.first() is Item.App)
            //First item is app, skip to just requirements
            val actuals = items.items.subList(1, items.items.size)
                .filterIsInstance<Item.Requirement>()
                .sortedBy { it.authority }
            val mocks = mockRequirements.sortedBy { it.authority }
            actuals.zip(mocks).forEach {
                val actual = it.first
                val mock = it.second
                assertTrue(actual.authority == mock.authority)
                assertTrue(actual.packageName == mock.sourcePackage)
            }
        }
    }

    @Test
    fun testSearchTerm() = runTest {
        sut.searchTerm.test {
            assertTrue(awaitItem().isEmpty())
            val random = randomString()
            sut.setSearchTerm(random)
            assertTrue(awaitItem() == random)
            assertTrue(sut.getSearchTerm() == random)
        }
    }

    @Test
    fun testShowSearchClear() = runTest {
        sut.showSearchClear.test {
            sut.setSearchTerm("")
            TestCase.assertFalse(awaitItem())
            sut.setSearchTerm(randomString())
            assertTrue(awaitItem())
        }
    }

}