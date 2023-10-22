package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements

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
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ComplicationsRequirementsPageViewModelTests: BaseTest<ComplicationsRequirementsPageViewModel>() {

    companion object {
        //For simplicity, all Requirements belong to one package
        private const val MOCK_PACKAGE = "com.example.one"

        private val DUMMY_ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        )

        private fun getMockRequirement(): Requirement {
            @Suppress("CloseRequirement")
            return Requirement(
                authority = randomString(),
                packageName = randomString(),
                invert = randomBoolean()
            )
        }

        private fun getMockAction(any: List<Requirement>, all: List<Requirement>): Action {
            return mock {
                every { anyRequirements } returns any.map { it.id }.toSet()
                every { allRequirements } returns all.map { it.id }.toSet()
            }
        }
    }

    private val mockAnyRequirements = listOf(
        getMockRequirement(),
        getMockRequirement()
    )

    private val mockAllRequirements = listOf(
        getMockRequirement(),
        getMockRequirement()
    )

    private val mockRequirements = mockAnyRequirements + mockAllRequirements
    private val mockAction = getMockAction(mockAnyRequirements, mockAllRequirements)
    private val navigationMock = mock<ContainerNavigation>()
    private val requirementsRepositoryMock = mock<RequirementsRepository>()
    private val mockSetupIntent = mock<Intent>()

    private val databaseRepositoryMock = mock<DatabaseRepository> {
        every { getActionById(any()) } returns flowOf(mockAction)
        every { getRequirementById(any()) } answers {
            flowOf(mockRequirements.firstOrNull { it.id == firstArg() })
        }
    }

    override val sut by lazy {
        ComplicationsRequirementsPageViewModelImpl(
            contextMock,
            databaseRepositoryMock,
            navigationMock,
            requirementsRepositoryMock,
            scope,
            Dispatchers.Main
        )
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

    @Test
    fun testAllState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            sut.setup(randomString(), PageType.ALL)
            sut.reload()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            item.items.zip(mockAllRequirements).forEach {
                val actual = it.first
                val mock = it.second
                assertTrue(actual.requirement.authority == mock.authority)
                assertTrue(actual.requirement.packageName == mock.packageName)
            }
        }
    }

    @Test
    fun testAnyState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            sut.setup(randomString(), PageType.ANY)
            sut.reload()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            item.items.zip(mockAnyRequirements).forEach {
                val actual = it.first
                val mock = it.second
                assertTrue(actual.requirement.authority == mock.authority)
                assertTrue(actual.requirement.packageName == mock.packageName)
            }
        }
    }

    @Test
    fun testDeleteClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            sut.setup(randomString(), PageType.ANY)
            sut.reload()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val requirement = item.items.first()
            sut.onDeleteClicked(requirement)
            coVerify {
                databaseRepositoryMock.deleteRequirement(any())
            }
            coVerify {
                databaseRepositoryMock.updateAction(any())
            }
        }
    }

    @Test
    fun testNotifyRequirementChange() = runTest {
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            sut.setup(randomString(), PageType.ANY)
            sut.reload()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            val requirement = item.items.first()
            sut.notifyRequirementChange(requirement)
            verify {
                requirementsRepositoryMock.notifyChangeAfterDelay(
                    requirement.requirement.id,
                    requirement.requirement.authority
                )
            }
        }
    }

    @Test
    fun testOnAddClicked() = runTest {
        sut.state.test {
            assertTrue(awaitItem() == State.Loading)
            val complicationId = randomString()
            val pageType = PageType.ANY
            sut.setup(complicationId, pageType)
            sut.reload()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            sut.onAddClicked()
            coVerify {
                navigationMock.navigate(
                    ComplicationsRequirementsFragmentDirections
                    .actionComplicationsRequirementsFragmentToComplicationsRequirementsAddFragment(
                        complicationId, pageType))
            }
        }
    }

}